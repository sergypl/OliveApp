"""Confirmación del borrador IA (feature confirmacion-de-borrador).

Con un toque, el socio confirma el borrador y la labor queda en "Mi cuaderno"
en la campaña que corresponde a su fecha. La confirmación lleva la `nota_id`
de la nota de voz que originó el borrador y la creación es idempotente: una
Nota de voz produce como mucho una Labor, aunque se confirme dos veces. El
alta manual sin nota no cambia: sigue creando labores sueltas.
"""

from fastapi.testclient import TestClient

from app.main import app

CUENTA_A = {
    "email": "socio.uno@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 1",
}
CUENTA_B = {
    "email": "socio.dos@ejemplo.com",
    "contrasena": "segura123",
    "codigo_socio": "Socio 2",
}

NOTA = "nota-1697234567890.m4a"

BORRADOR = {
    "fecha": "2026-01-20",
    "parcela_id": None,
    "categoria": "tratamiento",
    "campos": {
        "producto": "cobre",
        "plaga_a_controlar": "mosca del olivo",
        "cantidad": 2,
        "unidad": "L",
    },
    "observaciones": "Tratamiento de invierno.",
}


def _entrar(client, cuenta):
    assert client.post("/api/v1/registro", json=cuenta).status_code == 201
    respuesta = client.post(
        "/api/v1/login",
        json={"email": cuenta["email"], "contrasena": cuenta["contrasena"]},
    )
    assert respuesta.status_code == 200
    return {"Authorization": "Bearer " + respuesta.json()["token"]}


def _parcela(client, cabeceras, nombre):
    respuesta = client.post(
        "/api/v1/mis-parcelas", json={"nombre": nombre}, headers=cabeceras
    )
    assert respuesta.status_code == 201, respuesta.text
    return respuesta.json()["id"]


def _confirmar(client, cabeceras, parcela_id, nota_id=NOTA, **cambios):
    cuerpo = dict(BORRADOR)
    cuerpo["parcela_id"] = parcela_id
    cuerpo["nota_id"] = nota_id
    cuerpo.update(cambios)
    return client.post("/api/v1/mis-labores", json=cuerpo, headers=cabeceras)


def _cuaderno(client, cabeceras):
    return client.get("/api/v1/mis-labores", headers=cabeceras).json()["campanas"]


def _labores(campanas):
    return [labor for campana in campanas for labor in campana["labores"]]


def test_confirmar_el_borrador_crea_la_labor_en_la_campana_de_su_fecha():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")

        respuesta = _confirmar(client, cabeceras, parcela_id)
        assert respuesta.status_code == 201, respuesta.text
        labor = respuesta.json()
        assert labor["campana_id"] == "2025-2026"
        assert labor["fecha"] == "2026-01-20"
        assert labor["parcela"]["nombre"] == "La Noria"
        assert labor["campos"]["producto"] == "cobre"

        campanas = _cuaderno(client, cabeceras)
        assert [c["id"] for c in campanas] == ["2025-2026"]
        assert [l["id"] for l in _labores(campanas)] == [labor["id"]]


def test_confirmar_dos_veces_la_misma_nota_no_crea_dos_labores():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")

        primera = _confirmar(client, cabeceras, parcela_id)
        segunda = _confirmar(client, cabeceras, parcela_id)
        assert primera.status_code == 201
        assert segunda.status_code == 201
        assert segunda.json()["id"] == primera.json()["id"]

        assert len(_labores(_cuaderno(client, cabeceras))) == 1


def test_confirmar_otra_vez_tras_editar_la_labor_no_crea_una_segunda():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")

        creada = _confirmar(client, cabeceras, parcela_id).json()
        editada = client.put(
            "/api/v1/mis-labores/" + creada["id"],
            json={
                "fecha": BORRADOR["fecha"],
                "parcela_id": parcela_id,
                "categoria": BORRADOR["categoria"],
                "campos": BORRADOR["campos"],
                "observaciones": "Corregida a mano.",
            },
            headers=cabeceras,
        )
        assert editada.status_code == 200

        repetida = _confirmar(client, cabeceras, parcela_id)
        assert repetida.status_code == 201
        assert repetida.json()["id"] == creada["id"]
        assert repetida.json()["observaciones"] == "Corregida a mano."

        assert len(_labores(_cuaderno(client, cabeceras))) == 1


def test_el_alta_manual_sin_nota_no_se_vuelve_idempotente():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")

        primera = _confirmar(client, cabeceras, parcela_id, nota_id="")
        segunda = _confirmar(client, cabeceras, parcela_id, nota_id="")
        assert primera.status_code == 201
        assert segunda.status_code == 201
        assert segunda.json()["id"] != primera.json()["id"]

        assert len(_labores(_cuaderno(client, cabeceras))) == 2


def test_la_nota_de_un_socio_no_evita_la_labor_del_mismo_nombre_en_otro():
    with TestClient(app) as client:
        cabeceras_a = _entrar(client, CUENTA_A)
        cabeceras_b = _entrar(client, CUENTA_B)
        parcela_a = _parcela(client, cabeceras_a, "La Noria")
        parcela_b = _parcela(client, cabeceras_b, "El Campillo")

        labor_a = _confirmar(client, cabeceras_a, parcela_a).json()
        labor_b = _confirmar(client, cabeceras_b, parcela_b).json()
        assert labor_a["id"] != labor_b["id"]

        assert len(_labores(_cuaderno(client, cabeceras_a))) == 1
        assert len(_labores(_cuaderno(client, cabeceras_b))) == 1


def test_la_confirmacion_con_fecha_de_campana_cerrada_da_un_error_claro():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")

        rechazada = _confirmar(
            client, cabeceras, parcela_id, fecha="2024-03-05"
        )
        assert rechazada.status_code == 400
        assert "campaña cerrada" in rechazada.json()["detail"]

        # La confirmación fallida no consume la nota: al corregir la fecha
        # queda la labor, y sigue siendo una sola.
        corregida = _confirmar(client, cabeceras, parcela_id)
        assert corregida.status_code == 201
        assert len(_labores(_cuaderno(client, cabeceras))) == 1


def test_la_confirmacion_pide_iniciar_sesion():
    with TestClient(app) as client:
        cabeceras = _entrar(client, CUENTA_A)
        parcela_id = _parcela(client, cabeceras, "La Noria")
        cuerpo = dict(BORRADOR)
        cuerpo["parcela_id"] = parcela_id
        cuerpo["nota_id"] = NOTA

        assert client.post("/api/v1/mis-labores", json=cuerpo).status_code == 401
