"""Parcelas del socio (feature alta-de-parcela).

Reglas del MVP (docs/domain-model.md):
- Una parcela es una unidad de terreno con nombre y pertenece a un socio.
- Las parcelas no se eliminan ni archivan en el MVP.
"""

import uuid

LONGITUD_MAXIMA = 60


class DatosInvalidos(ValueError):
    """El nombre de la parcela no es válido."""


def validar_nombre(nombre):
    """Valida y normaliza el nombre de la parcela (sin espacios sobrantes)."""
    nombre = (nombre or "").strip()
    if not nombre:
        raise DatosInvalidos("Escribe un nombre para la parcela.")
    if len(nombre) > LONGITUD_MAXIMA:
        raise DatosInvalidos(
            "El nombre de la parcela es demasiado largo (máximo {} caracteres).".format(
                LONGITUD_MAXIMA
            )
        )
    return nombre


def crear_parcela(conn, socio_id, nombre):
    """Crea una parcela del socio. Devuelve id y nombre."""
    nombre = validar_nombre(nombre)
    parcela_id = str(uuid.uuid4())
    conn.execute(
        "INSERT INTO parcela (id, nombre, socio_id) VALUES (?, ?, ?)",
        (parcela_id, nombre, socio_id),
    )
    conn.commit()
    return {"id": parcela_id, "nombre": nombre}


def parcelas_del_socio(conn, socio_id):
    """Las parcelas del socio, de la más reciente a la más antigua."""
    filas = conn.execute(
        "SELECT id, nombre FROM parcela WHERE socio_id = ? ORDER BY rowid DESC",
        (socio_id,),
    ).fetchall()
    return [dict(f) for f in filas]
