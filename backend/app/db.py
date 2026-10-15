"""Conexión y esquema de la base de datos (SQLite de desarrollo).

La ruta se toma de la variable de entorno OLIVAPP_DB y, si no existe,
de `backend/olivapp.db`. Cambiar a PostgreSQL en el PaaS no toca el modelo.
"""

import os
import sqlite3
from pathlib import Path

RUTA_POR_DEFECTO = Path(__file__).resolve().parent.parent / "olivapp.db"

ESQUEMA = """
CREATE TABLE IF NOT EXISTS cooperativa (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    declaracion TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS socio (
    id TEXT PRIMARY KEY,
    codigo TEXT NOT NULL,
    cooperativa_id TEXT NOT NULL REFERENCES cooperativa(id)
);

CREATE TABLE IF NOT EXISTS parcela (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    socio_id TEXT NOT NULL REFERENCES socio(id)
);

CREATE TABLE IF NOT EXISTS campana (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    es_actual INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dato_oficial (
    socio_id TEXT NOT NULL REFERENCES socio(id),
    campana_id TEXT NOT NULL REFERENCES campana(id),
    kilos_entregados REAL NOT NULL,
    rendimiento_graso REAL NOT NULL,
    aceite_obtenido REAL NOT NULL,
    PRIMARY KEY (socio_id, campana_id)
);

CREATE TABLE IF NOT EXISTS cuenta (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    contrasena_hash TEXT NOT NULL,
    cooperativa_id TEXT NOT NULL REFERENCES cooperativa(id),
    socio_id TEXT NOT NULL DEFAULT 'socio-001' REFERENCES socio(id),
    creada_en TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS labor (
    id TEXT PRIMARY KEY,
    socio_id TEXT NOT NULL REFERENCES socio(id),
    parcela_id TEXT NOT NULL REFERENCES parcela(id),
    campana_id TEXT NOT NULL REFERENCES campana(id),
    categoria TEXT NOT NULL,
    fecha TEXT NOT NULL,
    campos TEXT NOT NULL,
    observaciones TEXT NOT NULL DEFAULT '',
    nota_id TEXT,
    creada_en TEXT NOT NULL DEFAULT (datetime('now'))
);
"""


def ruta_base_datos():
    return Path(os.environ.get("OLIVAPP_DB") or RUTA_POR_DEFECTO)


def conectar():
    # FastAPI ejecuta los handlers síncronos en un pool de hilos y la conexión
    # puede crearse en un hilo y usarse en otro: sin check_same_thread=False,
    # SQLite lanza ProgrammingError y la petición falla con 500.
    conn = sqlite3.connect(str(ruta_base_datos()), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    # Concurrencia: con el pool de hilos pueden coincidir escrituras; sin
    # `busy_timeout` y con el journal por defecto, SQLite responde enseguida
    # con "database is locked" y la petición cae con 500. WAL permite leer
    # mientras se escribe y el timeout da margen a la escritura en curso.
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 5000")
    return conn


def crear_esquema(conn):
    conn.executescript(ESQUEMA)
    _migrar_socio_en_cuenta(conn)
    _migrar_nota_en_labor(conn)
    conn.commit()


def _migrar_socio_en_cuenta(conn):
    """Bases creadas antes de `aislamiento-de-datos-por-socio`.

    El registro ya pide el código de socio y vincula la cuenta a su socio;
    las cuentas creadas antes quedan vinculadas al socio de demostración
    `socio-001` (decisiones de `panel-campana-actual` y
    `aislamiento-de-datos-por-socio`).
    """
    columnas = [fila[1] for fila in conn.execute("PRAGMA table_info(cuenta)")]
    if "socio_id" not in columnas:
        conn.execute(
            "ALTER TABLE cuenta ADD COLUMN socio_id TEXT NOT NULL DEFAULT 'socio-001'"
        )


def _migrar_nota_en_labor(conn):
    """Bases creadas antes de `confirmacion-de-borrador`.

    La labor guarda la nota de voz que la originó (`nota_id`) para que
    confirmar un borrador dos veces no cree dos labores: una Nota de voz
    produce como mucho una Labor (docs/domain-model.md). El alta manual no
    lleva nota y sigue creando labores sueltas.
    """
    columnas = [fila[1] for fila in conn.execute("PRAGMA table_info(labor)")]
    if "nota_id" not in columnas:
        conn.execute("ALTER TABLE labor ADD COLUMN nota_id TEXT")
    # El índice va aquí y no en el esquema base porque solo es posible cuando
    # la columna ya existe (también en las bases creadas antes de esta feature).
    conn.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS labor_nota_unica "
        "ON labor (socio_id, nota_id)"
    )
