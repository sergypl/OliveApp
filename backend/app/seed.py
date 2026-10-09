"""Carga del juego de datos sembrados (ADR 0006 y ADR 0007).

Fichero versionado `backend/data/seed.json`; se carga solo si la base está
vacía, de forma que reejecutar el servidor o el script no duplica datos.
Regla de coherencia (ADR 0007): aceite obtenido = round(kilos × rendimiento
graso × (1 − merma industrial)), con merma del 9,7 %.
"""

import json
from pathlib import Path

FICHERO_SEED = Path(__file__).resolve().parent.parent / "data" / "seed.json"


def base_vacia(conn):
    total = conn.execute("SELECT COUNT(*) FROM dato_oficial").fetchone()[0]
    return total == 0


def cargar_si_vacia(conn):
    """Carga el seed si la base no tiene datos oficiales. Devuelve lo insertado."""
    if not base_vacia(conn):
        return 0

    datos = json.loads(FICHERO_SEED.read_text(encoding="utf-8"))

    cooperativa = datos["cooperativa"]
    conn.execute(
        "INSERT INTO cooperativa (id, nombre, declaracion) VALUES (?, ?, ?)",
        (cooperativa["id"], cooperativa["nombre"], datos["declaracion"]),
    )

    for campana in datos["campanas"]:
        conn.execute(
            "INSERT INTO campana (id, nombre, es_actual) VALUES (?, ?, ?)",
            (campana["id"], campana["nombre"], 1 if campana["actual"] else 0),
        )

    for socio in datos["socios"]:
        conn.execute(
            "INSERT INTO socio (id, codigo, cooperativa_id) VALUES (?, ?, ?)",
            (socio["id"], socio["codigo"], cooperativa["id"]),
        )
        for posicion, nombre in enumerate(socio["parcelas"], start=1):
            conn.execute(
                "INSERT INTO parcela (id, nombre, socio_id) VALUES (?, ?, ?)",
                ("{}-parcela-{}".format(socio["id"], posicion), nombre, socio["id"]),
            )

    for dato in datos["datos_oficiales"]:
        conn.execute(
            "INSERT INTO dato_oficial "
            "(socio_id, campana_id, kilos_entregados, rendimiento_graso, aceite_obtenido) "
            "VALUES (?, ?, ?, ?, ?)",
            (
                dato["socio_id"],
                dato["campana_id"],
                dato["kilos_entregados"],
                dato["rendimiento_graso"],
                dato["aceite_obtenido"],
            ),
        )

    conn.commit()
    return len(datos["datos_oficiales"])
