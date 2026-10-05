"""API de datos de OlivApp.

De momento solo el healthcheck y la validación de la configuración por
variables de entorno (la clave de la API de IA y el secreto JWT nunca viajan
en el APK: viven solo aquí).
"""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI

# Variables de entorno obligatorias del servidor (ADR 0003).
REQUIRED_ENV_VARS = ("DEVEXPERT_API_KEY", "JWT_SECRET")


def read_required_env():
    """Lee la configuración obligatoria y falla de forma explícita si falta alguna."""
    missing = [name for name in REQUIRED_ENV_VARS if not os.environ.get(name)]
    if missing:
        raise RuntimeError(
            "Faltan variables de entorno obligatorias: " + ", ".join(missing)
        )
    return {name: os.environ[name] for name in REQUIRED_ENV_VARS}


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.settings = read_required_env()
    yield


app = FastAPI(title="OlivApp API", lifespan=lifespan)


@app.get("/health")
def health():
    """Comprobación de vida del servicio para el arranque y el despliegue."""
    return {"status": "ok"}
