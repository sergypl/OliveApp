"""Perfil del socio (feature perfil-del-socio): `GET /api/v1/mi-perfil`.

Con token devuelve código de socio, email, cooperativa, número de parcelas y
campaña actual; sin token (o con token no válido) responde 401; y un socio
nunca ve los datos de otro.
"""

from fastapi.testclient import TestClient

from app.main import app

CUENTA_A = {
    "email": "perfil.uno@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 1",
}
CUENTA_B = {
    "email": "perfil.dos@ejemplo.com",
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


def test_con_token_devuelve_el_perfil_del_socio():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        cooperativa = client.get("/api/v1/cooperativa", headers=cabeceras).json()
        panel = client.get("/api/v1/mi-panel", headers=cabeceras).json()
        parcelas = client.get("/api/v1/mis-parcelas", headers=cabeceras).json()

        respuesta = client.get("/api/v1/mi-perfil", headers=cabeceras)

    assert respuesta.status_code == 200
    perfil = respuesta.json()
    assert perfil == {
        "codigo_socio": "Socio 1",
        "email": CUENTA_A["email"],
        "cooperativa": cooperativa["nombre"],
        "numero_parcelas": len(parcelas),
        "campana_actual": panel["campana"]["nombre"],
    }


def test_el_numero_de_parcelas_cuenta_las_del_socio():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        antes = client.get("/api/v1/mi-perfil", headers=cabeceras).json()
        _crear_parcela(client, cabeceras, "El Encinar nueva")
        _crear_parcela(client, cabeceras, "La Solana nueva")

        despues = client.get("/api/v1/mi-perfil", headers=cabeceras).json()

    assert despues["numero_parcelas"] == antes["numero_parcelas"] + 2


def test_sin_token_es_401():
    with TestClient(app) as client:
        respuesta = client.get("/api/v1/mi-perfil")

    assert respuesta.status_code == 401


def test_con_token_no_valido_es_401():
    with TestClient(app) as client:
        respuesta = client.get(
            "/api/v1/mi-perfil", headers={"Authorization": "Bearer no-es-un-token"}
        )

    assert respuesta.status_code == 401


def test_un_socio_no_ve_los_datos_de_otro():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)
        b_antes = client.get("/api/v1/mi-perfil", headers=cabeceras_b).json()
        a_antes = client.get("/api/v1/mi-perfil", headers=cabeceras_a).json()
        _crear_parcela(client, cabeceras_a, "Parcela de A")
        _crear_parcela(client, cabeceras_a, "Otra de A")

        perfil_a = client.get("/api/v1/mi-perfil", headers=cabeceras_a).json()
        perfil_b = client.get("/api/v1/mi-perfil", headers=cabeceras_b).json()

    assert perfil_a["codigo_socio"] == "Socio 1"
    assert perfil_a["email"] == CUENTA_A["email"]
    assert perfil_a["numero_parcelas"] == a_antes["numero_parcelas"] + 2
    assert perfil_b["codigo_socio"] == "Socio 2"
    assert perfil_b["email"] == CUENTA_B["email"]
    # Las parcelas nuevas de A no cambian el perfil de B.
    assert perfil_b == b_antes
