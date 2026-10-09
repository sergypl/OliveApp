"""API de datos de OlivApp.

De momento: healthcheck, validación de la configuración por variables de
entorno (la clave de la API de IA y el secreto JWT nunca viajan en el APK) y
los datos oficiales de la cooperativa sembrados, **solo lectura** (ADR 0006).
"""

import os
import sqlite3
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel

from app import cuentas
from app import db as basedatos
from app import seed

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
    # Esquema y seed al arrancar; cada petición usará su propia conexión.
    conn = basedatos.conectar()
    try:
        basedatos.crear_esquema(conn)
        seed.cargar_si_vacia(conn)
    finally:
        conn.close()
    yield


def conexion():
    """Conexión a la base de datos para una petición (se cierra al terminar)."""
    conn = basedatos.conectar()
    try:
        yield conn
    finally:
        conn.close()


app = FastAPI(title="OlivApp API", lifespan=lifespan)


class RegistroEntrada(BaseModel):
    email: str
    contrasena: str


@app.post("/api/v1/registro", status_code=201)
def registro(entrada: RegistroEntrada, conn: sqlite3.Connection = Depends(conexion)):
    """El socio crea su cuenta con email y contraseña (asociada a la cooperativa).

    La contraseña se guarda hasheada y nunca se devuelve. Sin verificación de
    email ni reset de contraseña en el MVP (limitación declarada).
    """
    cooperativa = conn.execute("SELECT id FROM cooperativa").fetchone()
    try:
        return cuentas.crear_cuenta(
            conn, cooperativa["id"], entrada.email, entrada.contrasena
        )
    except cuentas.DatosInvalidos as error:
        raise HTTPException(status_code=400, detail=str(error))
    except cuentas.EmailDuplicado:
        raise HTTPException(
            status_code=409, detail="Ese email ya está registrado. Usa el inicio de sesión."
        )


@app.get("/health")
def health():
    """Comprobación de vida del servicio para el arranque y el despliegue."""
    return {"status": "ok"}


@app.get("/api/v1/cooperativa")
def cooperativa(conn: sqlite3.Connection = Depends(conexion)):
    """La cooperativa única del MVP, con su declaración de datos de demostración."""
    fila = conn.execute("SELECT * FROM cooperativa").fetchone()
    return dict(fila)


@app.get("/api/v1/socios")
def socios(conn: sqlite3.Connection = Depends(conexion)):
    """Los socios de la cooperativa (identificadores de demostración)."""
    filas = conn.execute("SELECT id, codigo FROM socio ORDER BY id").fetchall()
    return [dict(f) for f in filas]


@app.get("/api/v1/socios/{socio_id}/campanas")
def campanas_del_socio(socio_id: str, conn: sqlite3.Connection = Depends(conexion)):
    """Campañas de un socio con sus datos oficiales (solo lectura).

    La campaña actual va primero y después las anteriores, de más reciente a
    más antigua.
    """
    filas = conn.execute(
        "SELECT c.id, c.nombre, c.es_actual, "
        "d.kilos_entregados, d.rendimiento_graso, d.aceite_obtenido "
        "FROM campana c JOIN dato_oficial d ON d.campana_id = c.id "
        "WHERE d.socio_id = ? "
        "ORDER BY c.es_actual DESC, c.id DESC",
        (socio_id,),
    ).fetchall()
    if not filas:
        raise HTTPException(
            status_code=404,
            detail="Ese socio no existe o no tiene datos de campaña.",
        )
    return [
        {
            "id": f["id"],
            "nombre": f["nombre"],
            "actual": bool(f["es_actual"]),
            "datos_oficiales": {
                "kilos_entregados": f["kilos_entregados"],
                "rendimiento_graso": f["rendimiento_graso"],
                "aceite_obtenido": f["aceite_obtenido"],
            },
        }
        for f in filas
    ]


@app.get("/api/v1/socios/{socio_id}/parcelas")
def parcelas_del_socio(socio_id: str, conn: sqlite3.Connection = Depends(conexion)):
    """Parcelas del socio (3-4 sembradas en el seed)."""
    existe = conn.execute("SELECT 1 FROM socio WHERE id = ?", (socio_id,)).fetchone()
    if not existe:
        raise HTTPException(status_code=404, detail="Ese socio no existe.")
    filas = conn.execute(
        "SELECT id, nombre FROM parcela WHERE socio_id = ? ORDER BY id", (socio_id,)
    ).fetchall()
    return [dict(f) for f in filas]
