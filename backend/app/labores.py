"""Labores de "Mi cuaderno" (feature alta-manual-labor).

Reglas (docs/domain-model.md y ADR 0005):
- Una labor es una tarea en una parcela y una fecha, dentro de una campaña.
- Categorías del MVP: tratamiento fitosanitario, riego y cosecha; cada una
  tiene sus campos propios con listas cerradas donde sea posible, y toda
  labor admite observaciones opcionales.
- La campaña se deriva de la fecha de la labor (decisión de esta feature):
  la campaña oleícola va del 1 de noviembre al 31 de octubre.
- Las campañas cerradas no admiten nuevas labores (solo consulta).
- Una Nota de voz produce como mucho una Labor (docs/domain-model.md): la
  confirmación de su borrador lleva la `nota_id` y es idempotente.
"""

import datetime
import json
import sqlite3
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


class LaborAjena(ValueError):
    """La labor no existe o pertenece a otro socio."""


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


def _preparar_labor(
    conn, socio_id, fecha_texto, parcela_id, categoria, campos, observaciones
):
    """Valida todos los campos de una labor y devuelve los normalizados.

    Sirve para crear y para editar: la campaña se deriva de la fecha y solo
    la campaña actual admite labores (las cerradas son de solo consulta).
    """
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
    return fecha, parcela, campana_id, campos, observaciones


def crear_labor(
    conn, socio_id, fecha_texto, parcela_id, categoria, campos, observaciones,
    nota_id="",
):
    """Crea una labor en la campaña que corresponde a su fecha.

    Con `nota_id` (la nota de voz que originó la labor) la creación es
    idempotente (feature `confirmacion-de-borrador`): confirmar el borrador
    dos veces devuelve la labor ya creada en lugar de duplicarla, porque una
    Nota de voz produce como mucho una Labor. El alta manual no lleva nota y
    sigue creando labores sueltas.
    """
    if nota_id:
        existente = _labor_de_la_nota(conn, socio_id, nota_id)
        if existente:
            return existente
    fecha, parcela, campana_id, campos, observaciones = _preparar_labor(
        conn, socio_id, fecha_texto, parcela_id, categoria, campos, observaciones
    )

    labor_id = str(uuid.uuid4())
    try:
        conn.execute(
            "INSERT INTO labor "
            "(id, socio_id, parcela_id, campana_id, categoria, fecha, campos, "
            "observaciones, nota_id) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (
                labor_id,
                socio_id,
                parcela_id,
                campana_id,
                categoria,
                fecha.isoformat(),
                json.dumps(campos, ensure_ascii=False),
                observaciones,
                nota_id or None,
            ),
        )
        conn.commit()
    except sqlite3.IntegrityError:
        # Dos confirmaciones de la misma nota a la vez: gana la primera y la
        # segunda recibe esa misma labor.
        conn.rollback()
        if not nota_id:
            raise
        return _labor_de_la_nota(conn, socio_id, nota_id)
    return {
        "id": labor_id,
        "fecha": fecha.isoformat(),
        "categoria": categoria,
        "campana_id": campana_id,
        "campos": campos,
        "observaciones": observaciones,
        "parcela": {"id": parcela["id"], "nombre": parcela["nombre"]},
    }


def _labor_de_la_nota(conn, socio_id, nota_id):
    """La labor que ya creó esta nota de voz, si la hay (nunca dos)."""
    fila = conn.execute(
        "SELECT id FROM labor WHERE socio_id = ? AND nota_id = ?",
        (socio_id, nota_id),
    ).fetchone()
    if not fila:
        return None
    labor = conn.execute(
        "SELECT l.id, l.fecha, l.categoria, l.campos, l.observaciones, "
        "p.id AS parcela_id, p.nombre AS parcela_nombre "
        "FROM labor l JOIN parcela p ON p.id = l.parcela_id "
        "WHERE l.id = ?",
        (fila["id"],),
    ).fetchone()
    return _labor_de_fila(labor)


def actualizar_labor(
    conn, socio_id, labor_id, fecha_texto, parcela_id, categoria, campos, observaciones
):
    """Edita cualquier campo de una labor propia (escenario 'Corrección manual').

    La labor puede cambiar de fecha y de parcela; si la fecha cambia, cambia
    de campaña con ella, pero solo la campaña actual admite labores.
    """
    existe = conn.execute(
        "SELECT id FROM labor WHERE id = ? AND socio_id = ?", (labor_id, socio_id)
    ).fetchone()
    if not existe:
        raise LaborAjena(labor_id)
    fecha, parcela, campana_id, campos, observaciones = _preparar_labor(
        conn, socio_id, fecha_texto, parcela_id, categoria, campos, observaciones
    )
    conn.execute(
        "UPDATE labor SET fecha = ?, parcela_id = ?, campana_id = ?, "
        "categoria = ?, campos = ?, observaciones = ? WHERE id = ?",
        (
            fecha.isoformat(),
            parcela["id"],
            campana_id,
            categoria,
            json.dumps(campos, ensure_ascii=False),
            observaciones,
            labor_id,
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


def eliminar_labor(conn, socio_id, labor_id):
    """Elimina una labor propia: borrado directo, sin papelera (MVP)."""
    existe = conn.execute(
        "SELECT id FROM labor WHERE id = ? AND socio_id = ?", (labor_id, socio_id)
    ).fetchone()
    if not existe:
        raise LaborAjena(labor_id)
    conn.execute("DELETE FROM labor WHERE id = ?", (labor_id,))
    conn.commit()
    return {"id": labor_id, "eliminada": True}


def _labor_de_fila(f):
    return {
        "id": f["id"],
        "fecha": f["fecha"],
        "categoria": f["categoria"],
        "campos": json.loads(f["campos"]),
        "observaciones": f["observaciones"],
        "parcela": {"id": f["parcela_id"], "nombre": f["parcela_nombre"]},
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
    return [_labor_de_fila(f) for f in filas]


def labores_del_socio_por_campana(conn, socio_id):
    """Historial del socio: sus labores agrupadas por campaña ('Mi cuaderno').

    De la campaña más reciente a la más antigua y, dentro de cada campaña,
    de la labor más reciente a la más antigua. Solo labores vigentes (sin
    papelera, según el ciclo de vida de Labor).
    """
    filas = conn.execute(
        "SELECT l.id, l.fecha, l.categoria, l.campos, l.observaciones, "
        "p.id AS parcela_id, p.nombre AS parcela_nombre, "
        "c.id AS campana_id, c.nombre AS campana_nombre, c.es_actual "
        "FROM labor l JOIN parcela p ON p.id = l.parcela_id "
        "JOIN campana c ON c.id = l.campana_id "
        "WHERE l.socio_id = ? "
        "ORDER BY c.id DESC, l.fecha DESC, l.rowid DESC",
        (socio_id,),
    ).fetchall()
    campanas = []
    por_id = {}
    for f in filas:
        grupo = por_id.get(f["campana_id"])
        if grupo is None:
            grupo = {
                "id": f["campana_id"],
                "nombre": f["campana_nombre"],
                "es_actual": bool(f["es_actual"]),
                "labores": [],
            }
            por_id[f["campana_id"]] = grupo
            campanas.append(grupo)
        grupo["labores"].append(_labor_de_fila(f))
    return campanas
