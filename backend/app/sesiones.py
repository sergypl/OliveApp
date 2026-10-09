"""Sesiones del socio: tokens JWT con caducidad (feature inicio-de-sesion).

- Firma HS256 con el secreto JWT_SECRET (solo en el servidor; nunca en la app).
- Caducidad: 30 días, para que el socio sénior no tenga que iniciar sesión a
  menudo; la app guarda el token en almacenamiento seguro del dispositivo.
"""

import os
import time

import jwt

DURACION_EN_SEGUNDOS = 30 * 24 * 60 * 60  # 30 días


class SesionInvalida(Exception):
    """Token ausente, mal formado, con firma incorrecta o caducado."""


def crear_token(cuenta):
    """Crea el token de sesión de una cuenta (id y email)."""
    ahora = int(time.time())
    return jwt.encode(
        {
            "sub": cuenta["id"],
            "email": cuenta["email"],
            "iat": ahora,
            "exp": ahora + DURACION_EN_SEGUNDOS,
        },
        os.environ["JWT_SECRET"],
        algorithm="HS256",
    )


def leer_token(token):
    """Verifica el token y devuelve su contenido. Falla con SesionInvalida."""
    try:
        return jwt.decode(token, os.environ["JWT_SECRET"], algorithms=["HS256"])
    except jwt.PyJWTError:
        raise SesionInvalida()
