"""Verificación base del bootstrap del backend (healthcheck y configuración)."""

import os

import pytest
from fastapi.testclient import TestClient

from app.main import app


def test_health_responde_ok(monkeypatch):
    monkeypatch.setenv("DEVEXPERT_API_KEY", "de-prueba")
    monkeypatch.setenv("JWT_SECRET", "de-prueba")
    with TestClient(app) as client:
        respuesta = client.get("/health")
    assert respuesta.status_code == 200
    assert respuesta.json() == {"status": "ok"}


def test_arranque_falla_sin_devexpert_api_key(monkeypatch):
    monkeypatch.delenv("DEVEXPERT_API_KEY", raising=False)
    monkeypatch.setenv("JWT_SECRET", "de-prueba")
    with pytest.raises(RuntimeError, match="DEVEXPERT_API_KEY"):
        with TestClient(app):
            pass


def test_arranque_falla_sin_jwt_secret(monkeypatch):
    monkeypatch.setenv("DEVEXPERT_API_KEY", "de-prueba")
    monkeypatch.delenv("JWT_SECRET", raising=False)
    with pytest.raises(RuntimeError, match="JWT_SECRET"):
        with TestClient(app):
            pass
