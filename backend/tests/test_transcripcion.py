"""Verificación de la transcripción de notas de voz (feature transcripcion-de-nota-voz).

Reglas (ADR 0004): la app envía el audio, el backend lo transcribe con el
alias `transcribe` del gateway y borra el audio del servidor; si falla, la
nota queda pendiente de reintentar.
"""

import os

from fastapi.testclient import TestClient

from app import transcripcion
from app.main import app

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123"}


def _entrar(client):
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


def _subir(client, cabeceras, nombre="nota.m4a", contenido=b"audio-de-prueba"):
    return client.post(
        "/api/v1/mis-notas-voz/transcripcion",
        files={"audio": (nombre, contenido, "audio/mp4")},
        headers=cabeceras,
    )


def _falso_gateway(texto="He hecho un tratamiento en La Noria."):
    """Sustituye la llamada al gateway y devuelve el texto pedido."""

    def falso(api_key, ruta, nombre):
        falso.ultima_ruta = ruta
        return texto

    return falso


def test_transcribe_un_audio_y_lo_borra_del_servidor(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        falso = _falso_gateway()
        monkeypatch.setattr(transcripcion, "_llamar_al_gateway", falso)

        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 200, respuesta.text
        assert respuesta.json() == {"texto": "He hecho un tratamiento en La Noria."}
        # El audio no se conserva en el servidor tras transcribir.
        assert not os.path.exists(falso.ultima_ruta)


def test_usa_el_alias_transcribe_y_devuelve_el_texto(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)

        class Respuesta:
            status_code = 200

            def json(self):
                return {"text": "hola"}

        llamada = {}

        def falso_post(url, **kwargs):
            llamada["url"] = url
            llamada["modelo"] = kwargs["data"]["model"]
            return Respuesta()

        monkeypatch.setattr(transcripcion.httpx, "post", falso_post)
        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 200
        assert respuesta.json() == {"texto": "hola"}
        assert llamada["url"] == transcripcion.GATEWAY_TRANSCRIPCIONES
        assert llamada["modelo"] == transcripcion.MODELO == "transcribe"


def test_exige_token_valido(monkeypatch):
    with TestClient(app) as client:
        sin_token = _subir(client, {})
        assert sin_token.status_code == 401
        falso = client.post(
            "/api/v1/mis-notas-voz/transcripcion",
            files={"audio": ("nota.m4a", b"x", "audio/mp4")},
            headers={"Authorization": "Bearer inventado"},
        )
        assert falso.status_code == 401
        cabeceras = _entrar(client)
        monkeypatch.setattr(transcripcion, "_llamar_al_gateway", _falso_gateway())
        assert _subir(client, cabeceras).status_code == 200


def test_exige_fichero_de_audio():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.post("/api/v1/mis-notas-voz/transcripcion", headers=cabeceras)
        assert respuesta.status_code == 422


def test_rechaza_formato_desconocido():
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = _subir(client, cabeceras, nombre="nota.txt")
        assert respuesta.status_code == 400
        assert "formato" in respuesta.json()["detail"].lower()


def test_rechaza_audio_demasiado_grande(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(transcripcion, "LIMITE_BYTES", 10)
        respuesta = _subir(client, cabeceras, contenido=b"0123456789012345")
        assert respuesta.status_code == 400
        assert "grande" in respuesta.json()["detail"].lower()


def test_audio_sin_texto_pide_repetir(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(transcripcion, "_llamar_al_gateway", _falso_gateway(texto="   "))
        respuesta = _subir(client, cabeceras)
        assert respuesta.status_code == 400
        assert "vuelve a grabar" in respuesta.json()["detail"].lower()


def test_fallo_del_gateway_da_error_claro_y_reintentable(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)

        def falso(api_key, ruta, nombre):
            raise transcripcion.TranscripcionFallida("gateway caído")

        monkeypatch.setattr(transcripcion, "_llamar_al_gateway", falso)
        respuesta = _subir(client, cabeceras)
        assert respuesta.status_code == 502
        assert "inténtalo de nuevo" in respuesta.json()["detail"].lower()
