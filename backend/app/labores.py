"""Labores de "Mi cuaderno" (feature alta-manual-labor).

Reglas (docs/domain-model.md y ADR 0005):
- Una labor es una tarea en una parcela y una fecha, dentro de una campaña.
- Categorías del MVP: tratamiento fitosanitario, riego y cosecha; cada una
  tiene sus campos propios con listas cerradas donde sea posible, y toda
  labor admite observaciones opcionales.
- La campaña se deriva de la fecha de la labor (decisión de esta feature):
  la campaña oleícola va del 1 de noviembre al 31 de octubre.
- Las campañas cerradas no admiten nuevas labores (solo consulta).
"""

import datetime
import json
import uuid

CATEGORIAS = ("tratamiento", "riego", "cosecha")

# Listas cerradas (ADR 0005).
PLAGAS = (
    "mosca del olivo",
    "repilo",
    "prays",
    "tuberculosis",
    "cochinilla",
    "malas hierbas",
    "otra",
)
UNIDADES_TRATAMIENTO = ("kg", "L", "g", "ml")
UNIDADES_RIEGO = ("horas", "m³", "L")
MODALIDADES_COSECHA = ("a vuelo", "a suelo")

LONGITUD_MAXIMA_OBSERVACIONES = 500


class DatosInvalidos(ValueError):
    """Algún dato de la labor no cumple las reglas del dominio."""


class ParcelaAjena(ValueError):
    """La parcela no existe o pertenece a otro socio."""


def campana_de_la_fecha(fecha):
    """Campaña oleícola de una fecha: del 1 de noviembre al 31 de octubre."""
    inicio = fecha.year if fecha.month >= 11 else fecha.year - 1
    return "{}-{}".format(inicio, inicio + 1)


def _validar_fecha(texto):
    try:
        return datetime.datetime.strptime(texto, "%Y-%m-%d").date()
    except (ValueError, TypeError):
        raise DatosInvalidos("La fecha no es válida. Escríbela como año, mes y día.")


def _numero(valor, mensaje):
    try:
        numero = float(valor)
    except (TypeError, ValueError):
        raise DatosInvalidos(mensaje)
    if numero <= 0:
        raise DatosInvalidos(mensaje)
    return numero


def _cantidad_y_unidad(campos, unidades):
    cantidad = _numero(campos.get("cantidad"), "Escribe la cantidad de la labor.")
    unidad = campos.get("unidad")
    if unidad not in unidades:
        raise DatosInvalidos(
            "Elige una unidad válida: {}.".format(", ".join(unidades))
        )
    return cantidad, unidad


def _validar_campos(categoria, campos):
    """Valida los campos de la categoría y devuelve solo los que le tocan."""
    campos = dict(campos or {})
    if categoria == "tratamiento":
        producto = (campos.get("producto") or "").strip()
        if not producto:
            raise DatosInvalidos("Escribe el producto del tratamiento.")
        plaga = campos.get("plaga_a_controlar")
        if plaga not in PLAGAS:
            raise DatosInvalidos("Elige la plaga a controlar.")
        cantidad, unidad = _cantidad_y_unidad(campos, UNIDADES_TRATAMIENTO)
        return {
            "producto": producto,
            "plaga_a_controlar": plaga,
            "cantidad": cantidad,
            "unidad": unidad,
        }
    if categoria == "riego":
        cantidad, unidad = _cantidad_y_unidad(campos, UNIDADES_RIEGO)
        return {"cantidad": cantidad, "unidad": unidad}
    if categoria == "cosecha":
        modalidad = campos.get("modalidad")
        if modalidad not in MODALIDADES_COSECHA:
            raise DatosInvalidos("Elige la modalidad de la cosecha: a vuelo o a suelo.")
        kilos = _numero(campos.get("kilos"), "Escribe los kilos de la cosecha.")
        return {"modalidad": modalidad, "kilos": kilos}
    raise DatosInvalidos(
        "Elige una categoría de labor: tratamiento fitosanitario, riego o cosecha."
    )


def crear_labor(
    conn, socio_id, fecha_texto, parcela_id, categoria, campos, observaciones
):
    """Crea una labor en la campaña que corresponde a su fecha."""
    fecha = _validar_fecha(fecha_texto)
    if categoria not in CATEGORIAS:
        raise DatosInvalidos(
            "Elige una categoría de labor: tratamiento fitosanitario, riego o cosecha."
        )
    campos = _validar_campos(categoria, campos)
    observaciones = (observaciones or "").strip()
    if len(observaciones) > LONGITUD_MAXIMA_OBSERVACIONES:
        raise DatosInvalidos(
            "Las observaciones son demasiado largas (máximo {} caracteres).".format(
                LONGITUD_MAXIMA_OBSERVACIONES
            )
        )

    parcela = conn.execute(
        "SELECT id, nombre FROM parcela WHERE id = ? AND socio_id = ?",
        (parcela_id, socio_id),
    ).fetchone()
    if not parcela:
        raise ParcelaAjena(parcela_id)

    campana_id = campana_de_la_fecha(fecha)
    campana = conn.execute(
        "SELECT es_actual FROM campana WHERE id = ?", (campana_id,)
    ).fetchone()
    if not campana:
        raise DatosInvalidos("Esa fecha no corresponde a ninguna campaña conocida.")
    if not campana["es_actual"]:
        raise DatosInvalidos("Esa fecha corresponde a una campaña cerrada: solo consulta.")

    labor_id = str(uuid.uuid4())
    conn.execute(
        "INSERT INTO labor "
        "(id, socio_id, parcela_id, campana_id, categoria, fecha, campos, observaciones) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        (
            labor_id,
            socio_id,
            parcela_id,
            campana_id,
            categoria,
            fecha.isoformat(),
            json.dumps(campos, ensure_ascii=False),
            observaciones,
        ),
    )
    conn.commit()
    return {
        "id": labor_id,
        "fecha": fecha.isoformat(),
        "categoria": categoria,
        "campana_id": campana_id,
        "campos": campos,
        "observaciones": observaciones,
        "parcela": {"id": parcela["id"], "nombre": parcela["nombre"]},
    }


def labores_de_la_campana(conn, socio_id, campana_id):
    """Las labores del socio en una campaña, de la más reciente a la más antigua."""
    filas = conn.execute(
        "SELECT l.id, l.fecha, l.categoria, l.campos, l.observaciones, "
        "p.id AS parcela_id, p.nombre AS parcela_nombre "
        "FROM labor l JOIN parcela p ON p.id = l.parcela_id "
        "WHERE l.socio_id = ? AND l.campana_id = ? "
        "ORDER BY l.fecha DESC, l.rowid DESC",
        (socio_id, campana_id),
    ).fetchall()
    return [
        {
            "id": f["id"],
            "fecha": f["fecha"],
            "categoria": f["categoria"],
            "campos": json.loads(f["campos"]),
            "observaciones": f["observaciones"],
            "parcela": {"id": f["parcela_id"], "nombre": f["parcela_nombre"]},
        }
        for f in filas
    ]
