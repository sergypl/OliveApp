"""Perfil del socio (feature perfil-del-socio).

Vista de solo lectura de la cuenta autenticada: código de socio, email,
nombre de la cooperativa, número de parcelas y campaña actual. No es una
entidad nueva (docs/domain-model.md): se compone de la cuenta, su socio, la
cooperativa, las parcelas del socio y la campaña marcada como actual.
"""

from fastapi import APIRouter, Depends, HTTPException

SESION_NO_VALIDA = "Sesión caducada o no válida. Vuelve a iniciar sesión."


def perfil_de_la_cuenta(conn, cuenta_id):
    """El perfil de la cuenta, o `None` si la cuenta ya no existe.

    Todo se resuelve desde el socio vinculado a la cuenta del token: nunca
    desde un identificador que llegue en la petición, así un socio no puede
    ver los datos de otro.
    """
    fila = conn.execute(
        "SELECT c.email, s.id AS socio_id, s.codigo, coop.nombre AS cooperativa "
        "FROM cuenta c "
        "JOIN socio s ON s.id = c.socio_id "
        "JOIN cooperativa coop ON coop.id = s.cooperativa_id "
        "WHERE c.id = ?",
        (cuenta_id,),
    ).fetchone()
    if not fila:
        return None
    parcelas = conn.execute(
        "SELECT COUNT(*) AS total FROM parcela WHERE socio_id = ?", (fila["socio_id"],)
    ).fetchone()["total"]
    campana = conn.execute(
        "SELECT nombre FROM campana WHERE es_actual = 1"
    ).fetchone()
    return {
        "codigo_socio": fila["codigo"],
        "email": fila["email"],
        "cooperativa": fila["cooperativa"],
        "numero_parcelas": parcelas,
        "campana_actual": campana["nombre"] if campana else None,
    }


def crear_router(conexion, sesion_actual):
    """Router con `GET /api/v1/mi-perfil`, usando las dependencias de la app."""
    router = APIRouter()

    @router.get("/api/v1/mi-perfil")
    def mi_perfil(conn=Depends(conexion), sesion=Depends(sesion_actual)):
        """Perfil del socio autenticado (solo lectura)."""
        perfil = perfil_de_la_cuenta(conn, sesion["sub"])
        if perfil is None:
            raise HTTPException(status_code=401, detail=SESION_NO_VALIDA)
        return perfil

    return router
