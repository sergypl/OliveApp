"""Verificación del registro de cuentas de socio (feature registro-cuenta-socio).

Desde `aislamiento-de-datos-por-socio` el registro exige el código de socio
de la cooperativa y vincula la cuenta a ese socio.
"""

from fastapi.testclient import TestClient

from app import cuentas
from app import db as basedatos
from app.main import app

REGISTRO = "/api/v1/registro"


def test_registro_crea_cuenta_con_contrasena_hasheada():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "nuevo@ejemplo.com",
                "contrasena": "segura123",
                "codigo_socio": "Socio 1",
            },
        )
        assert respuesta.status_code == 201
        cuerpo = respuesta.json()
        assert cuerpo["email"] == "nuevo@ejemplo.com"
        assert cuerpo["socio_id"] == "socio-001"
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
        assert fila["socio_id"] == "socio-001"


def test_dos_registraciones_del_mismo_email_da_error_claro():
    with TestClient(app) as client:
        datos = {
            "email": "repetido@ejemplo.com",
            "contrasena": "segura123",
            "codigo_socio": "Socio 1",
        }
        assert client.post(REGISTRO, json=datos).status_code == 201

        repetida = client.post(REGISTRO, json=datos)
        assert repetida.status_code == 409
        assert "registrado" in repetida.json()["detail"].lower()

        # El mismo email con mayúsculas también choca (unicidad sin distinguir).
        en_mayusculas = client.post(
            REGISTRO,
            json={
                "email": "REPETIDO@Ejemplo.com",
                "contrasena": "otra12345",
                "codigo_socio": "Socio 1",
            },
        )
        assert en_mayusculas.status_code == 409


def test_contrasena_corta_da_error_claro():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "corta@ejemplo.com",
                "contrasena": "1234567",
                "codigo_socio": "Socio 1",
            },
        )
        assert respuesta.status_code == 400
        assert "contraseña" in respuesta.json()["detail"].lower()


def test_email_invalido_da_error_clarito():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "esto-no-es-un-email",
                "contrasena": "segura123",
                "codigo_socio": "Socio 1",
            },
        )
        assert respuesta.status_code == 400
        assert "email" in respuesta.json()["detail"].lower()


def test_faltan_campos():
    with TestClient(app) as client:
        assert client.post(
            REGISTRO, json={"email": "solo@ejemplo.com"}
        ).status_code == 422
        assert client.post(
            REGISTRO, json={"contrasena": "segura123"}
        ).status_code == 422
        # Sin código de socio tampoco se puede crear la cuenta.
        assert client.post(
            REGISTRO,
            json={"email": "sin-codigo@ejemplo.com", "contrasena": "segura123"},
        ).status_code == 422


def test_dos_contrasenas_distintas_producen_hashes_distintos():
    with TestClient(app) as client:
        assert client.post(
            REGISTRO,
            json={
                "email": "uno@ejemplo.com",
                "contrasena": "mismapass1",
                "codigo_socio": "Socio 1",
            },
        ).status_code == 201
        assert client.post(
            REGISTRO,
            json={
                "email": "dos@ejemplo.com",
                "contrasena": "mismapass1",
                "codigo_socio": "Socio 1",
            },
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


# --- Vinculación cuenta ↔ socio (feature aislamiento-de-datos-por-socio) ---


def test_el_codigo_de_socio_vincula_la_cuenta_a_ese_socio():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "socio2@ejemplo.com",
                "contrasena": "segura123",
                "codigo_socio": "Socio 2",
            },
        )
        assert respuesta.status_code == 201
        assert respuesta.json()["socio_id"] == "socio-002"

        conn = basedatos.conectar()
        try:
            fila = conn.execute(
                "SELECT socio_id FROM cuenta WHERE email = ?", ("socio2@ejemplo.com",)
            ).fetchone()
        finally:
            conn.close()
        assert fila["socio_id"] == "socio-002"


def test_el_codigo_de_socio_no_distingue_mayusculas_ni_espacios():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "flexible@ejemplo.com",
                "contrasena": "segura123",
                "codigo_socio": "  socio   2 ",
            },
        )
        assert respuesta.status_code == 201
        assert respuesta.json()["socio_id"] == "socio-002"


def test_codigo_de_socio_desconocido_da_error_claro_y_no_crea_cuenta():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "nadie@ejemplo.com",
                "contrasena": "segura123",
                "codigo_socio": "Socio 99",
            },
        )
        assert respuesta.status_code == 400
        assert "código de socio" in respuesta.json()["detail"].lower()

        conn = basedatos.conectar()
        try:
            fila = conn.execute(
                "SELECT 1 FROM cuenta WHERE email = ?", ("nadie@ejemplo.com",)
            ).fetchone()
        finally:
            conn.close()
        assert fila is None  # la cuenta no se crea


def test_codigo_de_socio_vacio_da_error_claro():
    with TestClient(app) as client:
        respuesta = client.post(
            REGISTRO,
            json={
                "email": "vacio@ejemplo.com",
                "contrasena": "segura123",
                "codigo_socio": "   ",
            },
        )
        assert respuesta.status_code == 400
        assert "código de socio" in respuesta.json()["detail"].lower()


def test_si_dos_registros_coinciden_el_segundo_recibe_error_claro(monkeypatch):
    """Carrera real: dos altas a la vez con el mismo email.

    La comprobación previa de email duplicado puede no ver la cuenta que la
    otra petición acaba de crear: el alta debe seguir respondiendo un error
    claro (409) y no un 500 por la restricción única de la base.
    """
    with TestClient(app) as client:
        datos = {
            "email": "carrera@ejemplo.com",
            "contrasena": "segura123",
            "codigo_socio": "Socio 1",
        }
        assert client.post(REGISTRO, json=datos).status_code == 201

        # La otra petición crea la cuenta entre la comprobación y el alta.
        monkeypatch.setattr(cuentas, "_existe_email", lambda conn, email: False)
        repetida = client.post(REGISTRO, json=datos)

        assert repetida.status_code == 409
        assert "registrado" in repetida.json()["detail"].lower()
