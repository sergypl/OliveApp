"""Verificación del alta manual de labores (feature alta-manual-labor).

Campos por categoría según ADR 0005 y campaña derivada de la fecha (campaña
oleícola del 1 de noviembre al 31 de octubre).
"""

from fastapi.testclient import TestClient

from app.main import app
from app.labores import campana_de_la_fecha
import datetime

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123"}


def _entrar(client):
    """Registra la cuenta de prueba, crea una parcela y devuelve el contexto."""
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    cabeceras = {"Authorization": "Bearer " + token}
    parcela = client.post(
        "/api/v1/mis-parcelas", json={"nombre": "La Noria"}, headers=cabeceras
    ).json()
    return cabeceras, parcela["id"]


def _crear(client, cabeceras, parcela_id, categoria, campos, fecha="2026-02-10", **extra):
    cuerpo = {
        "fecha": fecha,
        "parcela_id": parcela_id,
        "categoria": categoria,
        "campos": campos,
    }
    cuerpo.update(extra)
    return client.post("/api/v1/mis-labores", json=cuerpo, headers=cabeceras)


def test_una_labor_de_cada_categoria_se_guarda_con_sus_campos():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)

        tratamiento = _crear(
            client,
            cabeceras,
            parcela_id,
            "tratamiento",
            {
                "producto": "Azufre mojable",
                "plaga_a_controlar": "cochinilla",
                "cantidad": 2.5,
                "unidad": "L",
            },
            fecha="2025-12-10",
            observaciones="Tratamiento preventivo.",
        )
        riego = _crear(
            client,
            cabeceras,
            parcela_id,
            "riego",
            {"cantidad": 3, "unidad": "horas"},
            fecha="2026-01-20",
        )
        cosecha = _crear(
            client,
            cabeceras,
            parcela_id,
            "cosecha",
            {"modalidad": "a vuelo", "kilos": 850},
            fecha="2026-02-15",
        )

        for respuesta in (tratamiento, riego, cosecha):
            assert respuesta.status_code == 201, respuesta.text

        assert tratamiento.json()["campos"] == {
            "producto": "Azufre mojable",
            "plaga_a_controlar": "cochinilla",
            "cantidad": 2.5,
            "unidad": "L",
        }
        assert tratamiento.json()["observaciones"] == "Tratamiento preventivo."
        assert riego.json()["campos"] == {"cantidad": 3.0, "unidad": "horas"}
        assert cosecha.json()["campos"] == {"modalidad": "a vuelo", "kilos": 850.0}


def test_las_labores_se_listan_en_la_campana_que_corresponde_a_su_fecha():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        # Diciembre de 2025 y febrero de 2026: campaña oleícola 2025-2026.
        _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 2, "unidad": "horas"}, fecha="2025-12-10")
        _crear(client, cabeceras, parcela_id, "cosecha", {"modalidad": "a suelo", "kilos": 100}, fecha="2026-02-15")

        actual = client.get("/api/v1/mi-campanas/2025-2026", headers=cabeceras).json()
        fechas = [l["fecha"] for l in actual["labores"]]
        assert fechas == ["2026-02-15", "2025-12-10"]  # de la más reciente a la más antigua

        # Y en una campaña anterior no se cuela ninguna.
        anterior = client.get("/api/v1/mi-campanas/2024-2025", headers=cabeceras).json()
        assert anterior["labores"] == []


def test_la_campana_se_deriva_de_la_fecha():
    assert campana_de_la_fecha(datetime.date(2026, 10, 9)) == "2025-2026"
    assert campana_de_la_fecha(datetime.date(2025, 11, 1)) == "2025-2026"
    assert campana_de_la_fecha(datetime.date(2025, 10, 31)) == "2024-2025"
    assert campana_de_la_fecha(datetime.date(2024, 12, 1)) == "2024-2025"


def test_una_fecha_de_campana_cerrada_no_admite_labores():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        respuesta = _crear(
            client,
            cabeceras,
            parcela_id,
            "riego",
            {"cantidad": 2, "unidad": "horas"},
            fecha="2025-10-31",  # campaña 2024-2025, cerrada
        )
        assert respuesta.status_code == 400
        assert "cerrada" in respuesta.json()["detail"].lower()


def test_una_fecha_fuera_de_toda_campana_da_error_claro():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        respuesta = _crear(
            client,
            cabeceras,
            parcela_id,
            "riego",
            {"cantidad": 2, "unidad": "horas"},
            fecha="2030-01-01",
        )
        assert respuesta.status_code == 400
        assert "campaña" in respuesta.json()["detail"].lower()


def test_las_listas_cerradas_se_respetan():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        plaga_mala = _crear(
            client, cabeceras, parcela_id, "tratamiento",
            {"producto": "X", "plaga_a_controlar": "bichito", "cantidad": 1, "unidad": "L"},
        )
        assert plaga_mala.status_code == 400
        assert "plaga" in plaga_mala.json()["detail"].lower()

        unidad_mala = _crear(
            client, cabeceras, parcela_id, "riego", {"cantidad": 1, "unidad": "litros por segundo"},
        )
        assert unidad_mala.status_code == 400
        assert "unidad" in unidad_mala.json()["detail"].lower()

        modalidad_mala = _crear(
            client, cabeceras, parcela_id, "cosecha", {"modalidad": "a voleo", "kilos": 10},
        )
        assert modalidad_mala.status_code == 400
        assert "modalidad" in modalidad_mala.json()["detail"].lower()


def test_faltan_campos_obligatorios_de_la_categoria():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        sin_producto = _crear(
            client, cabeceras, parcela_id, "tratamiento",
            {"producto": "  ", "plaga_a_controlar": "repilo", "cantidad": 1, "unidad": "kg"},
        )
        assert sin_producto.status_code == 400
        assert "producto" in sin_producto.json()["detail"].lower()

        sin_kilos = _crear(
            client, cabeceras, parcela_id, "cosecha", {"modalidad": "a vuelo"},
        )
        assert sin_kilos.status_code == 400
        assert "kilos" in sin_kilos.json()["detail"].lower()

        sin_categoria = _crear(client, cabeceras, parcela_id, "fumigación", {})
        assert sin_categoria.status_code == 400
        assert "categoría" in sin_categoria.json()["detail"].lower()


def test_las_observaciones_son_opcionales():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        respuesta = _crear(
            client, cabeceras, parcela_id, "riego", {"cantidad": 2, "unidad": "m³"}
        )
        assert respuesta.status_code == 201
        assert respuesta.json()["observaciones"] == ""


def test_la_parcela_de_otro_socio_no_acepta_labores():
    with TestClient(app) as client:
        cabeceras, _ = _entrar(client)
        # Una parcela sembrada de otro socio.
        respuesta = _crear(
            client, cabeceras, "parcela-2-1", "riego", {"cantidad": 1, "unidad": "horas"}
        )
        assert respuesta.status_code == 404
        assert "parcela" in respuesta.json()["detail"].lower()


def test_mis_labores_exige_token_valido():
    with TestClient(app) as client:
        cabeceras, _ = _entrar(client)
        assert client.get("/api/v1/mi-campanas/2025-2026").status_code == 401
        falso = client.get(
            "/api/v1/mi-campanas/2025-2026",
            headers={"Authorization": "Bearer inventado"},
        )
        assert falso.status_code == 401
        assert (
            client.get("/api/v1/mi-campanas/2025-2026", headers=cabeceras).status_code
            == 200
        )
