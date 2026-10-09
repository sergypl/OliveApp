"""Verificación de parcelas (feature alta-de-parcela)."""

from fastapi.testclient import TestClient

from app.main import app

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123"}


def _entrar(client):
    """Registra la cuenta de prueba y devuelve su token de sesión."""
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


def test_crear_parcela_y_verla_en_la_lista_del_socio():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        creada = client.post(
            "/api/v1/mis-parcelas", json={"nombre": "La Noria"}, headers=cabeceras
        )
        assert creada.status_code == 201
        assert creada.json()["nombre"] == "La Noria"

        nombres = [
            p["nombre"]
            for p in client.get("/api/v1/mis-parcelas", headers=cabeceras).json()
        ]
        assert "La Noria" in nombres


def test_la_parcela_creada_no_aparece_en_la_lista_de_otro_socio():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        client.post(
            "/api/v1/mis-parcelas", json={"nombre": "La Noria"}, headers=cabeceras
        )

        # Las parcelas del otro socio (sembradas) no llevan la nueva...
        mias = {
            p["nombre"]
            for p in client.get("/api/v1/mis-parcelas", headers=cabeceras).json()
        }
        del_otro = {
            p["nombre"]
            for p in client.get(
                "/api/v1/socios/socio-002/parcelas", headers=cabeceras
            ).json()
        }
        assert "La Noria" not in del_otro
        # ...ni las suyas aparecen en la lista de este socio.
        assert mias.isdisjoint(del_otro)


def test_la_parcela_se_asocia_al_socio_de_la_cuenta():
    from app import db as basedatos

    with TestClient(app) as client:
        cabeceras = _entrar(client)
        client.post(
            "/api/v1/mis-parcelas", json={"nombre": "La Noria"}, headers=cabeceras
        )
        conn = basedatos.conectar()
        try:
            fila = conn.execute(
                "SELECT socio_id FROM parcela WHERE nombre = 'La Noria'"
            ).fetchone()
        finally:
            conn.close()
        assert fila["socio_id"] == "socio-001"


def test_nombre_vacio_da_error_claro():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.post(
            "/api/v1/mis-parcelas", json={"nombre": "   "}, headers=cabeceras
        )
        assert respuesta.status_code == 400
        assert "nombre" in respuesta.json()["detail"].lower()


def test_nombre_demasiado_largo_da_error():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.post(
            "/api/v1/mis-parcelas", json={"nombre": "x" * 61}, headers=cabeceras
        )
        assert respuesta.status_code == 400
        assert "largo" in respuesta.json()["detail"].lower()


def test_falta_el_nombre():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.post("/api/v1/mis-parcelas", json={}, headers=cabeceras)
        assert respuesta.status_code == 422


def test_mis_parcelas_exige_token_valido():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        assert client.get("/api/v1/mis-parcelas").status_code == 401
        assert (
            client.post("/api/v1/mis-parcelas", json={"nombre": "X"}).status_code == 401
        )
        falso = client.get(
            "/api/v1/mis-parcelas", headers={"Authorization": "Bearer inventado"}
        )
        assert falso.status_code == 401
        assert client.get("/api/v1/mis-parcelas", headers=cabeceras).status_code == 200


def test_las_parcelas_no_se_eliminan_ni_se_archivan():
    """En el MVP no hay borrado ni archivo de parcelas (docs/domain-model.md)."""
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        client.post(
            "/api/v1/mis-parcelas", json={"nombre": "La Noria"}, headers=cabeceras
        )
        for metodo in ("delete", "patch", "put"):
            respuesta = client.request(
                metodo.upper(), "/api/v1/mis-parcelas", json={}, headers=cabeceras
            )
            assert respuesta.status_code == 405, (metodo, respuesta.status_code)
