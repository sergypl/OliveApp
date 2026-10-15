"""API de datos de OlivApp.

De momento: healthcheck, validación de la configuración por variables de
entorno (la clave de la API de IA y el secreto JWT nunca viajan en el APK) y
los datos oficiales de la cooperativa sembrados, **solo lectura** (ADR 0006).
"""

import json
import os
import sqlite3
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from pydantic import BaseModel

from app import borrador
from app import cuentas
from app import db as basedatos
from app import labores
from app import parcelas
from app import perfil
from app import precios
from app import seed
from app import sesiones
from app import transcripcion

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
app.include_router(perfil.crear_router(conexion, sesion_actual))
app.include_router(precios.crear_router(conexion, sesion_actual))


class RegistroEntrada(BaseModel):
    email: str
    contrasena: str
    codigo_socio: str


class LoginEntrada(BaseModel):
    email: str
    contrasena: str


class ParcelaEntrada(BaseModel):
    nombre: str


class LaborEntrada(BaseModel):
    fecha: str
    parcela_id: str
    categoria: str
    campos: dict = {}
    observaciones: str = ""
    nota_id: str = ""


@app.post("/api/v1/registro", status_code=201)
def registro(entrada: RegistroEntrada, conn: sqlite3.Connection = Depends(conexion)):
    """El socio crea su cuenta con email, contraseña y código de socio.

    La cuenta queda vinculada al socio de la cooperativa cuyo código se
    indique (feature `aislamiento-de-datos-por-socio`): a partir de ahí solo
    ve sus propios datos. La contraseña se guarda hasheada y nunca se
    devuelve. Sin verificación de email ni reset de contraseña en el MVP.
    """
    cooperativa = conn.execute("SELECT id FROM cooperativa").fetchone()
    try:
        return cuentas.crear_cuenta(
            conn,
            cooperativa["id"],
            entrada.email,
            entrada.contrasena,
            entrada.codigo_socio,
        )
    except cuentas.DatosInvalidos as error:
        raise HTTPException(status_code=400, detail=str(error))
    except cuentas.SocioDesconocido:
        raise HTTPException(
            status_code=400,
            detail="No encontramos ese código de socio en tu cooperativa. Revísalo con la cooperativa.",
        )
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
    las cerradas son anteriores y de solo consulta. Cada campaña lleva su
    `aceite_obtenido` (kg, dato oficial) para la comparativa del panel sin
    pedir el detalle de cada una (campo añadido, el resto no cambia).
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    filas = conn.execute(
        "SELECT c.id, c.nombre, c.es_actual, d.aceite_obtenido FROM campana c "
        "JOIN dato_oficial d ON d.campana_id = c.id "
        "WHERE d.socio_id = ? ORDER BY c.es_actual DESC, c.id DESC",
        (cuenta["socio_id"],),
    ).fetchall()
    return [
        {
            "id": f["id"],
            "nombre": f["nombre"],
            "estado": "actual" if f["es_actual"] else "cerrada",
            "aceite_obtenido": f["aceite_obtenido"],
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
        "labores": labores.labores_de_la_campana(conn, cuenta["socio_id"], fila["id"]),
    }


@app.get("/api/v1/mis-labores")
def mis_labores(conn: sqlite3.Connection = Depends(conexion), sesion=Depends(sesion_actual)):
    """Historial de labores del socio agrupado por campaña ('Mi cuaderno')."""
    cuenta = _cuenta_del_socio(conn, sesion)
    return {"campanas": labores.labores_del_socio_por_campana(conn, cuenta["socio_id"])}


@app.post("/api/v1/mis-labores", status_code=201)
def crear_labor(
    entrada: LaborEntrada,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """El socio registra una labor con el formulario manual o confirmando.

    La labor se guarda en la campaña que corresponde a su fecha (campaña
    oleícola del 1 de noviembre al 31 de octubre) y solo en la campaña
    actual: las cerradas son de solo consulta. Si el socio confirma el
    borrador IA de una nota de voz, el cuerpo lleva su `nota_id` y la
    creación es idempotente (feature `confirmacion-de-borrador`): confirmar
    dos veces devuelve la labor ya creada y no crea una segunda (una Nota de
    voz produce como mucho una Labor).
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    try:
        return labores.crear_labor(
            conn,
            cuenta["socio_id"],
            entrada.fecha,
            entrada.parcela_id,
            entrada.categoria,
            entrada.campos,
            entrada.observaciones,
            entrada.nota_id,
        )
    except labores.ParcelaAjena:
        raise HTTPException(status_code=404, detail="No existe esa parcela.")
    except labores.DatosInvalidos as error:
        raise HTTPException(status_code=400, detail=str(error))


@app.put("/api/v1/mis-labores/{labor_id}")
def editar_labor(
    labor_id: str,
    entrada: LaborEntrada,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Edita cualquier campo de una labor del socio ('Corrección manual').

    La campaña de la labor se deriva de su fecha: solo la campaña actual
    admite labores (las cerradas son de solo consulta).
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    try:
        return labores.actualizar_labor(
            conn,
            cuenta["socio_id"],
            labor_id,
            entrada.fecha,
            entrada.parcela_id,
            entrada.categoria,
            entrada.campos,
            entrada.observaciones,
        )
    except labores.LaborAjena:
        raise HTTPException(status_code=404, detail="No existe esa labor.")
    except labores.ParcelaAjena:
        raise HTTPException(status_code=404, detail="No existe esa parcela.")
    except labores.DatosInvalidos as error:
        raise HTTPException(status_code=400, detail=str(error))


@app.delete("/api/v1/mis-labores/{labor_id}")
def eliminar_labor(
    labor_id: str,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Elimina una labor del socio: borrado directo, sin papelera (MVP)."""
    cuenta = _cuenta_del_socio(conn, sesion)
    try:
        return labores.eliminar_labor(conn, cuenta["socio_id"], labor_id)
    except labores.LaborAjena:
        raise HTTPException(status_code=404, detail="No existe esa labor.")


@app.post("/api/v1/mis-notas-voz/transcripcion")
async def transcribir_nota(
    audio: UploadFile = File(...),
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Transcribe una nota de voz y propone su borrador IA.

    El audio se transcribe con el alias `transcribe` y el borrador IA se
    extrae con el alias `chat` (ADR 0004), apoyándose en las parcelas del
    socio. El audio se borra del servidor tras la transcripción: no se
    conserva nada. Si la IA no reconoce la categoría de la labor, el
    borrador se rechaza (devuelve null) y la app lo dice claro.
    """
    contenido = await audio.read()
    nombre = audio.filename
    await audio.close()
    try:
        texto = transcripcion.transcribir(
            os.environ["DEVEXPERT_API_KEY"], nombre, contenido
        )
    except transcripcion.AudioInvalido as error:
        raise HTTPException(status_code=400, detail=str(error))
    except transcripcion.TranscripcionFallida:
        raise HTTPException(
            status_code=502,
            detail="No se pudo transcribir la nota. Inténtalo de nuevo.",
        )
    cuenta = _cuenta_del_socio(conn, sesion)
    nombres = [
        fila["nombre"] for fila in parcelas.parcelas_del_socio(conn, cuenta["socio_id"])
    ]
    try:
        propuesta = borrador.extraer(
            os.environ["DEVEXPERT_API_KEY"], texto, nombres
        )
    except borrador.CategoriaNoReconocida:
        propuesta = None
    except borrador.ExtraccionFallida:
        raise HTTPException(
            status_code=502,
            detail="No se pudo preparar el borrador. Inténtalo de nuevo.",
        )
    return {"texto": texto, "borrador": propuesta}


@app.post("/api/v1/borradores/completar")
async def completar_borrador(
    audio: UploadFile = File(...),
    borrador_recibido: str = Form(..., alias="borrador"),
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Completa el borrador IA con la respuesta por voz del socio.

    Relleno conversacional (ADR 0005): cuando faltan campos obligatorios, el
    asistente pregunta por ellos y el socio contesta por voz. La respuesta se
    transcribe con el alias `transcribe` y de ella se extraen los valores que
    aporta (alias `chat`), una pregunta por respuesta. El borrador vuelve con
    la `pregunta` por el campo que siga faltando y, cuando ya no falta nada,
    con la pregunta vacía: listo para confirmar. El audio de la respuesta no se
    conserva en el servidor, igual que el de la nota.
    """
    try:
        propuesta = json.loads(borrador_recibido)
    except ValueError:
        propuesta = None
    if (
        not isinstance(propuesta, dict)
        or propuesta.get("rechazado")
        or propuesta.get("categoria") not in labores.CATEGORIAS
    ):
        raise HTTPException(
            status_code=400,
            detail="Ese borrador no se puede completar. Vuelve a dictar la nota.",
        )
    contenido = await audio.read()
    nombre = audio.filename
    await audio.close()
    try:
        texto = transcripcion.transcribir(
            os.environ["DEVEXPERT_API_KEY"], nombre, contenido
        )
    except transcripcion.AudioInvalido:
        raise HTTPException(
            status_code=400, detail="Este audio no se puede usar. Vuelve a responder."
        )
    except transcripcion.TranscripcionFallida:
        raise HTTPException(
            status_code=502,
            detail="No se pudo escuchar tu respuesta. Inténtalo de nuevo.",
        )
    try:
        completado = borrador.completar(
            os.environ["DEVEXPERT_API_KEY"], propuesta, texto
        )
    except borrador.ExtraccionFallida:
        raise HTTPException(
            status_code=502,
            detail="No se pudo completar el borrador. Inténtalo de nuevo.",
        )
    return {"texto": texto, "borrador": completado}


@app.get("/api/v1/mis-parcelas")
def mis_parcelas(conn: sqlite3.Connection = Depends(conexion), sesion=Depends(sesion_actual)):
    """Parcelas del socio autenticado (para poder asignarles labores)."""
    cuenta = _cuenta_del_socio(conn, sesion)
    return parcelas.parcelas_del_socio(conn, cuenta["socio_id"])


@app.post("/api/v1/mis-parcelas", status_code=201)
def crear_parcela(
    entrada: ParcelaEntrada,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """El socio crea una parcela con nombre propio.

    La parcela pertenece al socio de la cuenta (no se comparte con otros
    socios) y en el MVP no se elimina ni archiva.
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    try:
        return parcelas.crear_parcela(conn, cuenta["socio_id"], entrada.nombre)
    except parcelas.DatosInvalidos as error:
        raise HTTPException(status_code=400, detail=str(error))


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
    """El socio del propio socio autenticado (aislamiento por socio).

    Antes listaba todos los socios de la cooperativa; desde la feature
    `aislamiento-de-datos-por-socio` cada cuenta solo accede al suyo.
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    filas = conn.execute(
        "SELECT id, codigo FROM socio WHERE id = ?", (cuenta["socio_id"],)
    ).fetchall()
    return [dict(f) for f in filas]


def _exigir_socio_propio(cuenta, socio_id):
    """Rechaza con 404 las consultas a datos de otro socio (aislamiento)."""
    if socio_id != cuenta["socio_id"]:
        raise HTTPException(status_code=404, detail="Ese socio no existe.")


@app.get("/api/v1/socios/{socio_id}/campanas")
def campanas_del_socio(
    socio_id: str,
    conn: sqlite3.Connection = Depends(conexion),
    sesion=Depends(sesion_actual),
):
    """Campañas de un socio con sus datos oficiales (solo lectura).

    Solo las del propio socio autenticado: las de otro socio son 404 (no se
    revela si existe). La campaña actual va primero y después las
    anteriores, de más reciente a más antigua.
    """
    cuenta = _cuenta_del_socio(conn, sesion)
    _exigir_socio_propio(cuenta, socio_id)
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
    """Parcelas del propio socio autenticado (las de otro socio son 404)."""
    cuenta = _cuenta_del_socio(conn, sesion)
    _exigir_socio_propio(cuenta, socio_id)
    filas = conn.execute(
        "SELECT id, nombre FROM parcela WHERE socio_id = ? ORDER BY id", (socio_id,)
    ).fetchall()
    return [dict(f) for f in filas]
