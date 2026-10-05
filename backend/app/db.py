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
"""


def ruta_base_datos():
    return Path(os.environ.get("OLIVAPP_DB") or RUTA_POR_DEFECTO)


def conectar():
    conn = sqlite3.connect(str(ruta_base_datos()))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def crear_esquema(conn):
    conn.executescript(ESQUEMA)
    conn.commit()
