"""Verificación del panel de la campaña actual (feature panel-campana-actual)."""

import sqlite3

from fastapi.testclient import TestClient

from app.main import app

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123", "codigo_socio": "Socio 1"}


def _entrar(client):
    """Registra la cuenta de prueba y devuelve su token de sesión."""
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


def test_el_panel_muestra_la_campana_actual_con_los_tres_datos_oficiales():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        panel = client.get("/api/v1/mi-panel", headers=cabeceras).json()

        # Campaña actual sembrada: 2025/2026.
        assert panel["campana"] == {"id": "2025-2026", "nombre": "2025/2026"}
        # Los tres datos oficiales del socio de demostración, tal cual el seed.
        assert panel["datos_oficiales"] == {
            "kilos_entregados": 10958.0,
            "rendimiento_graso": 17.93,
            "aceite_obtenido": 1774.0,
        }


def test_la_campana_actual_es_la_marcada_como_actual_en_los_datos():
    """No se deriva de la fecha del sistema: es la campaña marcada."""
    from app import db as basedatos

    with TestClient(app) as client:
        cabeceras = _entrar(client)
        panel = client.get("/api/v1/mi-panel", headers=cabeceras).json()

        conn = basedatos.conectar()
        try:
            marcada = conn.execute(
                "SELECT id FROM campana WHERE es_actual = 1"
            ).fetchone()
        finally:
            conn.close()
        assert panel["campana"]["id"] == marcada["id"]


def test_la_cuenta_queda_vinculada_al_socio_de_demostracion():
    from app import db as basedatos

    with TestClient(app) as client:
        _entrar(client)
        conn = basedatos.conectar()
        try:
            fila = conn.execute(
                "SELECT socio_id FROM cuenta WHERE email = ?", (DATOS["email"],)
            ).fetchone()
        finally:
            conn.close()
        assert fila["socio_id"] == "socio-001"


def test_el_panel_solo_muestra_datos_oficiales_del_propio_socio():
    """Lo que devuelve el panel son datos oficiales de cooperativa, no labores."""
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        panel = client.get("/api/v1/mi-panel", headers=cabeceras).json()
        # Solo campaña y datos oficiales: nada de labores ni cuaderno.
        assert set(panel.keys()) == {"campana", "datos_oficiales"}
        assert set(panel["datos_oficiales"].keys()) == {
            "kilos_entregados",
            "rendimiento_graso",
            "aceite_obtenido",
        }


def test_el_panel_exige_token_valido():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        assert client.get("/api/v1/mi-panel").status_code == 401
        falso = client.get(
            "/api/v1/mi-panel", headers={"Authorization": "Bearer inventado"}
        )
        assert falso.status_code == 401
        assert client.get("/api/v1/mi-panel", headers=cabeceras).status_code == 200


def test_migracion_vincula_las_cuentas_antiguas_al_socio_de_demostracion():
    """Bases creadas antes de esta feature: la migración añade la vinculación."""
    from app import db as basedatos

    conn = sqlite3.connect(":memory:")
    conn.row_factory = sqlite3.Row
    conn.execute(
        "CREATE TABLE cuenta ("
        "id TEXT PRIMARY KEY, email TEXT NOT NULL UNIQUE, "
        "contrasena_hash TEXT NOT NULL, cooperativa_id TEXT NOT NULL, "
        "creada_en TEXT NOT NULL DEFAULT (datetime('now')))"
    )
    conn.execute(
        "INSERT INTO cuenta (id, email, contrasena_hash, cooperativa_id) "
        "VALUES ('c1', 'vieja@ejemplo.com', 'x', 'coop-demo')"
    )
    basedatos.crear_esquema(conn)
    fila = conn.execute("SELECT socio_id FROM cuenta WHERE id = 'c1'").fetchone()
    assert fila["socio_id"] == "socio-001"
    conn.close()
