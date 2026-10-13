"""Verificación de los datos sembrados de campaña (ADR 0006 y ADR 0007).

Desde `inicio-de-sesion` las llamadas a los datos exigen token válido y,
desde `aislamiento-de-datos-por-socio`, cada cuenta solo accede a los datos
de su propio socio. Los tests se autentican con una cuenta por socio del
seed (cambio de verificación declarado en `aislamiento-de-datos-por-socio`:
antes una única cuenta veía ambos socios).
"""

from fastapi.testclient import TestClient

from app import db as basedatos
from app.main import app

# Regla de coherencia en dos etapas (ADR 0007).
MERMA_INDUSTRIAL = 0.097

CUENTA_SOCIO_1 = {
    "email": "verificacion1@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 1",
}
CUENTA_SOCIO_2 = {
    "email": "verificacion2@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 2",
}

RUTAS_DE_LECTURA = [
    "/api/v1/cooperativa",
    "/api/v1/socios",
    "/api/v1/socios/socio-001/campanas",
    "/api/v1/socios/socio-001/parcelas",
]


def _cabeceras(client, cuenta):
    assert client.post("/api/v1/registro", json=cuenta).status_code == 201
    return _con_token(client, cuenta)


def _con_token(client, cuenta):
    credenciales = {"email": cuenta["email"], "contrasena": cuenta["contrasena"]}
    token = client.post("/api/v1/login", json=credenciales).json()["token"]
    return {"Authorization": "Bearer " + token}


def _campanas_propias(client, cabeceras, socio_id):
    return client.get(
        "/api/v1/socios/{}/campanas".format(socio_id), headers=cabeceras
    ).json()


def test_seed_carga_una_cooperativa_dos_socios_y_cuatro_campanas():
    with TestClient(app) as client:
        cabeceras_1 = _cabeceras(client, CUENTA_SOCIO_1)
        cabeceras_2 = _cabeceras(client, CUENTA_SOCIO_2)

        cooperativa = client.get("/api/v1/cooperativa", headers=cabeceras_1).json()
        assert cooperativa["id"] == "coop-demo"
        assert "demostración" in cooperativa["declaracion"]

        # El seed tiene exactamente 2 socios (las dos cuentas de verificación
        # se vinculan a ellos por su código; un tercer código no existe).
        conn = basedatos.conectar()
        try:
            socios = conn.execute("SELECT id FROM socio ORDER BY id").fetchall()
        finally:
            conn.close()
        assert [s["id"] for s in socios] == ["socio-001", "socio-002"]

        for socio_id, cabeceras in (("socio-001", cabeceras_1), ("socio-002", cabeceras_2)):
            propios = client.get("/api/v1/socios", headers=cabeceras).json()
            assert [s["id"] for s in propios] == [socio_id]
            assert len(_campanas_propias(client, cabeceras, socio_id)) == 4


def test_cada_socio_tiene_datos_en_la_campana_actual_y_al_menos_una_anterior():
    with TestClient(app) as client:
        for cuenta, socio_id in ((CUENTA_SOCIO_1, "socio-001"), (CUENTA_SOCIO_2, "socio-002")):
            cabeceras = _cabeceras(client, cuenta)
            campanas = _campanas_propias(client, cabeceras, socio_id)
            assert any(c["actual"] for c in campanas), socio_id
            assert any(not c["actual"] for c in campanas), socio_id
            for c in campanas:
                datos = c["datos_oficiales"]
                assert datos["kilos_entregados"] > 0, (socio_id, c["id"])
                assert datos["rendimiento_graso"] > 0, (socio_id, c["id"])
                assert datos["aceite_obtenido"] > 0, (socio_id, c["id"])


def test_regla_de_coherencia_en_dos_etapas():
    with TestClient(app) as client:
        for cuenta, socio_id in ((CUENTA_SOCIO_1, "socio-001"), (CUENTA_SOCIO_2, "socio-002")):
            cabeceras = _cabeceras(client, cuenta)
            for c in _campanas_propias(client, cabeceras, socio_id):
                datos = c["datos_oficiales"]
                esperado = round(
                    datos["kilos_entregados"]
                    * datos["rendimiento_graso"]
                    / 100.0
                    * (1 - MERMA_INDUSTRIAL)
                )
                assert datos["aceite_obtenido"] == esperado, (socio_id, c["id"])


def test_campana_2025_2026_del_socio_1_con_datos_reales_anonimizados():
    with TestClient(app) as client:
        cabeceras = _cabeceras(client, CUENTA_SOCIO_1)
        campanas = _campanas_propias(client, cabeceras, "socio-001")
        actual = next(c for c in campanas if c["id"] == "2025-2026")
        assert actual["datos_oficiales"] == {
            "kilos_entregados": 10958.0,
            "rendimiento_graso": 17.93,
            "aceite_obtenido": 1774.0,
        }


def test_las_parcelas_sembradas_estan_completas():
    with TestClient(app) as client:
        cabeceras = _cabeceras(client, CUENTA_SOCIO_1)
        parcelas_1 = client.get(
            "/api/v1/socios/socio-001/parcelas", headers=cabeceras
        ).json()
        cabeceras = _cabeceras(client, CUENTA_SOCIO_2)
        parcelas_2 = client.get(
            "/api/v1/socios/socio-002/parcelas", headers=cabeceras
        ).json()
        assert len(parcelas_1) == 4
        assert len(parcelas_2) == 3


def test_los_datos_oficiales_no_admiten_escritura_desde_la_api():
    with TestClient(app) as client:
        for ruta in RUTAS_DE_LECTURA:
            for metodo in ("post", "put", "patch", "delete"):
                respuesta = client.request(metodo.upper(), ruta, json={})
                assert respuesta.status_code == 405, (metodo, ruta, respuesta.status_code)


def test_rejecta_socio_desconocido():
    with TestClient(app) as client:
        cabeceras = _cabeceras(client, CUENTA_SOCIO_1)
        assert (
            client.get("/api/v1/socios/nadie/campanas", headers=cabeceras).status_code
            == 404
        )
        assert (
            client.get("/api/v1/socios/nadie/parcelas", headers=cabeceras).status_code
            == 404
        )


def test_reejecutar_el_seed_no_duplica_datos():
    from app.seed import cargar_si_vacia

    with TestClient(app) as client:
        cabeceras = _cabeceras(client, CUENTA_SOCIO_1)
        conn = basedatos.conectar()
        try:
            assert cargar_si_vacia(conn) == 0  # base ya llena: no hace nada
        finally:
            conn.close()
        campanas = _campanas_propias(client, cabeceras, "socio-001")
        assert len(campanas) == 4
        assert basedatos.ruta_base_datos().name == "olivapp-test.db"
