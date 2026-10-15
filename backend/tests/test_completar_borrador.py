"""Verificación del relleno conversacional del borrador IA
(feature completar-borrador-por-preguntas).

Reglas (ADR 0005 y docs/domain-model.md): cuando el dictado no trae todos los
campos obligatorios, el asistente pregunta por ellos (una pregunta por
respuesta), el socio contesta por voz y el borrador se completa con cada
respuesta. Mientras falte un campo obligatorio hay pregunta; cuando ya no
falta ninguno, la pregunta queda vacía: el borrador está listo para confirmar.
"""

import json

import pytest
from fastapi.testclient import TestClient

from app import borrador
from app import transcripcion
from app.main import app

BASICO = {
    "fecha": "2026-10-10",
    "parcela": "Cañada Honda",
    "categoria": "tratamiento",
    "campos": {"plaga_a_controlar": "mosca del olivo"},
    "observaciones": "",
    "pregunta": "¿Qué producto has echado?",
}

DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123", "codigo_socio": "Socio 1"}


def _respuesta(texto):
    """Sustituye la llamada al gateway y devuelve la respuesta JSON pedida."""

    def falso(api_key, mensajes):
        falso.llamadas = getattr(falso, "llamadas", 0) + 1
        return json.dumps(texto)

    return falso


def _entrar(client):
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


def _completar(client, cabeceras, ficha=BASICO):
    return client.post(
        "/api/v1/borradores/completar",
        files={"audio": ("respuesta.m4a", b"audio-de-prueba", "audio/mp4")},
        data={"borrador": json.dumps(ficha)},
        headers=cabeceras,
    )


def test_la_respuesta_completa_los_campos_y_pregunta_lo_siguiente(monkeypatch):
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "campos": {"producto": "cobre"},
                "preguntas": {"cantidad": "¿Cuánta cantidad has echado?"},
            }
        ),
    )

    ficha = borrador.completar("clave", dict(BASICO), "Pues he echado cobre.")

    assert ficha["campos"] == {
        "plaga_a_controlar": "mosca del olivo",
        "producto": "cobre",
    }
    # Sigue faltando la cantidad: el asistente pregunta por ella.
    assert ficha["pregunta"] == "¿Cuánta cantidad has echado?"
    assert borrador.campos_faltantes(ficha) == ("cantidad", "unidad")


def test_una_respuesta_puede_aportar_varios_campos(monkeypatch):
    # "Dos litros de cobre" trae producto, cantidad y unidad de golpe.
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "campos": {
                    "producto": "cobre",
                    "cantidad": "2",
                    "unidad": "litros",
                    "plaga_a_controlar": "repilo",
                },
                "preguntas": {},
            }
        ),
    )

    ficha = borrador.completar("clave", dict(BASICO), "Dos litros de cobre.")

    assert ficha["campos"] == {
        "producto": "cobre",
        "cantidad": 2.0,
        "unidad": "L",
        "plaga_a_controlar": "mosca del olivo",
    }
    # Ficha completa: sin preguntas, lista para confirmar.
    assert ficha["pregunta"] == ""
    assert borrador.campos_faltantes(ficha) == ()


def test_si_la_respuesta_no_aporta_el_dato_se_vuelve_a_preguntar(monkeypatch):
    monkeypatch.setattr(
        borrador, "_llamar_al_gateway", _respuesta({"campos": {}, "preguntas": {}})
    )

    ficha = borrador.completar("clave", dict(BASICO), "Hoy he visto pajaritos.")

    assert ficha["campos"] == {"plaga_a_controlar": "mosca del olivo"}
    # La pregunta de respaldo vuelve a por el primer campo que falta.
    assert ficha["pregunta"] == "¿Qué producto has echado?"


def test_la_pregunta_de_respaldo_se_usa_cuando_la_ia_no_la_redacta(monkeypatch):
    monkeypatch.setattr(
        borrador, "_llamar_al_gateway", _respuesta({"campos": {"producto": "cobre"}})
    )

    ficha = borrador.completar("clave", dict(BASICO), "Cobre.")

    assert borrador.campos_faltantes(ficha) == ("cantidad", "unidad")
    assert ficha["pregunta"] == "¿Cuánta cantidad has echado?"


def test_no_se_tocan_los_campos_que_ya_venian_del_dictado(monkeypatch):
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "campos": {
                    "plaga_a_controlar": "repilo",
                    "producto": "azufre",
                    "modalidad": "a vuelo",
                },
                "preguntas": {},
            }
        ),
    )

    ficha = borrador.completar("clave", dict(BASICO), "Azufre.")

    # La plaga ya venía dictada y los campos de otras categorías no entran.
    assert ficha["campos"]["plaga_a_controlar"] == "mosca del olivo"
    assert ficha["campos"]["producto"] == "azufre"


def test_si_no_falta_nada_no_se_llama_a_la_ia(monkeypatch):
    completo = dict(BASICO)
    completo["campos"] = {
        "producto": "cobre",
        "plaga_a_controlar": "mosca del olivo",
        "cantidad": 2.0,
        "unidad": "L",
    }
    falso = _respuesta({})
    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)

    ficha = borrador.completar("clave", completo, "Lo que sea.")

    assert getattr(falso, "llamadas", 0) == 0
    assert ficha["pregunta"] == ""


def test_el_endpoint_devuelve_texto_y_borrador_completado(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "Dos litros de cobre."
        )
        monkeypatch.setattr(
            borrador,
            "_llamar_al_gateway",
            _respuesta(
                {
                    "campos": {
                        "producto": "cobre",
                        "cantidad": "2",
                        "unidad": "litros",
                    },
                    "preguntas": {},
                }
            ),
        )

        respuesta = _completar(client, cabeceras)

        assert respuesta.status_code == 200, respuesta.text
        assert respuesta.json() == {
            "texto": "Dos litros de cobre.",
            "borrador": {
                "fecha": "2026-10-10",
                "parcela": "Cañada Honda",
                "categoria": "tratamiento",
                "campos": {
                    "plaga_a_controlar": "mosca del olivo",
                    "producto": "cobre",
                    "cantidad": 2.0,
                    "unidad": "L",
                },
                "observaciones": "",
                "pregunta": "",
            },
        }


def test_el_endpoint_devuelve_la_siguiente_pregunta(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "Cobre."
        )
        monkeypatch.setattr(
            borrador,
            "_llamar_al_gateway",
            _respuesta(
                {
                    "campos": {"producto": "cobre"},
                    "preguntas": {"cantidad": "¿Cuánta cantidad has echado?"},
                }
            ),
        )

        respuesta = _completar(client, cabeceras)

        assert respuesta.status_code == 200, respuesta.text
        assert respuesta.json()["borrador"]["pregunta"] == "¿Cuánta cantidad has echado?"


def test_audio_no_usable_da_400_para_volver_a_responder(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "   "
        )

        respuesta = _completar(client, cabeceras)

        assert respuesta.status_code == 400
        assert "Vuelve a responder" in respuesta.json()["detail"]


def test_fallo_de_transcripcion_da_502_reintentable(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)

        def falso(api_key, ruta, nombre):
            raise transcripcion.TranscripcionFallida("gateway caído")

        monkeypatch.setattr(transcripcion, "_llamar_al_gateway", falso)
        respuesta = _completar(client, cabeceras)

        assert respuesta.status_code == 502
        assert "respuesta" in respuesta.json()["detail"].lower()


def test_fallo_de_completado_da_502_reintentable(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "Cobre."
        )

        def falso(api_key, mensajes):
            raise borrador.ExtraccionFallida("gateway caído")

        monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
        respuesta = _completar(client, cabeceras)

        assert respuesta.status_code == 502
        assert "borrador" in respuesta.json()["detail"].lower()


@pytest.mark.parametrize(
    "ficha",
    [
        "no soy un json",
        json.dumps({"rechazado": True}),
        json.dumps({"categoria": "poda", "campos": {}}),
        json.dumps({"campos": {}}),
    ],
)
def test_borrador_que_no_se_puede_completar_da_400(ficha):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.post(
            "/api/v1/borradores/completar",
            files={"audio": ("respuesta.m4a", b"audio-de-prueba", "audio/mp4")},
            data={"borrador": ficha},
            headers=cabeceras,
        )

        assert respuesta.status_code == 400
        assert "Vuelve a dictar la nota" in respuesta.json()["detail"]


def test_exige_token_valido():
    with TestClient(app) as client:
        respuesta = _completar(client, {})

        assert respuesta.status_code == 401


@pytest.mark.parametrize("campos", [["producto"], "producto", 3])
def test_la_ficha_con_campos_que_no_son_objeto_se_rechaza(campos):
    """La ficha que llega del cliente no es de fiar: `campos` debe ser objeto.

    Con una lista o un texto el relleno conversacional revienta con un 500;
    ahora el borrador se rechaza con el error claro de siempre (400).
    """
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = _completar(client, cabeceras, dict(BASICO, campos=campos))

        assert respuesta.status_code == 400
        assert "Vuelve a dictar la nota" in respuesta.json()["detail"]
