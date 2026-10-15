"""Verificación del borrador IA (feature borrador-ia-desde-transcripcion).

Reglas (ADR 0004 y docs/domain-model.md): la transcripción se extrae a una
ficha {fecha, parcela, categoría, campos de la categoría} con salida JSON
forzada, apoyándose en las parcelas del socio; si la IA no reconoce la
categoría, el borrador se rechaza con motivo claro.
"""

import json

import httpx
import pytest
from fastapi.testclient import TestClient

from app import borrador
from app import transcripcion
from app.main import app
# La implementación real del gateway de borrador, antes de que los tests la
# sustituyan por sus fakes (para poder interceptar la llamada HTTP).
from app.borrador import _llamar_al_gateway as llamar_de_verdad

PARCELAS = ["El Encinar", "Los Llanos", "Cañada Honda", "La Umbría"]

BORRADOR = {
    "fecha": "2026-10-10",
    "parcela": "El Encinar",
    "categoria": "riego",
    "campos": {"cantidad": 3, "unidad": "horas"},
    "observaciones": "",
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


def _subir(client, cabeceras):
    return client.post(
        "/api/v1/mis-notas-voz/transcripcion",
        files={"audio": ("nota.m4a", b"audio-de-prueba", "audio/mp4")},
        headers=cabeceras,
    )


def test_extrae_la_ficha_de_un_dictado(monkeypatch):
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "fecha": "2026-01-20",
                "parcela": "La Noria",
                "categoria": "tratamiento fitosanitario",
                "campos": {
                    "producto": "cobre",
                    "plaga_a_controlar": "mosca del olivo",
                    "cantidad": "2",
                    "unidad": "litros",
                },
                "observaciones": "",
            }
        ),
    )

    ficha = borrador.extraer("clave", "He hecho un tratamiento...", PARCELAS)

    assert ficha["fecha"] == "2026-01-20"
    assert ficha["categoria"] == "tratamiento"
    # Unidad normalizada a la lista cerrada y cantidad como número.
    assert ficha["campos"] == {
        "producto": "cobre",
        "plaga_a_controlar": "mosca del olivo",
        "cantidad": 2.0,
        "unidad": "L",
    }


def test_la_parcela_se_ajusta_a_las_del_socio(monkeypatch):
    # "Lanoria" es un typo de transcripción de "La Noria" (ADR 0004): si el
    # socio no tiene esa parcela, el borrador queda sin parcela.
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "fecha": "2026-01-20",
                "parcela": "lanoria",
                "categoria": "riego",
                "campos": {"cantidad": 3, "unidad": "horas"},
            }
        ),
    )

    ficha = borrador.extraer("clave", "...", ["La Noria", "El Romeral"])
    assert ficha["parcela"] == "La Noria"

    ficha = borrador.extraer("clave", "...", ["El Encinar"])
    assert ficha["parcela"] == ""


def test_rechaza_cuando_no_reconoce_la_categoria(monkeypatch):
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {"fecha": "2026-01-20", "parcela": "El Encinar", "categoria": "poda",
             "campos": {"algo": 1}}
        ),
    )

    with pytest.raises(borrador.CategoriaNoReconocida):
        borrador.extraer("clave", "He podado los olivos...", PARCELAS)


def test_rechaza_cuando_no_hay_categoria(monkeypatch):
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta({"fecha": "", "parcela": None, "categoria": None, "campos": {}}),
    )

    with pytest.raises(borrador.CategoriaNoReconocida):
        borrador.extraer("clave", "Hoy he visto pajaritos...", PARCELAS)


def test_si_faltan_campos_el_borrador_llega_con_su_pregunta(monkeypatch):
    # Relleno conversacional (ADR 0005): si el dictado no trae todos los
    # campos obligatorios, el borrador sale con la primera pregunta.
    monkeypatch.setattr(
        borrador,
        "_llamar_al_gateway",
        _respuesta(
            {
                "fecha": "2026-01-20",
                "parcela": "La Noria",
                "categoria": "tratamiento",
                "campos": {"plaga_a_controlar": "mosca del olivo"},
                "preguntas": {
                    "producto": "¿Qué producto has echado?",
                    "cantidad": "¿Cuánta cantidad has echado?",
                },
            }
        ),
    )

    ficha = borrador.extraer("clave", "He hecho un tratamiento...", ["La Noria"])

    assert borrador.campos_faltantes(ficha) == ("producto", "cantidad", "unidad")
    assert ficha["pregunta"] == "¿Qué producto has echado?"


def test_reintenta_si_el_json_no_es_valido(monkeypatch):
    intentos = []

    def falso(api_key, mensajes):
        intentos.append(mensajes)
        if len(intentos) == 1:
            return "no soy un json"
        return json.dumps(
            {
                "fecha": "2026-03-05",
                "parcela": "El Encinar",
                "categoria": "cosecha",
                "campos": {"modalidad": "a vuelo", "kilos": "800"},
            }
        )

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
    ficha = borrador.extraer("clave", "...", PARCELAS)

    assert len(intentos) == 2
    assert ficha["categoria"] == "cosecha"
    assert ficha["campos"] == {"modalidad": "a vuelo", "kilos": 800.0}


def test_si_el_gateway_falla_queda_error_reintentable(monkeypatch):
    def falso(api_key, mensajes):
        raise borrador.ExtraccionFallida("gateway caído")

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
    with pytest.raises(borrador.ExtraccionFallida):
        borrador.extraer("clave", "...", PARCELAS)


def test_el_endpoint_devuelve_texto_y_borrador(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "He regado El Encinar."
        )
        monkeypatch.setattr(
            borrador,
            "_llamar_al_gateway",
            _respuesta(
                {
                    "fecha": "2026-10-10",
                    "parcela": "El Encinar",
                    "categoria": "riego",
                    "campos": {"cantidad": 3, "unidad": "horas"},
                    "observaciones": "",
                }
            ),
        )

        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 200, respuesta.text
        # Cambio declarado de completar-borrador-por-preguntas: el borrador
        # lleva siempre su `pregunta` (vacía cuando la ficha ya está completa).
        assert respuesta.json() == {
            "texto": "He regado El Encinar.",
            "borrador": {
                "fecha": "2026-10-10",
                "parcela": "El Encinar",
                "categoria": "riego",
                "campos": {"cantidad": 3.0, "unidad": "horas"},
                "observaciones": "",
                "pregunta": "",
            },
        }


def test_el_endpoint_rechaza_el_borrador_sin_categoria(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "Hoy he visto pajaritos."
        )
        monkeypatch.setattr(
            borrador,
            "_llamar_al_gateway",
            _respuesta({"fecha": "", "parcela": None, "categoria": None, "campos": {}}),
        )

        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 200
        assert respuesta.json() == {
            "texto": "Hoy he visto pajaritos.",
            "borrador": None,
        }


def test_si_falla_la_extraccion_da_502_reintentable(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "He regado El Encinar."
        )

        def falso(api_key, mensajes):
            raise borrador.ExtraccionFallida("gateway caído")

        monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 502
        assert "borrador" in respuesta.json()["detail"].lower()


def test_usa_el_alias_chat_con_salida_json(monkeypatch):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        monkeypatch.setattr(
            transcripcion, "_llamar_al_gateway", lambda k, r, n: "He regado El Encinar."
        )
        llamada = {}

        class Respuesta:
            status_code = 200

            def json(self):
                return {
                    "choices": [
                        {"message": {"content": json.dumps(BORRADOR)}}
                    ]
                }

        def falso_post(url, **kwargs):
            llamada["url"] = url
            llamada["modelo"] = kwargs["json"]["model"]
            llamada["formato"] = kwargs["json"]["response_format"]
            return Respuesta()

        # Se vuelve a la implementación real para interceptar la llamada HTTP.
        monkeypatch.setattr(borrador, "_llamar_al_gateway", llamar_de_verdad)
        monkeypatch.setattr(borrador.httpx, "post", falso_post)
        respuesta = _subir(client, cabeceras)

        assert respuesta.status_code == 200
        assert llamada["url"] == borrador.GATEWAY_CHAT
        assert llamada["modelo"] == borrador.MODELO == "chat"
        assert llamada["formato"] == {"type": "json_object"}


def test_si_el_json_no_es_objeto_se_reintenta_y_falla_con_error_claro(monkeypatch):
    """La salida del modelo no es de fiar: también puede ser una lista.

    Una lista no es una ficha: se reintenta (plan B del ADR 0004) y, si
    insiste, la extracción falla con un error claro en vez de reventar.
    """
    intentos = []

    def falso(api_key, mensajes):
        intentos.append(mensajes)
        return "[1, 2]"

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
    with pytest.raises(borrador.ExtraccionFallida):
        borrador.extraer("clave", "...", PARCELAS)

    assert len(intentos) == 2


def test_los_campos_que_no_son_objeto_se_tratan_como_vacios():
    ficha = {"categoria": "tratamiento", "campos": ["producto"]}

    assert set(borrador.campos_faltantes(ficha)) == set(
        borrador.CAMPOS_POR_CATEGORIA["tratamiento"]
    )
    assert borrador._normalizar_campos("tratamiento", "producto") == {}
