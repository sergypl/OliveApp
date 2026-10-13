"""Extracción del borrador IA desde la transcripción (feature borrador-ia-desde-transcripcion).

Reglas (ADR 0004 y docs/domain-model.md):
- El alias `chat` del gateway extrae, con salida JSON forzada, la ficha
  {fecha, parcela, categoría, campos de la categoría} de la transcripción,
  apoyándose en las parcelas del socio para desambiguar los nombres.
- Solo hay 3 categorías en el MVP (tratamiento, riego y cosecha). Si la IA no
  reconoce la categoría, el borrador se rechaza (CategoriaNoReconocida) y la
  app muestra un mensaje claro de rechazo.
- Las fechas relativas ("hoy", "ayer") se dejan tal cual: las resuelve la
  feature `fecha-relativa-en-borrador` antes de confirmar.
"""

import datetime
import json
import unicodedata

import httpx

from app import labores

GATEWAY_CHAT = "https://inference.devexpert.io/v1/chat/completions"
MODELO = "chat"
TIEMPO_DE_ESPERA = 30

# El dictado libre no usa siempre las listas cerradas (ADR 0005): se normaliza
# lo habitual antes de proponer el borrador.
SINONIMOS_CATEGORIA = {
    "tratamiento": "tratamiento",
    "tratamiento fitosanitario": "tratamiento",
    "fitosanitario": "tratamiento",
    "riego": "riego",
    "riego del olivar": "riego",
    "cosecha": "cosecha",
    "recoleccion": "cosecha",
    "recogida": "cosecha",
}
SINONIMOS_UNIDAD = {
    "kilo": "kg",
    "kilos": "kg",
    "kilogramo": "kg",
    "kilogramos": "kg",
    "litro": "L",
    "litros": "L",
    "gramo": "g",
    "gramos": "g",
    "mililitro": "ml",
    "mililitros": "ml",
    "m3": "m³",
    "metro cubico": "m³",
    "metros cubicos": "m³",
    "hora": "horas",
    "horas": "horas",
}
SINONIMOS_MODALIDAD = {
    "a vuelo": "a vuelo",
    "vuelo": "a vuelo",
    "a suelo": "a suelo",
    "suelo": "a suelo",
}

# Campos que le tocan a cada categoría (docs/domain-model.md).
CAMPOS_POR_CATEGORIA = {
    "tratamiento": ("producto", "plaga_a_controlar", "cantidad", "unidad"),
    "riego": ("cantidad", "unidad"),
    "cosecha": ("modalidad", "kilos"),
}

INSTRUCCIONES = (
    "Eres el asistente de OlivApp y extraes la ficha de una labor del olivar "
    "a partir de la transcripción de una nota de voz dictada por un socio.\n"
    "Devuelve SOLO un objeto JSON con esta forma exacta:\n"
    '{"fecha": "...", "parcela": "...", "categoria": "...", "campos": {...}, '
    '"observaciones": "..."}\n'
    "- fecha: la fecha dictada como AAAA-MM-DD. Si el socio dicta una fecha "
    'relativa ("hoy", "ayer", "la semana pasada"), devuélvela tal cual.\n'
    "- parcela: el nombre exacto de una de las parcelas del socio; si ninguna "
    "encaja, devuelve null.\n"
    "- categoria: una de estas tres palabras exactas: tratamiento, riego, "
    "cosecha. Si la transcripción no describe ninguna de esas labores, "
    'devuelve "categoria": null y "campos": {}.\n'
    "- campos: solo los de la categoría:\n"
    '  tratamiento: {"producto": texto, "plaga_a_controlar": texto, '
    '"cantidad": número, "unidad": "kg"|"L"|"g"|"ml"}\n'
    '  riego: {"cantidad": número, "unidad": "horas"|"m³"|"L"}\n'
    '  cosecha: {"modalidad": "a vuelo"|"a suelo", "kilos": número}\n'
    "- plaga_a_controlar debe ser una de: "
    + ", ".join(labores.PLAGAS)
    + ".\n"
    "- observaciones: solo si el socio dicta una nota libre o detalles que no "
    'encajan en los campos; si no, devuelve "".\n'
    "Corrige los errores de transcripción con el sentido del dictado y usa el "
    "vocabulario exacto de las listas."
)


class ExtraccionFallida(RuntimeError):
    """El gateway no pudo extraer el borrador de la transcripción."""


class CategoriaNoReconocida(ValueError):
    """La IA no reconoció una categoría de labor del MVP."""


def extraer(api_key, texto, parcelas, hoy=None):
    """Extrae el borrador IA de una transcripción; rechaza categorías ajenas.

    `parcelas` son los nombres de las parcelas del socio (para desambiguar)
    y `hoy` la fecha de referencia que se le pasa a la IA (hoy por defecto).
    """
    hoy = hoy or datetime.date.today().isoformat()
    mensajes = [
        {"role": "system", "content": INSTRUCCIONES},
        {
            "role": "user",
            "content": "Fecha de hoy: {}.\nParcelas del socio: {}.\nTranscripción: {}".format(
                hoy, ", ".join(parcelas) or "(ninguna)", texto
            ),
        },
    ]
    datos = _extraer_json(api_key, mensajes)
    return _construir_borrador(datos, parcelas)


def _extraer_json(api_key, mensajes):
    contenido = _llamar_al_gateway(api_key, mensajes)
    try:
        return json.loads(contenido)
    except ValueError:
        # Plan B del ADR 0004: se pide JSON válido una vez más antes de fallar.
        mensajes = mensajes + [{"role": "user", "content": "Responde solo con JSON válido."}]
        contenido = _llamar_al_gateway(api_key, mensajes)
    try:
        return json.loads(contenido)
    except ValueError:
        raise ExtraccionFallida("El gateway devolvió una respuesta que no es JSON.")


def _llamar_al_gateway(api_key, mensajes):
    try:
        respuesta = httpx.post(
            GATEWAY_CHAT,
            headers={"Authorization": "Bearer " + api_key},
            json={
                "model": MODELO,
                "messages": mensajes,
                "response_format": {"type": "json_object"},
            },
            timeout=TIEMPO_DE_ESPERA,
        )
    except httpx.HTTPError as error:
        raise ExtraccionFallida(str(error))
    if respuesta.status_code != 200:
        raise ExtraccionFallida(
            "El gateway devolvió el código {}".format(respuesta.status_code)
        )
    try:
        return respuesta.json()["choices"][0]["message"]["content"]
    except (ValueError, KeyError, IndexError, TypeError):
        raise ExtraccionFallida("Respuesta inesperada del gateway.")


def _sin_acentos(texto):
    return "".join(
        letra
        for letra in unicodedata.normalize("NFD", str(texto or "").strip().lower())
        if unicodedata.category(letra) != "Mn"
    )


def _buscar_en_lista(valor, lista):
    """Empareja un texto del dictado con un valor de lista cerrada (sin acentos)."""
    buscado = _sin_acentos(valor)
    for opcion in lista:
        if _sin_acentos(opcion) == buscado:
            return opcion
    return None


def _buscar_parcela(nombre, parcelas):
    """La parcela dictada, siempre con el nombre exacto de las del socio.

    Se compara sin acentos ni espacios para absorber typos de transcripción
    ("Lanoria" → "La Noria", ADR 0004). Si ninguna encaja, queda vacía: la
    elige el socio en `borrador-sin-parcela`.
    """
    buscado = _sin_acentos(nombre).replace(" ", "")
    for parcela in parcelas:
        if _sin_acentos(parcela).replace(" ", "") == buscado:
            return parcela
    return ""


def _numero(valor):
    try:
        return float(valor)
    except (TypeError, ValueError):
        return valor


def _construir_borrador(datos, parcelas):
    """Normaliza la salida de la IA al borrador del dominio.

    Si la categoría no es una de las tres del MVP, el borrador se rechaza.
    """
    categoria = SINONIMOS_CATEGORIA.get(_sin_acentos(datos.get("categoria")))
    if categoria not in labores.CATEGORIAS:
        raise CategoriaNoReconocida(
            "La IA no reconoció la categoría de la labor."
        )
    campos = dict(datos.get("campos") or {})
    if "unidad" in campos:
        campos["unidad"] = SINONIMOS_UNIDAD.get(
            _sin_acentos(campos.get("unidad")), campos.get("unidad")
        )
    if "modalidad" in campos:
        campos["modalidad"] = SINONIMOS_MODALIDAD.get(
            _sin_acentos(campos.get("modalidad")), campos.get("modalidad")
        )
    for campo in ("cantidad", "kilos"):
        if campo in campos:
            campos[campo] = _numero(campos[campo])
    # Solo los campos de la categoría (docs/domain-model.md).
    campos = {
        campo: campos[campo]
        for campo in CAMPOS_POR_CATEGORIA[categoria]
        if campo in campos and campos[campo] not in (None, "")
    }
    return {
        "fecha": str(datos.get("fecha") or "").strip(),
        "parcela": _buscar_parcela(datos.get("parcela"), parcelas),
        "categoria": categoria,
        "campos": campos,
        "observaciones": str(datos.get("observaciones") or "").strip(),
    }
