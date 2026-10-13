"""Cuentas de socio: registro con email, contraseña y código de socio.

Reglas del MVP (decididas en la feature `registro-cuenta-socio`):
- La contraseña se guarda SIEMPRE hasheada (PBKDF2-HMAC-SHA256 con sal y
  200.000 iteraciones); nunca en texto claro.
- Longitud mínima de contraseña: 8 caracteres; email único (sin distinguir
  mayúsculas de minúsculas).
- Sin verificación de email ni reset de contraseña (limitación declarada en
  docs/risks-and-open-questions.md).
- La cuenta queda asociada a la cooperativa única del MVP.

Vinculación cuenta ↔ socio (feature `aislamiento-de-datos-por-socio`):
el socio se identifica con el **código de socio** que le da su cooperativa
(en el seed: "Socio 1", "Socio 2"); la cuenta queda vinculada a ese socio y
a partir de ahí solo ve sus propios datos.
"""

import hashlib
import hmac
import re
import secrets
import uuid

ITERACIONES_PBKDF2 = 200000
LONGITUD_MINIMA = 8

PATRON_EMAIL = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


class DatosInvalidos(ValueError):
    """El email, la contraseña o el código de socio no cumplen las reglas."""


class EmailDuplicado(Exception):
    """Ya existe una cuenta con ese email."""


class SocioDesconocido(ValueError):
    """El código de socio no corresponde a ningún socio de la cooperativa."""


def normalizar_email(email):
    return email.strip().lower()


def normalizar_codigo(codigo):
    """Normaliza un código de socio: sin espacios dobles y sin distinguir mayúsculas."""
    return " ".join((codigo or "").split()).casefold()


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


def verificar_contrasena(contrasena, contrasena_hash):
    """Compara una contraseña con su hash guardado (tiempo constante)."""
    try:
        algoritmo, iteraciones, sal, resumen = contrasena_hash.split("$")
        if algoritmo != "pbkdf2_sha256":
            return False
        calculado = hashlib.pbkdf2_hmac(
            "sha256",
            contrasena.encode("utf-8"),
            bytes.fromhex(sal),
            int(iteraciones),
        )
        return hmac.compare_digest(calculado.hex(), resumen)
    except (ValueError, TypeError):
        return False


def crear_cuenta(conn, cooperativa_id, email, contrasena, codigo_socio):
    """Crea la cuenta del socio vinculada a su socio de la cooperativa.

    El código de socio debe corresponder a un socio de la cooperativa (el que
    le ha dado la cooperativa); si no, la cuenta no se crea. Devuelve id y
    email (nunca la contraseña).
    """
    email = normalizar_email(email)
    validar(email, contrasena)

    codigo = (codigo_socio or "").strip()
    if not codigo:
        raise DatosInvalidos("Escribe el código de socio que te ha dado tu cooperativa.")
    socio = conn.execute(
        "SELECT id FROM socio WHERE cooperativa_id = ? AND lower(codigo) = ?",
        (cooperativa_id, normalizar_codigo(codigo)),
    ).fetchone()
    if not socio:
        raise SocioDesconocido(codigo_socio)

    existe = conn.execute("SELECT 1 FROM cuenta WHERE email = ?", (email,)).fetchone()
    if existe:
        raise EmailDuplicado(email)

    cuenta_id = str(uuid.uuid4())
    conn.execute(
        "INSERT INTO cuenta (id, email, contrasena_hash, cooperativa_id, socio_id) "
        "VALUES (?, ?, ?, ?, ?)",
        (cuenta_id, email, hashear(contrasena), cooperativa_id, socio["id"]),
    )
    conn.commit()
    return {"id": cuenta_id, "email": email, "socio_id": socio["id"]}
