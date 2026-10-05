"""Verificación de los datos sembrados de campaña (ADR 0006 y ADR 0007)."""

from fastapi.testclient import TestClient

from app.main import app

# Regla de coherencia en dos etapas (ADR 0007).
MERMA_INDUSTRIAL = 0.097

RUTAS_DE_LECTURA = [
    "/api/v1/cooperativa",
    "/api/v1/socios",
    "/api/v1/socios/socio-001/campanas",
    "/api/v1/socios/socio-001/parcelas",
]


def _campanas_por_socio(client):
    return {
        socio["id"]: client.get("/api/v1/socios/{}/campanas".format(socio["id"])).json()
        for socio in client.get("/api/v1/socios").json()
    }


def test_seed_carga_una_cooperativa_dos_socios_y_cuatro_campanas():
    with TestClient(app) as client:
        cooperativa = client.get("/api/v1/cooperativa").json()
        assert cooperativa["id"] == "coop-demo"
        assert "demostración" in cooperativa["declaracion"]

        assert len(client.get("/api/v1/socios").json()) == 2

        campanas = _campanas_por_socio(client)
        for socio_id, lista in campanas.items():
            assert len(lista) == 4, socio_id


def test_cada_socio_tiene_datos_en_la_campana_actual_y_al_menos_una_anterior():
    with TestClient(app) as client:
        for socio_id, campanas in _campanas_por_socio(client).items():
            assert any(c["actual"] for c in campanas), socio_id
            assert any(not c["actual"] for c in campanas), socio_id
            for c in campanas:
                datos = c["datos_oficiales"]
                assert datos["kilos_entregados"] > 0, (socio_id, c["id"])
                assert datos["rendimiento_graso"] > 0, (socio_id, c["id"])
                assert datos["aceite_obtenido"] > 0, (socio_id, c["id"])


def test_regla_de_coherencia_en_dos_etapas():
    with TestClient(app) as client:
        for socio_id, campanas in _campanas_por_socio(client).items():
            for c in campanas:
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
        campanas = client.get("/api/v1/socios/socio-001/campanas").json()
        actual = next(c for c in campanas if c["id"] == "2025-2026")
        assert actual["datos_oficiales"] == {
            "kilos_entregados": 10958.0,
            "rendimiento_graso": 17.93,
            "aceite_obtenido": 1774.0,
        }


def test_las_parcelas_sembradas_estan_completas():
    with TestClient(app) as client:
        parcelas_1 = client.get("/api/v1/socios/socio-001/parcelas").json()
        parcelas_2 = client.get("/api/v1/socios/socio-002/parcelas").json()
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
        assert client.get("/api/v1/socios/nadie/campanas").status_code == 404
        assert client.get("/api/v1/socios/nadie/parcelas").status_code == 404


def test_reejecutar_el_seed_no_duplica_datos():
    from app import db as basedatos
    from app.seed import cargar_si_vacia

    with TestClient(app) as client:
        conn = basedatos.conectar()
        try:
            assert cargar_si_vacia(conn) == 0  # base ya llena: no hace nada
        finally:
            conn.close()
        campanas = client.get("/api/v1/socios/socio-001/campanas").json()
        assert len(campanas) == 4
        assert basedatos.ruta_base_datos().name == "olivapp-test.db"
