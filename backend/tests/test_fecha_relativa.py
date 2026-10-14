"""fecha-relativa-en-borrador: el borrador IA resuelve las fechas relativas
sencillas ("hoy", "ayer", "anteayer") a un día concreto contra la fecha de hoy,
y deja las fechas ambiguas tal cual para que el socio elija la fecha en el
calendario antes de confirmar (decisión de la feature)."""

import json

from app import borrador


def test_las_fechas_relativas_sencillas_se_resuelven_a_un_dia_concreto():
    assert borrador.resolver_fecha("hoy", "2026-10-14") == "2026-10-14"
    assert borrador.resolver_fecha("ayer", "2026-10-14") == "2026-10-13"
    assert borrador.resolver_fecha("anteayer", "2026-10-14") == "2026-10-12"
    assert borrador.resolver_fecha("Ayer", "2026-10-14") == "2026-10-13"
    assert borrador.resolver_fecha("AYER", "2026-10-14") == "2026-10-13"


def test_las_fechas_concretas_se_quedan_tal_cual():
    assert borrador.resolver_fecha("2026-01-20", "2026-10-14") == "2026-01-20"


def test_las_fechas_ambiguas_quedan_sin_resolver():
    assert (
        borrador.resolver_fecha("el 5 de diciembre", "2026-10-14")
        == "el 5 de diciembre"
    )
    assert (
        borrador.resolver_fecha("la semana pasada", "2026-10-14") == "la semana pasada"
    )
    assert borrador.resolver_fecha("", "2026-10-14") == ""


def test_el_borrador_llega_con_la_fecha_relativa_resuelta(monkeypatch):
    def falso(api_key, mensajes):
        return json.dumps(
            {
                "fecha": "ayer",
                "parcela": "El Encinar",
                "categoria": "tratamiento",
                "campos": {
                    "producto": "cobre",
                    "plaga_a_controlar": "mosca del olivo",
                    "cantidad": 2,
                    "unidad": "L",
                },
                "observaciones": "",
            }
        )

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)

    ficha = borrador.extraer(
        "clave", "Ayer eché un tratamiento en El Encinar", ["El Encinar"],
        hoy="2026-10-14",
    )

    assert ficha["fecha"] == "2026-10-13"


def test_si_la_fecha_dictada_es_ambigua_el_borrador_la_deja_tal_cual(monkeypatch):
    def falso(api_key, mensajes):
        return json.dumps(
            {
                "fecha": "la semana pasada",
                "parcela": "El Encinar",
                "categoria": "tratamiento",
                "campos": {
                    "producto": "cobre",
                    "plaga_a_controlar": "mosca del olivo",
                    "cantidad": 2,
                    "unidad": "L",
                },
                "observaciones": "",
            }
        )

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)

    ficha = borrador.extraer(
        "clave", "La semana pasada eché un tratamiento en El Encinar",
        ["El Encinar"], hoy="2026-10-14",
    )

    assert ficha["fecha"] == "la semana pasada"
