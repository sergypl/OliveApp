"""Extracción y completado del borrador IA (features borrador-ia-desde-transcripcion
y completar-borrador-por-preguntas).

Reglas (ADR 0004, ADR 0005 y docs/domain-model.md):
- El alias `chat` del gateway extrae, con salida JSON forzada, la ficha
  {fecha, parcela, categoría, campos de la categoría} de la transcripción,
  apoyándose en las parcelas del socio para desambiguar los nombres.
- Solo hay 3 categorías en el MVP (tratamiento, riego y cosecha). Si la IA no
  reconoce la categoría, el borrador se rechaza (CategoriaNoReconocida) y la
  app muestra un mensaje claro de rechazo.
- Relleno conversacional (ADR 0005): cuando faltan campos obligatorios, el
  asistente pregunta por ellos (una pregunta por respuesta) y el socio contesta
  por voz hasta completar la ficha. Cada borrador lleva su `pregunta` (vacía
  cuando ya está completo y listo para confirmar).
- Las fechas relativas sencillas ("hoy", "ayer", "anteayer") se resuelven a un
  día concreto contra la fecha de hoy (feature `fecha-relativa-en-borrador`).
  Las fechas ambiguas ("el 5 de diciembre", "la semana pasada") quedan tal cual:
  no tienen un día concreto y la fecha la elige el socio en el calendario antes
  de confirmar.
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

# Pregunta del asistente por campo faltante, según la categoría. La redacta el
# alias `chat`; esta es la de respaldo cuando la IA no la devuelve.
PREGUNTAS_POR_CAMPO = {
    "tratamiento": {
        "producto": "¿Qué producto has echado?",
        "plaga_a_controlar": "¿Contra qué plaga lo has echado?",
        "cantidad": "¿Cuánta cantidad has echado?",
        "unidad": "¿En qué unidad la mides: kilos, litros, gramos o mililitros?",
    },
    "riego": {
        "cantidad": "¿Cuánto has regado?",
        "unidad": "¿En qué unidad lo mides: horas, metros cúbicos o litros?",
    },
    "cosecha": {
        "modalidad": "¿Has recogido a vuelo o a suelo?",
        "kilos": "¿Cuántos kilos de aceituna has recogido?",
    },
}

INSTRUCCIONES = (
    "Eres el asistente de OlivApp y extraes la ficha de una labor del olivar "
    "a partir de la transcripción de una nota de voz dictada por un socio.\n"
    "Devuelve SOLO un objeto JSON con esta forma exacta:\n"
    '{"fecha": "...", "parcela": "...", "categoria": "...", "campos": {...}, '
    '"observaciones": "...", "preguntas": {...}}\n'
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
    "- preguntas: si falta algún campo obligatorio de la categoría, una "
    "pregunta breve y natural para el socio por cada campo que no hayas podido "
    'rellenar (una por campo). Ejemplo: {"producto": "¿Qué producto has '
    'echado?"}. Si no falta nada, devuelve {}.\n'
    "Corrige los errores de transcripción con el sentido del dictado y usa el "
    "vocabulario exacto de las listas."
)

INSTRUCCIONES_COMPLETAR = (
    "Eres el asistente de OlivApp y ayudas a un socio a completar la ficha de "
    "una labor del olivar. El socio te responde por voz y hay campos "
    "obligatorios que siguen faltando.\n"
    "Devuelve SOLO un objeto JSON con esta forma exacta:\n"
    '{"campos": {...}, "preguntas": {...}}\n'
    "- campos: solo los valores de los campos que faltan (te doy la lista) que "
    "se puedan sacar de la respuesta del socio. Si la respuesta no aporta un "
    "campo, no lo incluyas. Formato por campo:\n"
    "  producto: texto. plaga_a_controlar: una de: "
    + ", ".join(labores.PLAGAS)
    + ". cantidad: número. "
    'unidad: "kg"|"L"|"g"|"ml" en tratamiento u "horas"|"m³"|"L" en riego. '
    'modalidad: "a vuelo"|"a suelo". kilos: número.\n'
    "- preguntas: una pregunta breve y natural para el socio por cada campo "
    "que siga faltando tras su respuesta (una por campo), para seguir "
    'completando la ficha. Ejemplo: {"cantidad": "¿Cuánta cantidad has '
    'echado?"}.\n'
    "Habla de tú, en español sencillo y sin tecnicismos. Si la respuesta no "
    'tiene nada que ver con la labor, devuelve {"campos": {}, "preguntas": {}}.'
)


class ExtraccionFallida(RuntimeError):
    """El gateway no pudo extraer el borrador de la transcripción."""


class CategoriaNoReconocida(ValueError):
    """La IA no reconoció una categoría de labor del MVP."""


def extraer(api_key, texto, parcelas, hoy=None):
    """Extrae el borrador IA de una transcripción; rechaza categorías ajenas.

    `parcelas` son los nombres de las parcelas del socio (para desambiguar)
    y `hoy` la fecha de referencia que se le pasa a la IA (hoy por defecto).
    El borrador lleva su primera `pregunta` si faltan campos obligatorios
    (feature completar-borrador-por-preguntas).
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
    return _con_pregunta(_construir_borrador(datos, parcelas, hoy), datos.get("preguntas"))


def completar(api_key, borrador, texto_respuesta):
    """Completa el borrador con la respuesta por voz del socio y pregunta lo siguiente.

    La respuesta puede aportar varios campos de golpe; solo se rellenan los
    que faltaban (los ya dictados no se tocan). Después el borrador vuelve con
    la `pregunta` por el campo que siga faltando (una pregunta por respuesta)
    y, cuando ya no falta ninguno, con la pregunta vacía: listo para confirmar.
    """
    categoria = borrador.get("categoria")
    faltantes = campos_faltantes(borrador)
    actualizado = dict(borrador)
    actualizado["campos"] = dict(borrador.get("campos") or {})
    if not faltantes:
        actualizado["pregunta"] = ""
        return actualizado
    mensajes = [
        {"role": "system", "content": INSTRUCCIONES_COMPLETAR},
        {
            "role": "user",
            "content": (
                "Categoría de la labor: {}.\n"
                "Datos que ya tiene la ficha: {}.\n"
                "Campos que faltan: {}.\n"
                "Respuesta del socio: {}"
            ).format(
                categoria,
                json.dumps(
                    {
                        "fecha": borrador.get("fecha"),
                        "parcela": borrador.get("parcela"),
                        "campos": actualizado["campos"],
                        "observaciones": borrador.get("observaciones"),
                    },
                    ensure_ascii=False,
                ),
                ", ".join(faltantes),
                texto_respuesta,
            ),
        },
    ]
    datos = _extraer_json(api_key, mensajes)
    nuevos = _normalizar_campos(categoria, datos.get("campos") or {})
    for campo in faltantes:
        if campo in nuevos:
            actualizado["campos"][campo] = nuevos[campo]
    return _con_pregunta(actualizado, datos.get("preguntas"))


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


def _normalizar_campos(categoria, campos):
    """Normaliza los campos de la IA a las listas cerradas del dominio.

    Solo sobreviven los campos de la categoría con algún valor (ADR 0005).
    """
    campos = dict(campos or {})
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
    return {
        campo: campos[campo]
        for campo in CAMPOS_POR_CATEGORIA.get(categoria, ())
        if campo in campos and campos[campo] not in (None, "")
    }


def campos_faltantes(borrador):
    """Campos obligatorios de la categoría que aún faltan en el borrador.

    Son los que el asistente pregunta por voz hasta completar la ficha
    (feature completar-borrador-por-preguntas).
    """
    campos = borrador.get("campos") or {}
    return tuple(
        campo
        for campo in CAMPOS_POR_CATEGORIA.get(borrador.get("categoria"), ())
        if campos.get(campo) in (None, "")
    )


def _pregunta_para(categoria, campo, preguntas):
    """La pregunta del asistente para un campo, la de la IA o la de respaldo."""
    redactada = ""
    if isinstance(preguntas, dict):
        redactada = str(preguntas.get(campo) or "").strip()
    return redactada or PREGUNTAS_POR_CAMPO.get(categoria, {}).get(campo, "")


def _con_pregunta(borrador, preguntas=None):
    """Borrador con su `pregunta` por el campo que falta (vacía si está completo).

    Siempre hay pregunta cuando falta un campo obligatorio: la redactada por la
    IA o, en su defecto, la de respaldo por campo y categoría.
    """
    borrador = dict(borrador)
    faltantes = campos_faltantes(borrador)
    borrador["pregunta"] = (
        _pregunta_para(borrador.get("categoria"), faltantes[0], preguntas)
        if faltantes
        else ""
    )
    return borrador


# Expresiones de fecha que sí tienen un día concreto (fecha-relativa-en-borrador).
FECHAS_RELATIVAS = {"hoy": 0, "ayer": 1, "anteayer": 2}


def resolver_fecha(fecha, hoy):
    """Fecha del borrador IA a un día concreto (fecha-relativa-en-borrador).

    Las expresiones relativas sencillas ("hoy", "ayer", "anteayer") se resuelven
    contra `hoy` (AAAA-MM-DD). El resto queda tal cual: las fechas ambiguas
    ("el 5 de diciembre", "la semana pasada") y las vacías no tienen un día
    concreto y la fecha la elige el socio en el calendario antes de confirmar.
    """
    texto = str(fecha or "").strip()
    if not texto:
        return ""
    dias = FECHAS_RELATIVAS.get(_sin_acentos(texto))
    if dias is None:
        return texto
    base = datetime.date.fromisoformat(str(hoy))
    return str(base - datetime.timedelta(days=dias))


def _construir_borrador(datos, parcelas, hoy):
    """Normaliza la salida de la IA al borrador del dominio.

    Si la categoría no es una de las tres del MVP, el borrador se rechaza.
    """
    categoria = SINONIMOS_CATEGORIA.get(_sin_acentos(datos.get("categoria")))
    if categoria not in labores.CATEGORIAS:
        raise CategoriaNoReconocida(
            "La IA no reconoció la categoría de la labor."
        )
    return {
        "fecha": resolver_fecha(datos.get("fecha"), hoy),
        "parcela": _buscar_parcela(datos.get("parcela"), parcelas),
        "categoria": categoria,
        "campos": _normalizar_campos(categoria, datos.get("campos")),
        "observaciones": str(datos.get("observaciones") or "").strip(),
    }
