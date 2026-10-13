"""Verificación del alta manual de labores (feature alta-manual-labor).

Campos por categoría según ADR 0005 y campaña derivada de la fecha (campaña
oleícola del 1 de noviembre al 31 de octubre).
"""

from fastapi.testclient import TestClient

from app.main import app
from app.labores import campana_de_la_fecha
import datetime
import json
import os
import sqlite3

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123", "codigo_socio": "Socio 1"}


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


# --- Historial por campaña (feature consulta-cuaderno-por-campana) ---


def _socio_de_la_cuenta():
    conn = sqlite3.connect(os.environ["OLIVAPP_DB"])
    try:
        fila = conn.execute(
            "SELECT socio_id FROM cuenta WHERE email = ?", (DATOS["email"],)
        ).fetchone()
        return fila[0]
    finally:
        conn.close()


def _insertar_labor_directa(socio_id, parcela_id, campana_id, fecha, categoria, campos):
    """Inserta una labor directamente: la API solo admite la campaña actual."""
    conn = sqlite3.connect(os.environ["OLIVAPP_DB"])
    try:
        conn.execute(
            "INSERT INTO labor "
            "(id, socio_id, parcela_id, campana_id, categoria, fecha, campos) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                "labor-" + fecha + "-" + socio_id,
                socio_id,
                parcela_id,
                campana_id,
                categoria,
                fecha,
                json.dumps(campos, ensure_ascii=False),
            ),
        )
        conn.commit()
    finally:
        conn.close()


def test_historial_agrupado_por_campana_en_orden_cronologico():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        assert _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10").status_code == 201
        assert _crear(client, cabeceras, parcela_id, "cosecha", {"modalidad": "a vuelo", "kilos": 850}, fecha="2026-02-15").status_code == 201
        _insertar_labor_directa(
            _socio_de_la_cuenta(),
            parcela_id,
            "2024-2025",
            "2024-12-05",
            "tratamiento",
            {"producto": "Azufre mojable", "plaga_a_controlar": "repilo", "cantidad": 2, "unidad": "L"},
        )

        respuesta = client.get("/api/v1/mis-labores", headers=cabeceras)

        assert respuesta.status_code == 200
        campanas = respuesta.json()["campanas"]
        # De la campaña más reciente a la más antigua.
        assert [c["id"] for c in campanas] == ["2025-2026", "2024-2025"]
        assert campanas[0]["nombre"] == "2025/2026"
        assert campanas[0]["es_actual"] is True
        assert campanas[1]["es_actual"] is False
        # Dentro de cada campaña, de la labor más reciente a la más antigua.
        assert [l["fecha"] for l in campanas[0]["labores"]] == ["2026-02-15", "2025-12-10"]
        assert [l["fecha"] for l in campanas[1]["labores"]] == ["2024-12-05"]


def test_historial_ensena_los_campos_tipo_siex_de_cada_labor():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(
            client,
            cabeceras,
            parcela_id,
            "tratamiento",
            {"producto": "Azufre mojable", "plaga_a_controlar": "cochinilla", "cantidad": 2.5, "unidad": "L"},
            fecha="2025-12-10",
            observaciones="Tratamiento preventivo.",
        )
        assert creada.status_code == 201, creada.text

        campanas = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"]
        labor = campanas[0]["labores"][0]

        assert labor["categoria"] == "tratamiento"
        assert labor["parcela"]["nombre"] == "La Noria"
        assert labor["campos"] == {
            "producto": "Azufre mojable",
            "plaga_a_controlar": "cochinilla",
            "cantidad": 2.5,
            "unidad": "L",
        }
        assert labor["observaciones"] == "Tratamiento preventivo."


def test_historial_solo_incluye_las_labores_del_socio():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        assert _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10").status_code == 201
        # Una labor de otro socio no debe aparecer en el historial.
        _insertar_labor_directa(
            "socio-002",
            parcela_id,
            "2025-2026",
            "2026-01-20",
            "cosecha",
            {"modalidad": "a suelo", "kilos": 100},
        )

        campanas = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"]
        fechas = [l["fecha"] for c in campanas for l in c["labores"]]

        assert fechas == ["2025-12-10"]


def test_historial_vacio_sin_labores():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)

        respuesta = client.get("/api/v1/mis-labores", headers=cabeceras)

        assert respuesta.status_code == 200
        assert respuesta.json() == {"campanas": []}


# --- Edición de labores (feature edicion-de-labor) ---


def _editar(client, cabeceras, labor_id, categoria, campos, fecha, parcela_id, **extra):
    cuerpo = {
        "fecha": fecha,
        "parcela_id": parcela_id,
        "categoria": categoria,
        "campos": campos,
    }
    cuerpo.update(extra)
    return client.put(
        "/api/v1/mis-labores/" + labor_id, json=cuerpo, headers=cabeceras
    )


def test_editar_cualquier_campo_incluida_fecha_y_parcela():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(
            client,
            cabeceras,
            parcela_id,
            "tratamiento",
            {"producto": "Azufre mojable", "plaga_a_controlar": "cochinilla", "cantidad": 2.5, "unidad": "L"},
            fecha="2025-12-10",
            observaciones="Tratamiento preventivo.",
        )
        assert creada.status_code == 201, creada.text
        labor_id = creada.json()["id"]
        otra = client.post(
            "/api/v1/mis-parcelas", json={"nombre": "La Umbría"}, headers=cabeceras
        ).json()

        # Cambia todo: fecha, parcela, categoría, campos y observaciones.
        respuesta = _editar(
            client,
            cabeceras,
            labor_id,
            "cosecha",
            {"modalidad": "a suelo", "kilos": 720},
            "2026-03-05",
            otra["id"],
            observaciones="Cosecha corregida.",
        )

        assert respuesta.status_code == 200, respuesta.text
        assert respuesta.json()["fecha"] == "2026-03-05"
        assert respuesta.json()["parcela"]["nombre"] == "La Umbría"
        assert respuesta.json()["categoria"] == "cosecha"
        # El cambio se refleja en el listado del cuaderno.
        campanas = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"]
        labores = campanas[0]["labores"]
        assert len(labores) == 1
        assert labores[0]["fecha"] == "2026-03-05"
        assert labores[0]["categoria"] == "cosecha"
        assert labores[0]["campos"] == {"modalidad": "a suelo", "kilos": 720}
        assert labores[0]["parcela"]["nombre"] == "La Umbría"
        assert labores[0]["observaciones"] == "Cosecha corregida."


def test_editar_mueve_la_labor_en_el_orden_cronologico():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        primera = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert primera.status_code == 201
        segunda = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 2, "unidad": "horas"}, fecha="2026-01-20")
        assert segunda.status_code == 201

        # La más antigua pasa a ser la más reciente.
        respuesta = _editar(
            client,
            cabeceras,
            primera.json()["id"],
            "riego",
            {"cantidad": 3, "unidad": "horas"},
            "2026-02-01",
            parcela_id,
        )

        assert respuesta.status_code == 200
        labores = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"][0]["labores"]
        assert [l["fecha"] for l in labores] == ["2026-02-01", "2026-01-20"]


def test_editar_a_fecha_de_campana_cerrada_se_rechaza():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert creada.status_code == 201

        respuesta = _editar(
            client,
            cabeceras,
            creada.json()["id"],
            "riego",
            {"cantidad": 3, "unidad": "horas"},
            "2024-12-05",
            parcela_id,
        )

        assert respuesta.status_code == 400
        assert "cerrada" in respuesta.json()["detail"].lower()
        # La labor sigue con su fecha original.
        labores = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"][0]["labores"]
        assert [l["fecha"] for l in labores] == ["2025-12-10"]


def test_editar_labor_de_otro_socio_no_existe():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)

        respuesta = _editar(
            client,
            cabeceras,
            "labor-inventada",
            "riego",
            {"cantidad": 3, "unidad": "horas"},
            "2025-12-10",
            parcela_id,
        )

        assert respuesta.status_code == 404


def test_editar_exige_token():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert creada.status_code == 201

        sin_token = client.put(
            "/api/v1/mis-labores/" + creada.json()["id"],
            json={
                "fecha": "2025-12-10",
                "parcela_id": parcela_id,
                "categoria": "riego",
                "campos": {"cantidad": 3, "unidad": "horas"},
            },
        )
        assert sin_token.status_code == 401


# --- Eliminación de labores (feature eliminacion-de-labor) ---


def test_eliminar_labor_la_quita_del_cuaderno_y_de_la_campana():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert creada.status_code == 201, creada.text
        labor_id = creada.json()["id"]

        respuesta = client.delete("/api/v1/mis-labores/" + labor_id, headers=cabeceras)

        assert respuesta.status_code == 200
        assert respuesta.json() == {"id": labor_id, "eliminada": True}
        # Desaparece del cuaderno…
        assert client.get("/api/v1/mis-labores", headers=cabeceras).json() == {"campanas": []}
        # …y de la campaña.
        detalle = client.get("/api/v1/mi-campanas/2025-2026", headers=cabeceras).json()
        assert detalle["labores"] == []


def test_eliminar_no_toca_las_demas_labores():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        primera = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert primera.status_code == 201
        segunda = _crear(client, cabeceras, parcela_id, "cosecha", {"modalidad": "a vuelo", "kilos": 850}, fecha="2026-02-15")
        assert segunda.status_code == 201

        respuesta = client.delete("/api/v1/mis-labores/" + primera.json()["id"], headers=cabeceras)

        assert respuesta.status_code == 200
        labores = client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"][0]["labores"]
        assert [l["fecha"] for l in labores] == ["2026-02-15"]


def test_eliminar_una_labor_inexistente_o_ajena_no_existe():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert creada.status_code == 201
        labor_id = creada.json()["id"]

        assert client.delete("/api/v1/mis-labores/labor-inventada", headers=cabeceras).status_code == 404
        # La segunda vez ya no existe: borrado directo, sin papelera.
        assert client.delete("/api/v1/mis-labores/" + labor_id, headers=cabeceras).status_code == 200
        assert client.delete("/api/v1/mis-labores/" + labor_id, headers=cabeceras).status_code == 404


def test_eliminar_exige_token():
    with TestClient(app) as client:
        cabeceras, parcela_id = _entrar(client)
        creada = _crear(client, cabeceras, parcela_id, "riego", {"cantidad": 3, "unidad": "horas"}, fecha="2025-12-10")
        assert creada.status_code == 201

        assert client.delete("/api/v1/mis-labores/" + creada.json()["id"]).status_code == 401
