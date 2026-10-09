"""Aislamiento de las pruebas: cada test usa su propia base de datos temporal
y unas variables de entorno obligatorias de prueba (los tests que verifican el
fallo explícito las borran después)."""

import pytest


@pytest.fixture(autouse=True)
def entorno_de_prueba(tmp_path, monkeypatch):
    monkeypatch.setenv("OLIVAPP_DB", str(tmp_path / "olivapp-test.db"))
    monkeypatch.setenv("DEVEXPERT_API_KEY", "de-prueba")
    monkeypatch.setenv("JWT_SECRET", "de-prueba")
