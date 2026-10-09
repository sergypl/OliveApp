"""Verificación del registro de cuentas de socio (feature registro-cuenta-socio)."""

from fastapi.testclient import TestClient

from app import db as basedatos
from app.main import app

REGISTRO = "/api/v1/registro"


def test_registro_crea_cuenta_con_contrasena_hasheada():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO, json={"email": "nuevo@ejemplo.com", "contrasena": "segura123"}
        )
        assert respuesta.status_code == 201
        cuerpo = respuesta.json()
        assert cuerpo["email"] == "nuevo@ejemplo.com"
        assert "contrasena" not in cuerpo
        assert "contrasena_hash" not in cuerpo

        conn = basedatos.conectar()
        try:
            fila = conn.execute(
                "SELECT * FROM cuenta WHERE email = ?", ("nuevo@ejemplo.com",)
            ).fetchone()
        finally:
            conn.close()
        assert fila is not None
        assert fila["contrasena_hash"] != "segura123"
        assert fila["contrasena_hash"].startswith("pbkdf2_sha256$")
        assert fila["cooperativa_id"] == "coop-demo"


def test_dos_registraciones_del_mismo_email_da_error_claro():
    with TestClient(app) as client:
        datos = {"email": "repetido@ejemplo.com", "contrasena": "segura123"}
        assert client.post(REGISTRO, json=datos).status_code == 201

        repetida = client.post(REGISTRO, json=datos)
        assert repetida.status_code == 409
        assert "registrado" in repetida.json()["detail"].lower()

        # El mismo email con mayúsculas también choca (unicidad sin distinguir).
        en_mayusculas = client.post(
            REGISTRO, json={"email": "REPETIDO@Ejemplo.com", "contrasena": "otra12345"}
        )
        assert en_mayusculas.status_code == 409


def test_contrasena_corta_da_error_claro():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO, json={"email": "corta@ejemplo.com", "contrasena": "1234567"}
        )
        assert respuesta.status_code == 400
        assert "contraseña" in respuesta.json()["detail"].lower()


def test_email_invalido_da_error_clarito():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO, json={"email": "esto-no-es-un-email", "contrasena": "segura123"}
        )
        assert respuesta.status_code == 400
        assert "email" in respuesta.json()["detail"].lower()


def test_faltan_campos():
    with TestClient(app) as client:
        assert client.post(REGISTRO, json={"email": "solo@ejemplo.com"}).status_code == 422
        assert client.post(REGISTRO, json={"contrasena": "segura123"}).status_code == 422


def test_dos_contrasenas_distintas_producen_hashes_distintos():
    with TestClient(app) as client:
        assert client.post(
            REGISTRO, json={"email": "uno@ejemplo.com", "contrasena": "mismapass1"}
        ).status_code == 201
        assert client.post(
            REGISTRO, json={"email": "dos@ejemplo.com", "contrasena": "mismapass1"}
        ).status_code == 201

        conn = basedatos.conectar()
        try:
            filas = conn.execute(
                "SELECT contrasena_hash FROM cuenta WHERE email IN (?, ?)",
                ("uno@ejemplo.com", "dos@ejemplo.com"),
            ).fetchall()
        finally:
            conn.close()
        hashes = [f["contrasena_hash"] for f in filas]
        assert len(hashes) == 2
        assert hashes[0] != hashes[1]  # sal distinta para cada cuenta
