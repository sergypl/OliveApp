"""Verificación del inicio de sesión y de las llamadas autenticadas."""

import time

import jwt
from fastapi.testclient import TestClient

from app.main import app

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123"}


def _secreto():
    import os

    return os.environ["JWT_SECRET"]


def _registrarse(client, **cambios):
    datos = dict(DATOS, **cambios)
    assert client.post("/api/v1/registro", json=datos).status_code == 201
    return datos


def _iniciar_sesion(client, **cambios):
    datos = dict(DATOS, **cambios)
    respuesta = client.post("/api/v1/login", json=datos)
    assert respuesta.status_code == 200
    return respuesta.json()["token"]


def test_login_correcto_devuelve_token_con_caducidad():
    with TestClient(app) as client:
        _registrarse(client)
        respuesta = client.post("/api/v1/login", json=DATOS)
        assert respuesta.status_code == 200
        cuerpo = respuesta.json()
        assert cuerpo["email"] == DATOS["email"]
        assert "contrasena" not in cuerpo

        token = cuerpo["token"]
        contenido = jwt.decode(token, _secreto(), algorithms=["HS256"])
        assert contenido["email"] == DATOS["email"]
        assert contenido["exp"] > int(time.time())  # con caducidad futura


def test_contrasena_incorrecta_da_error_claro():
    with TestClient(app) as client:
        _registrarse(client)
        respuesta = client.post(
            "/api/v1/login", json={"email": DATOS["email"], "contrasena": "mala12345"}
        )
        assert respuesta.status_code == 401
        assert "incorrectos" in respuesta.json()["detail"].lower()


def test_email_inexistente_da_el_mismo_error():
    with TestClient(app) as client:
        respuesta = client.post(
            "/api/v1/login", json={"email": "nadie@ejemplo.com", "contrasena": "segura123"}
        )
        assert respuesta.status_code == 401
        assert "incorrectos" in respuesta.json()["detail"].lower()


def test_los_datos_exigen_token_valido():
    with TestClient(app) as client:
        _registrarse(client)
        token = _iniciar_sesion(client)

        # Sin token: no entra.
        assert client.get("/api/v1/socios/socio-001/campanas").status_code == 401
        # Con token inventado: no entra.
        falso = client.get(
            "/api/v1/socios/socio-001/campanas",
            headers={"Authorization": "Bearer esto-no-es-un-token"},
        )
        assert falso.status_code == 401
        # Con el token de la sesión: entra.
        ok = client.get(
            "/api/v1/socios/socio-001/campanas",
            headers={"Authorization": "Bearer " + token},
        )
        assert ok.status_code == 200


def test_token_caducado_no_da_acceso():
    with TestClient(app) as client:
        _registrarse(client)
        token = _iniciar_sesion(client)
        import jwt as pyjwt

        contenido = pyjwt.decode(token, _secreto(), algorithms=["HS256"])
        contenido["exp"] = int(time.time()) - 10
        caducado = pyjwt.encode(contenido, _secreto(), algorithm="HS256")

        respuesta = client.get(
            "/api/v1/socios/socio-001/campanas",
            headers={"Authorization": "Bearer " + caducado},
        )
        assert respuesta.status_code == 401
        assert "sesión" in respuesta.json()["detail"].lower()


def test_mi_cuenta_devuelve_la_cuenta_del_token():
    with TestClient(app) as client:
        _registrarse(client)
        token = _iniciar_sesion(client)
        respuesta = client.get(
            "/api/v1/mi-cuenta", headers={"Authorization": "Bearer " + token}
        )
        assert respuesta.status_code == 200
        assert respuesta.json() == {
            "id": respuesta.json()["id"],
            "email": DATOS["email"],
        }
        # Y también exige token.
        assert client.get("/api/v1/mi-cuenta").status_code == 401


def test_login_y_registro_siguen_sin_exigir_token():
    with TestClient(app) as client:
        assert client.post(
            "/api/v1/registro", json=DATOS
        ).status_code == 201  # registro público
        assert client.post("/api/v1/login", json=DATOS).status_code == 200
        assert client.get("/health").status_code == 200
