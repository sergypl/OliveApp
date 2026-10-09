"""API de datos de OlivApp.

De momento: healthcheck, validación de la configuración por variables de
entorno (la clave de la API de IA y el secreto JWT nunca viajan en el APK) y
los datos oficiales de la cooperativa sembrados, **solo lectura** (ADR 0006).
"""

import os
import sqlite3
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel

from app import cuentas
from app import db as basedatos
from app import seed
from app import sesiones

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


def sesion_actual(authorization: str = Header(default="")):
    """Devuelve el contenido del token de sesión o rechaza con 401."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Inicia sesión para ver tus datos.")
    try:
        return sesiones.leer_token(authorization[len("Bearer "):])
    except sesiones.SesionInvalida:
        raise HTTPException(
            status_code=401,
            detail="Sesión caducada o no válida. Vuelve a iniciar sesión.",
        )


app = FastAPI(title="OlivApp API", lifespan=lifespan)


class RegistroEntrada(BaseModel):
    email: str
    contrasena: str


class LoginEntrada(BaseModel):
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


@app.post("/api/v1/login")
def login(entrada: LoginEntrada, conn: sqlite3.Connection = Depends(conexion)):
    """El socio inicia sesión con email y contraseña y recibe su token.

    El token (JWT con caducidad) lo guarda la app en almacenamiento seguro;
    los errores de acceso no revelan si lo que no cuadra es el email o la
    contraseña.
    """
    email = cuentas.normalizar_email(entrada.email)
    fila = conn.execute(
        "SELECT id, email, contrasena_hash FROM cuenta WHERE email = ?", (email,)
    ).fetchone()
    if not fila or not cuentas.verificar_contrasena(entrada.contrasena, fila["contrasena_hash"]):
        raise HTTPException(status_code=401, detail="Email o contraseña incorrectos.")
    return {"token": sesiones.crear_token(dict(fila)), "email": fila["email"]}


@app.get("/api/v1/mi-cuenta")
def mi_cuenta(sesion=Depends(sesion_actual)):
    """La cuenta del socio autenticado (prueba de llamadas con token)."""
    return {"id": sesion["sub"], "email": sesion["email"]}


@app.get("/api/v1/mi-panel")
def mi_panel(conn: sqlite3.Connection = Depends(conexion), sesion=Depends(sesion_actual)):
    """Panel del socio autenticado: la campaña actual con sus datos oficiales.

    La campaña actual es la marcada como `es_actual` en los datos de la
    cooperativa (decisión de esta feature): no se deriva de la fecha del
    sistema, para que la demo siempre muestre la campaña sembrada.
    """
    cuenta = conn.execute(
        "SELECT socio_id FROM cuenta WHERE id = ?", (sesion["sub"],)
    ).fetchone()
    if not cuenta:
        raise HTTPException(
            status_code=401, detail="Sesión caducada o no válida. Vuelve a iniciar sesión."
        )
    fila = conn.execute(
        "SELECT c.id, c.nombre, "
        "d.kilos_entregados, d.rendimiento_graso, d.aceite_obtenido "
        "FROM campana c JOIN dato_oficial d ON d.campana_id = c.id "
        "WHERE d.socio_id = ? AND c.es_actual = 1",
        (cuenta["socio_id"],),
    ).fetchone()
    if not fila:
        raise HTTPException(
            status_code=404, detail="No hay datos oficiales de la campaña actual."
        )
    return {
        "campana": {"id": fila["id"], "nombre": fila["nombre"]},
        "datos_oficiales": {
            "kilos_entregados": fila["kilos_entregados"],
            "rendimiento_graso": fila["rendimiento_graso"],
            "aceite_obtenido": fila["aceite_obtenido"],
        },
    }


def _cuenta_del_socio(conn, sesion):
    """La cuenta del token con su socio vinculado (o 401 si no existe)."""
    cuenta = conn.execute(
        "SELECT socio_id FROM cuenta WHERE id = ?", (sesion["sub"],)
    ).fetchone()
    if not cuenta:
        raise HTTPException(
            status_code=401, detail="Sesión caducada o no válida. Vuelve a iniciar sesión."
        )
    return cuenta


@app.get("/api/v1/mi-campanas")
def mis_campanas(conn: sqlite3.Connection = Depends(conexion), sesion=Depends(sesion_actual)):
    """Campañas del socio autenticado, con su estado (`actual` o `cerrada`).

    Estados de Campaña (docs/domain-model.md): la actual admite labores;
    las cerradas son anteriores y de solo consulta.
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    filas = conn.execute(
        "SELECT c.id, c.nombre, c.es_actual FROM campana c "
        "JOIN dato_oficial d ON d.campana_id = c.id "
        "WHERE d.socio_id = ? ORDER BY c.es_actual DESC, c.id DESC",
        (cuenta["socio_id"],),
    ).fetchall()
    return [
        {
            "id": f["id"],
            "nombre": f["nombre"],
            "estado": "actual" if f["es_actual"] else "cerrada",
        }
        for f in filas
    ]


@app.get("/api/v1/mi-campanas/{campana_id}")
def mi_campana(
    campana_id: str,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Detalle de una campaña del socio: datos oficiales y labores por separado.

    Solo lectura: los datos oficiales no cambian desde la app y las campañas
    cerradas no admiten nuevas labores. `labores` queda ya separado como
    sección propia (vacía hasta que llegue el cuaderno de campo).
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    fila = conn.execute(
        "SELECT c.id, c.nombre, c.es_actual, "
        "d.kilos_entregados, d.rendimiento_graso, d.aceite_obtenido "
        "FROM campana c JOIN dato_oficial d ON d.campana_id = c.id "
        "WHERE d.socio_id = ? AND c.id = ?",
        (cuenta["socio_id"], campana_id),
    ).fetchone()
    if not fila:
        raise HTTPException(status_code=404, detail="No existe esa campaña.")
    return {
        "campana": {
            "id": fila["id"],
            "nombre": fila["nombre"],
            "estado": "actual" if fila["es_actual"] else "cerrada",
        },
        "datos_oficiales": {
            "kilos_entregados": fila["kilos_entregados"],
            "rendimiento_graso": fila["rendimiento_graso"],
            "aceite_obtenido": fila["aceite_obtenido"],
        },
        "labores": [],
    }


@app.get("/health")
def health():
    """Comprobación de vida del servicio para el arranque y el despliegue."""
    return {"status": "ok"}


@app.get("/api/v1/cooperativa")
def cooperativa(
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """La cooperativa única del MVP, con su declaración de datos de demostración."""
    fila = conn.execute("SELECT * FROM cooperativa").fetchone()
    return dict(fila)


@app.get("/api/v1/socios")
def socios(
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Los socios de la cooperativa (identificadores de demostración)."""
    filas = conn.execute("SELECT id, codigo FROM socio ORDER BY id").fetchall()
    return [dict(f) for f in filas]


@app.get("/api/v1/socios/{socio_id}/campanas")
def campanas_del_socio(
    socio_id: str,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
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
def parcelas_del_socio(
    socio_id: str,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Parcelas del socio (3-4 sembradas en el seed)."""
    existe = conn.execute("SELECT 1 FROM socio WHERE id = ?", (socio_id,)).fetchone()
    if not existe:
        raise HTTPException(status_code=404, detail="Ese socio no existe.")
    filas = conn.execute(
        "SELECT id, nombre FROM parcela WHERE socio_id = ? ORDER BY id", (socio_id,)
    ).fetchall()
    return [dict(f) for f in filas]
