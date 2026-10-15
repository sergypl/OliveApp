"""Verificación de campañas anteriores (feature consulta-campanas-anteriores).

Estados de Campaña (docs/domain-model.md): `actual | cerrada`; las cerradas
son anteriores y de solo consulta.
"""

from fastapi.testclient import TestClient

from app.main import app

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123", "codigo_socio": "Socio 1"}


def _entrar(client):
    """Registra la cuenta de prueba y devuelve su token de sesión."""
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


def test_el_listado_marca_la_actual_y_las_cerradas():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        campanas = client.get("/api/v1/mi-campanas", headers=cabeceras).json()

        assert len(campanas) == 4
        assert campanas[0] == {
            "id": "2025-2026",
            "nombre": "2025/2026",
            "estado": "actual",
            "aceite_obtenido": 1774.0,
        }
        cerradas = [c for c in campanas if c["estado"] == "cerrada"]
        assert [c["id"] for c in cerradas] == ["2024-2025", "2023-2024", "2022-2023"]
        # El aceite obtenido de cada campaña viaja en el listado (comparativa del panel).
        assert cerradas[0]["aceite_obtenido"] == 1198.0
        assert all(isinstance(c["aceite_obtenido"], float) for c in campanas)


def test_el_detalle_de_una_campana_anterior_separa_datos_oficiales_y_labores():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        detalle = client.get(
            "/api/v1/mi-campanas/2024-2025", headers=cabeceras
        ).json()

        assert detalle["campana"] == {
            "id": "2024-2025",
            "nombre": "2024/2025",
            "estado": "cerrada",
        }
        # Datos oficiales sembrados del socio de demostración (2024/2025).
        assert detalle["datos_oficiales"] == {
            "kilos_entregados": 6200.0,
            "rendimiento_graso": 21.4,
            "aceite_obtenido": 1198.0,
        }
        # Las labores van en su propia sección, aparte de los datos oficiales.
        assert detalle["labores"] == []
        assert set(detalle.keys()) == {"campana", "datos_oficiales", "labores"}


def test_las_campañas_son_de_solo_lectura_no_admiten_nuevas_labores():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        rutas = ["/api/v1/mi-campanas", "/api/v1/mi-campanas/2024-2025"]
        for ruta in rutas:
            for metodo in ("post", "put", "patch", "delete"):
                respuesta = client.request(
                    metodo.upper(), ruta, json={}, headers=cabeceras
                )
                assert respuesta.status_code == 405, (metodo, ruta, respuesta.status_code)


def test_el_detalle_exige_token_valido():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        assert client.get("/api/v1/mi-campanas").status_code == 401
        falso = client.get(
            "/api/v1/mi-campanas", headers={"Authorization": "Bearer inventado"}
        )
        assert falso.status_code == 401
        assert (
            client.get("/api/v1/mi-campanas/2024-2025", headers=cabeceras).status_code
            == 200
        )


def test_una_campana_inexistente_da_404():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.get("/api/v1/mi-campanas/1999-2000", headers=cabeceras)
        assert respuesta.status_code == 404
        assert "campaña" in respuesta.json()["detail"].lower()
