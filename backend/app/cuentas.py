"""Cuentas de socio: registro con email y contraseña.

Reglas del MVP (decididas en la feature `registro-cuenta-socio`):
- La contraseña se guarda SIEMPRE hasheada (PBKDF2-HMAC-SHA256 con sal y
  200.000 iteraciones); nunca en texto claro.
- Longitud mínima de contraseña: 8 caracteres; email único (sin distinguir
  mayúsculas de minúsculas).
- Sin verificación de email ni reset de contraseña (limitación declarada en
  docs/risks-and-open-questions.md).
- La cuenta queda asociada a la cooperativa única del MVP.
"""

import hashlib
import re
import secrets
import uuid

ITERACIONES_PBKDF2 = 200000
LONGITUD_MINIMA = 8
PATRON_EMAIL = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


class DatosInvalidos(ValueError):
    """El email o la contraseña no cumplen las reglas mínimas."""


class EmailDuplicado(Exception):
    """Ya existe una cuenta con ese email."""


def normalizar_email(email):
    return email.strip().lower()


def validar(email, contrasena):
    if not PATRON_EMAIL.match(email):
        raise DatosInvalidos(
            "Ese email no parece válido. Revísalo (p. ej. nombre@ejemplo.com)."
        )
    if len(contrasena) < LONGITUD_MINIMA:
        raise DatosInvalidos(
            "La contraseña debe tener al menos {} caracteres.".format(LONGITUD_MINIMA)
        )


def hashear(contrasena):
    """Hashea una contraseña con sal: pbkdf2_sha256$iteraciones$sal$hash."""
    sal = secrets.token_hex(16)
    resumen = hashlib.pbkdf2_hmac(
        "sha256", contrasena.encode("utf-8"), bytes.fromhex(sal), ITERACIONES_PBKDF2
    )
    return "pbkdf2_sha256${}${}${}".format(ITERACIONES_PBKDF2, sal, resumen.hex())


def crear_cuenta(conn, cooperativa_id, email, contrasena):
    """Crea la cuenta del socio. Devuelve id y email (nunca la contraseña)."""
    email = normalizar_email(email)
    validar(email, contrasena)

    existe = conn.execute("SELECT 1 FROM cuenta WHERE email = ?", (email,)).fetchone()
    if existe:
        raise EmailDuplicado(email)

    cuenta_id = str(uuid.uuid4())
    conn.execute(
        "INSERT INTO cuenta (id, email, contrasena_hash, cooperativa_id) "
        "VALUES (?, ?, ?, ?)",
        (cuenta_id, email, hashear(contrasena), cooperativa_id),
    )
    conn.commit()
    return {"id": cuenta_id, "email": email}
