"""Aislamiento de las pruebas: cada test usa su propia base de datos temporal
y unas variables de entorno obligatorias de prueba (los tests que verifican el
fallo explícito las borran después). Además, ningún test debe llamar al
gateway de IA real: la extracción del borrador IA (alias `chat`) se sustituye
por defecto con un borrador de prueba, y cada test puede sobrescribirla con su
propio fake."""

import json

import pytest

from app import borrador

BORRADOR_POR_DEFECTO = {
    "fecha": "2026-01-20",
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


@pytest.fixture(autouse=True)
def entorno_de_prueba(tmp_path, monkeypatch):
    monkeypatch.setenv("OLIVAPP_DB", str(tmp_path / "olivapp-test.db"))
    monkeypatch.setenv("DEVEXPERT_API_KEY", "de-prueba")
    monkeypatch.setenv("JWT_SECRET", "de-prueba")


@pytest.fixture(autouse=True)
def sin_gateway_de_borrador(monkeypatch):
    def falso(api_key, mensajes):
        return json.dumps(BORRADOR_POR_DEFECTO)

    monkeypatch.setattr(borrador, "_llamar_al_gateway", falso)
