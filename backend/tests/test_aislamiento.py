"""Aislamiento de datos por socio (feature aislamiento-de-datos-por-socio).

Con dos cuentas vinculadas a socios distintos (por su código de socio), cada
una solo ve y edita sus propias parcelas, labores y datos oficiales de
campaña; las consultas a los datos del otro socio fallan (404) o devuelven
vacío.
"""

from fastapi.testclient import TestClient

from app.main import app

CUENTA_A = {
    "email": "socio.uno@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 1",
}
CUENTA_B = {
    "email": "socio.dos@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 2",
}


def _entrar(client, cuenta):
    assert client.post("/api/v1/registro", json=cuenta).status_code == 201
    respuesta = client.post(
        "/api/v1/login",
        json={"email": cuenta["email"], "contrasena": cuenta["contrasena"]},
    )
    assert respuesta.status_code == 200
    return {"Authorization": "Bearer " + respuesta.json()["token"]}


def _crear_parcela(client, cabeceras, nombre):
    respuesta = client.post(
        "/api/v1/mis-parcelas", json={"nombre": nombre}, headers=cabeceras
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()["id"]


def _crear_labor(client, cabeceras, parcela_id):
    respuesta = client.post(
        "/api/v1/mis-labores",
        json={
            "fecha": "2025-12-10",
            "parcela_id": parcela_id,
            "categoria": "riego",
            "campos": {"cantidad": 3, "unidad": "horas"},
            "observaciones": "Labor del socio A.",
        },
        headers=cabeceras,
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()["id"]


def test_cada_solo_solo_ve_sus_parcelas():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)
        _crear_parcela(client, cabeceras_a, "Parcela del socio A")

        nombres_a = [
            p["nombre"] for p in client.get("/api/v1/mis-parcelas", headers=cabeceras_a).json()
        ]
        nombres_b = [
            p["nombre"] for p in client.get("/api/v1/mis-parcelas", headers=cabeceras_b).json()
        ]
        assert "Parcela del socio A" in nombres_a
        assert "Parcela del socio A" not in nombres_b
        # Cada socio conserva sus parcelas sembradas.
        assert "La Umbría" in nombres_a
        assert "El Romeral" in nombres_b


def test_cada_solo_solo_ve_sus_labores():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)
        parcela_a = _crear_parcela(client, cabeceras_a, "Parcela del socio A")
        labor_a = _crear_labor(client, cabeceras_a, parcela_a)

        cuaderno_a = client.get("/api/v1/mis-labores", headers=cabeceras_a).json()
        cuaderno_b = client.get("/api/v1/mis-labores", headers=cabeceras_b).json()
        ids_a = [l["id"] for c in cuaderno_a["campanas"] for l in c["labores"]]
        ids_b = [l["id"] for c in cuaderno_b["campanas"] for l in c["labores"]]
        assert ids_a == [labor_a]
        assert ids_b == []

        # El otro socio ni siquiera puede editarla ni eliminarla.
        editar = client.put(
            "/api/v1/mis-labores/" + labor_a,
            json={
                "fecha": "2025-12-11",
                "parcela_id": parcela_a,
                "categoria": "riego",
                "campos": {"cantidad": 1, "unidad": "horas"},
            },
            headers=cabeceras_b,
        )
        assert editar.status_code == 404
        assert (
            client.delete("/api/v1/mis-labores/" + labor_a, headers=cabeceras_b).status_code
            == 404
        )


def test_un_socio_no_puede_apuntar_labores_en_la_parcela_de_otro():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)
        parcela_a = _crear_parcela(client, cabeceras_a, "Parcela del socio A")

        respuesta = client.post(
            "/api/v1/mis-labores",
            json={
                "fecha": "2025-12-10",
                "parcela_id": parcela_a,
                "categoria": "riego",
                "campos": {"cantidad": 3, "unidad": "horas"},
            },
            headers=cabeceras_b,
        )
        assert respuesta.status_code == 404


def test_los_datos_oficiales_son_los_del_socio_de_la_cuenta():
    with TestClient(app) as client:
        panel_a = client.get(
            "/api/v1/mi-panel", headers=_entrar(client, CUENTA_A)
        ).json()["datos_oficiales"]
        panel_b = client.get(
            "/api/v1/mi-panel", headers=_entrar(client, CUENTA_B)
        ).json()["datos_oficiales"]
        assert panel_a == {
            "kilos_entregados": 10958.0,
            "rendimiento_graso": 17.93,
            "aceite_obtenido": 1774.0,
        }
        assert panel_b == {
            "kilos_entregados": 2100.0,
            "rendimiento_graso": 19.2,
            "aceite_obtenido": 364.0,
        }


def test_las_consultas_a_los_datos_del_otro_socio_fallan():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)

        # Cada cuenta lista solo su propio socio…
        propios_a = client.get("/api/v1/socios", headers=cabeceras_a).json()
        propios_b = client.get("/api/v1/socios", headers=cabeceras_b).json()
        assert [s["id"] for s in propios_a] == ["socio-001"]
        assert [s["id"] for s in propios_b] == ["socio-002"]

        # …y las rutas del otro socio responden 404 (sin revelar que existe).
        assert (
            client.get("/api/v1/socios/socio-002/campanas", headers=cabeceras_a).status_code
            == 404
        )
        assert (
            client.get("/api/v1/socios/socio-002/parcelas", headers=cabeceras_a).status_code
            == 404
        )
        assert (
            client.get("/api/v1/socios/socio-001/campanas", headers=cabeceras_b).status_code
            == 404
        )
        assert (
            client.get("/api/v1/socios/socio-001/parcelas", headers=cabeceras_b).status_code
            == 404
        )


def test_el_detalle_de_campana_los_datos_son_los_del_propio_socio():
    with TestClient(app) as client:
        detalle_a = client.get(
            "/api/v1/mi-campanas/2025-2026", headers=_entrar(client, CUENTA_A)
        ).json()
        detalle_b = client.get(
            "/api/v1/mi-campanas/2025-2026", headers=_entrar(client, CUENTA_B)
        ).json()
        assert detalle_a["datos_oficiales"]["kilos_entregados"] == 10958.0
        assert detalle_b["datos_oficiales"]["kilos_entregados"] == 2100.0
        assert detalle_a["labores"] == [] and detalle_b["labores"] == []


def test_sin_token_no_se_ve_nada_de_nadie():
    with TestClient(app) as client:
        for ruta in (
            "/api/v1/socios",
            "/api/v1/socios/socio-001/campanas",
            "/api/v1/mi-panel",
            "/api/v1/mis-parcelas",
            "/api/v1/mis-labores",
        ):
            assert client.get(ruta).status_code == 401, ruta
