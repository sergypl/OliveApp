"""Transcripción de notas de voz vía el gateway (feature transcripcion-de-nota-voz).

Reglas (ADR 0004 y docs/domain-model.md):
- La app envía el audio al backend y el backend lo transcribe con el alias
  `transcribe` del gateway. La clave del gateway nunca llega a la app.
- El audio no se conserva en el servidor: se borra tras la transcripción
  (en realidad solo vive en un fichero temporal durante la llamada).
- Si la transcripción falla, la nota permanece pendiente y la app puede
  reintentar.
"""

import mimetypes
import os
import tempfile

import httpx

GATEWAY_TRANSCRIPCIONES = "https://inference.devexpert.io/v1/audio/transcriptions"
MODELO = "transcribe"
TIEMPO_DE_ESPERA = 60

# 5 MB sobra de sobra para los 60 segundos máximo de una nota de voz.
LIMITE_BYTES = 5 * 1024 * 1024
TROZO_LEIDO = 64 * 1024
FORMATOS_ACEPTADOS = (".m4a", ".mp4", ".aac", ".wav", ".ogg", ".webm")


class AudioInvalido(ValueError):
    """El audio enviado no cumple las reglas (formato, tamaño o ilegible)."""


class TranscripcionFallida(RuntimeError):
    """El gateway no pudo transcribir el audio."""


def validar_formato(nombre):
    extension = os.path.splitext(nombre or "")[1].lower()
    if extension not in FORMATOS_ACEPTADOS:
        raise AudioInvalido("Formato de audio no válido. Graba la nota de nuevo.")
    return extension


def validar_audio(nombre, tamano):
    extension = validar_formato(nombre)
    if tamano > LIMITE_BYTES:
        raise AudioInvalido("El audio es demasiado grande para transcribirlo.")
    return extension


def leer_audio(nombre, archivo):
    """Lee una subida por trozos y rechaza la que supera el límite de tamaño.

    Nunca se carga un audio entero en memoria: al superar el límite se corta y
    se responde que es demasiado grande (la validación completa está en
    `transcribir`).
    """
    validar_formato(nombre)
    trozos = []
    total = 0
    while True:
        trozo = archivo.read(TROZO_LEIDO)
        if not trozo:
            break
        total += len(trozo)
        if total > LIMITE_BYTES:
            raise AudioInvalido("El audio es demasiado grande para transcribirlo.")
        trozos.append(trozo)
    return b"".join(trozos)


def transcribir(api_key, nombre, contenido):
    """Transcribe un audio y devuelve el texto; el audio no se conserva."""
    extension = validar_audio(nombre, len(contenido))
    ruta = None
    try:
        with tempfile.NamedTemporaryFile(suffix=extension, delete=False) as temporal:
            temporal.write(contenido)
            ruta = temporal.name
        texto = _llamar_al_gateway(api_key, ruta, nombre)
        if not texto.strip():
            raise AudioInvalido(
                "No se ha entendido el audio. Vuelve a grabar la nota."
            )
        return texto.strip()
    finally:
        # El audio se borra del servidor tras transcribir.
        if ruta and os.path.exists(ruta):
            os.remove(ruta)


def _llamar_al_gateway(api_key, ruta, nombre):
    tipo = mimetypes.guess_type(nombre or "")[0] or "application/octet-stream"
    try:
        with open(ruta, "rb") as audio:
            respuesta = httpx.post(
                GATEWAY_TRANSCRIPCIONES,
                headers={"Authorization": "Bearer " + api_key},
                data={"model": MODELO},
                files={"file": (nombre or "nota.m4a", audio, tipo)},
                timeout=TIEMPO_DE_ESPERA,
            )
    except httpx.HTTPError as error:
        raise TranscripcionFallida(str(error))
    if respuesta.status_code != 200:
        raise TranscripcionFallida(
            "El gateway devolvió el código {}".format(respuesta.status_code)
        )
    try:
        texto = respuesta.json()["text"]
    except (ValueError, KeyError, TypeError):
        raise TranscripcionFallida("Respuesta inesperada del gateway.")
    # La respuesta del gateway no es de fiar: si `text` no es un texto, la
    # transcripción falla con un error claro en vez de reventar al tratarla.
    if not isinstance(texto, str):
        raise TranscripcionFallida("Respuesta inesperada del gateway.")
    return texto
