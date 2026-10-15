"""Precios de referencia del aceite de oliva (feature simulador-avanzado-precio-aceite).

El simulador avanzado necesita un precio de referencia en origen a granel en
€/kg de aceite. La fuente es la API pública del Agri-food Data Portal de la
Comisión Europea (sin clave) y este módulo hace de proxy: parsea, mapea las
categorías y mercados del MVP, deduplica y cachea en SQLite. La app nunca
llama directamente a la API europea.

Reglas:
- El precio llega en "€/100kg" (por ejemplo "€445.00"): se quita el símbolo y
  se DIVIDE ENTRE 100 para obtener €/kg.
- Solo se usan las tres categorías del MVP (AOVE, virgen y lampante) y los
  mercados españoles del mapa; los refinados y los de orujo se ignoran.
- Las semanas de cambio de campaña (septiembre y octubre) salen duplicadas
  con dos `marketingYear`: se deduplica por (beginDate, product, market),
  dejando una sola fila por semana, categoría y mercado.
- Si la API falla o tarda se sirve la caché de SQLite indicando su antigüedad;
  sin caché se avisa de que no hay precio disponible (nunca un precio inventado).

Los precios son de referencia: **no es lo que paga la cooperativa** (CONTEXT.md).
"""

from datetime import date, datetime, timedelta

import httpx
from fastapi import APIRouter, Depends, HTTPException

BASE = "https://api.tech.ec.europa.eu/agrifood/api/oliveOil/"
TIEMPO_DE_ESPERA = 10
FUENTE = "Comisión Europea · Agri-food Data Portal"

# Categorías del MVP -> producto de la API (se ignoran refinados y orujo).
CATEGORIAS = {
    "aove": "Extra virgin olive oil (up to 0.8%)",
    "virgen": "Virgin olive oil (up to 2%)",
    "lampante": "Lampante olive oil (2%)",
}
PRODUCTO_DE_CATEGORIA = {producto: categoria for categoria, producto in CATEGORIAS.items()}

# Mercados del MVP -> mercado de la API ("Málaga(ES617)" va sin espacio, tal cual).
MERCADOS = {
    "media-nacional": "Average national price",
    "jaen": "Jaén (ES616)",
    "cordoba": "Córdoba (ES613)",
    "granada": "Granada (ES614)",
    "sevilla": "Sevilla (ES618)",
    "malaga": "Málaga(ES617)",
    "badajoz": "Badajoz (ES431)",
    "toledo": "Toledo (ES425)",
    "tarragona": "Tarragona (ES514)",
}
NOMBRE_DE_MERCADO = {nombre: mercado for mercado, nombre in MERCADOS.items()}

# Tabla NUEVA de caché: no toca ninguna tabla existente del esquema.
ESQUEMA_CACHE = """
CREATE TABLE IF NOT EXISTS precio_aceite (
    categoria TEXT NOT NULL,
    mercado TEXT NOT NULL,
    semana TEXT NOT NULL,
    campana TEXT NOT NULL,
    precio REAL NOT NULL,
    semana_fin TEXT NOT NULL,
    numero_semana INTEGER NOT NULL,
    consultado_en TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (categoria, mercado, semana, campana)
);
"""


class ParametroInvalido(ValueError):
    """Categoría, mercado o campaña que el simulador avanzado no entiende."""


class PrecioNoDisponible(RuntimeError):
    """No se pudo obtener el precio ni de la API ni de la caché."""


def validar(mercado, categoria=None):
    if mercado not in MERCADOS:
        raise ParametroInvalido("Mercado no disponible: " + str(mercado))
    if categoria not in (None, "") and categoria not in CATEGORIAS:
        raise ParametroInvalido("Categoría no disponible: " + str(categoria))


def precio_en_euros_por_kilo(texto):
    """'€445.00' (€/100kg) -> 4.45 €/kg. Vacío o ilegible devuelve None."""
    if texto is None:
        return None
    limpio = str(texto).replace("€", "").replace("EUR", "").strip()
    if not limpio:
        return None
    try:
        return round(float(limpio) / 100.0, 4)
    except ValueError:
        return None


def fecha_iso(texto):
    """'28/09/2026' (o una fecha ISO) -> '2026-09-28'. Ilegible devuelve None."""
    if not texto:
        return None
    limpio = str(texto).strip()[:10]
    for formato in ("%d/%m/%Y", "%Y-%m-%d"):
        try:
            return datetime.strptime(limpio, formato).date().isoformat()
        except ValueError:
            continue
    return None


def normalizar_registros(registros):
    """Registros de la API al formato interno; descarta lo que no interesa."""
    normalizados = []
    for registro in registros or []:
        categoria = PRODUCTO_DE_CATEGORIA.get(registro.get("product"))
        mercado = NOMBRE_DE_MERCADO.get(registro.get("market"))
        semana = fecha_iso(registro.get("beginDate"))
        precio = precio_en_euros_por_kilo(registro.get("price"))
        if categoria is None or mercado is None or semana is None or precio is None:
            continue
        normalizados.append({
            "categoria": categoria,
            "mercado": mercado,
            "semana": semana,
            "semana_fin": fecha_iso(registro.get("endDate")) or semana,
            "numero_semana": registro.get("weekNumber"),
            "campana": registro.get("marketingYear") or "",
            "precio": precio,
        })
    return normalizados


def deduplicar(registros):
    """Una fila por (semana, categoría y mercado): las semanas de cambio de
    campaña salen dos veces, con dos marketingYear."""
    vistos = set()
    unicos = []
    # Con la campaña más reciente primero, las tres categorías de una semana
    # duplicada salen siempre con la misma campaña.
    for registro in sorted(registros, key=lambda item: item["campana"], reverse=True):
        clave = (registro["semana"], registro["categoria"], registro["mercado"])
        if clave in vistos:
            continue
        vistos.add(clave)
        unicos.append(registro)
    return unicos


def _consultar_en_la_api(**filtros):
    """Llamada a la API europea. Devuelve los registros tal cual llegan."""
    try:
        respuesta = httpx.get(
            BASE + "prices",
            params={"memberStateCodes": "ES", **filtros},
            timeout=TIEMPO_DE_ESPERA,
        )
    except httpx.HTTPError:
        raise PrecioNoDisponible("No se ha podido consultar el precio del aceite.")
    if respuesta.status_code != 200:
        raise PrecioNoDisponible(
            "La fuente de precios ha devuelto el código {}.".format(respuesta.status_code)
        )
    try:
        datos = respuesta.json()
    except ValueError:
        raise PrecioNoDisponible("La fuente de precios ha devuelto una respuesta ilegible.")
    if not isinstance(datos, list):
        raise PrecioNoDisponible("La fuente de precios ha devuelto una respuesta ilegible.")
    return datos


def _asegurar_tabla(conn):
    conn.execute(ESQUEMA_CACHE)
    conn.commit()


def _guardar_en_cache(conn, registros):
    _asegurar_tabla(conn)
    for registro in registros:
        conn.execute(
            "INSERT OR REPLACE INTO precio_aceite "
            "(categoria, mercado, semana, campana, precio, semana_fin, numero_semana) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                registro["categoria"],
                registro["mercado"],
                registro["semana"],
                registro["campana"],
                registro["precio"],
                registro["semana_fin"],
                registro["numero_semana"],
            ),
        )
    conn.commit()


def _desde_cache(conn, mercado):
    _asegurar_tabla(conn)
    filas = conn.execute(
        "SELECT categoria, mercado, semana, campana, precio, semana_fin, numero_semana, "
        "consultado_en FROM precio_aceite WHERE mercado = ? ORDER BY semana ASC",
        (mercado,),
    ).fetchall()
    return [dict(fila) for fila in filas]


def _consultado_en(registros):
    marcas = [registro.get("consultado_en") for registro in registros if registro.get("consultado_en")]
    return max(marcas) if marcas else None


def _antiguedad_en_horas(consultado_en):
    if not consultado_en:
        return None
    try:
        marca = datetime.strptime(str(consultado_en)[:19], "%Y-%m-%d %H:%M:%S")
    except ValueError:
        return None
    return max(0, int((datetime.utcnow() - marca).total_seconds() // 3600))


def _respuesta(registros, categoria, mercado, campana, desactualizado):
    consultado_en = _consultado_en(registros)
    return {
        "categoria": categoria or None,
        "mercado": mercado,
        "campana": campana or None,
        "semanas": _semanas(registros, categoria),
        "fuente": FUENTE,
        "desactualizado": desactualizado,
        "consultado_en": consultado_en,
        "antiguedad_horas": _antiguedad_en_horas(consultado_en),
    }


def _semanas(registros, categoria):
    """Semanas en orden descendente, con los tres precios y la fecha en ISO."""
    por_semana = {}
    for registro in registros:
        semana = por_semana.setdefault(registro["semana"], {
            "semana": registro["semana"],
            "semana_fin": registro["semana_fin"],
            "numero_semana": registro["numero_semana"],
            "campana": registro["campana"],
            "precios": {},
        })
        semana["precios"][registro["categoria"]] = registro["precio"]
    semanas = []
    for semana in sorted(por_semana.values(), key=lambda item: item["semana"], reverse=True):
        semana = dict(semana)
        semana["precios"] = dict(sorted(semana["precios"].items()))
        semana["precio"] = semana["precios"].get(categoria) if categoria else None
        semanas.append(semana)
    return semanas


def semanas_de_precios(conn, categoria, mercado, campana):
    """Semanas de una campaña y un mercado con su precio en €/kg.

    Pide a la API (y guarda en caché) las tres categorías a la vez, para que
    el simulador pueda ponderar el reparto por calidad con la misma semana.
    Si la API falla sirve la caché indicando su antigüedad.
    """
    validar(mercado, categoria)
    if not campana:
        raise ParametroInvalido("Indica la campaña (por ejemplo, 2024/2025).")
    try:
        registros = [
            r for r in normalizar_registros(_consultar_en_la_api(marketingYears=campana))
            if r["mercado"] == mercado
        ]
        # En la caché se guarda tal cual (con su campaña); se deduplica al servir.
        _guardar_en_cache(conn, registros)
        return _respuesta(
            deduplicar([r for r in registros if r["campana"] == campana]),
            categoria, mercado, campana, desactualizado=False,
        )
    except PrecioNoDisponible:
        cache = [r for r in _desde_cache(conn, mercado) if r["campana"] == campana]
        if not cache:
            raise PrecioNoDisponible(
                "No se ha podido consultar el precio del aceite y no hay precios "
                "guardados. Inténtalo más tarde o escribe el precio a mano."
            )
        return _respuesta(deduplicar(cache), categoria, mercado, campana, desactualizado=True)


def ultima_semana(conn, mercado):
    """La última semana con las tres categorías en un mercado."""
    validar(mercado)
    hoy = date.today()
    try:
        registros = [
            r for r in normalizar_registros(_consultar_en_la_api(
                beginDate=(hoy - timedelta(days=120)).strftime("%d/%m/%Y"),
                endDate=hoy.strftime("%d/%m/%Y"),
            ))
            if r["mercado"] == mercado
        ]
        _guardar_en_cache(conn, registros)
        return _respuesta_ultima_semana(deduplicar(registros), mercado, desactualizado=False)
    except PrecioNoDisponible:
        cache = [r for r in _desde_cache(conn, mercado)]
        if not cache:
            raise PrecioNoDisponible(
                "No se ha podido consultar el precio del aceite y no hay precios "
                "guardados. Inténtalo más tarde o escribe el precio a mano."
            )
        return _respuesta_ultima_semana(deduplicar(cache), mercado, desactualizado=True)


def _respuesta_ultima_semana(registros, mercado, desactualizado):
    semana = _ultima_semana_completa(registros)
    if semana is None:
        raise PrecioNoDisponible(
            "No hay precios de las tres categorías para ese mercado."
        )
    consultado_en = _consultado_en(registros)
    return {
        "mercado": mercado,
        "semana": semana["semana"],
        "semana_fin": semana["semana_fin"],
        "numero_semana": semana["numero_semana"],
        "campana": semana["campana"],
        "precios": semana["precios"],
        "fuente": FUENTE,
        "desactualizado": desactualizado,
        "consultado_en": consultado_en,
        "antiguedad_horas": _antiguedad_en_horas(consultado_en),
    }


def _ultima_semana_completa(registros):
    """La semana más reciente que tiene AOVE, virgen y lampante."""
    for semana in _semanas(registros, None):
        if len(semana["precios"]) == len(CATEGORIAS):
            return semana
    return None


def crear_router(conexion, sesion_actual):
    """Rutas del módulo. Las dependencias (conexión y sesión) vienen de main.py
    para no importarlo de vuelta."""
    router = APIRouter(prefix="/api/v1/precios-aceite")

    @router.get("")
    def precios_aceite(
        categoria: str = "aove",
        mercado: str = "media-nacional",
        campana: str = "",
        conn=Depends(conexion),
        sesion=Depends(sesion_actual),
    ):
        """Semanas de una campaña con su precio de referencia en €/kg."""
        try:
            return semanas_de_precios(conn, categoria, mercado, campana)
        except ParametroInvalido as error:
            raise HTTPException(status_code=400, detail=str(error))
        except PrecioNoDisponible as error:
            raise HTTPException(status_code=503, detail=str(error))

    @router.get("/ultima-semana")
    def precios_ultima_semana(
        mercado: str = "media-nacional",
        conn=Depends(conexion),
        sesion=Depends(sesion_actual),
    ):
        """La última semana disponible con las tres categorías."""
        try:
            return ultima_semana(conn, mercado)
        except ParametroInvalido as error:
            raise HTTPException(status_code=400, detail=str(error))
        except PrecioNoDisponible as error:
            raise HTTPException(status_code=503, detail=str(error))

    return router
