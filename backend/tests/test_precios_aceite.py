"""Precios de referencia del aceite (feature simulador-avanzado-precio-aceite).

Todo sin red: la API de la Comisión Europea se sustituye por un fixture grabado
(`fixtures/precios_api_bruto.json`, respuesta real de `GET prices` con
memberStateCodes=ES) que se filtra como lo haría la API.
"""

import json
import sqlite3
from datetime import datetime
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app import db as basedatos
from app import precios
from app.main import app

FIXTURE = Path(__file__).parent / "fixtures" / "precios_api_bruto.json"
DATOS = {"email": "socio@ejemplo.com", "contrasena": "segura123", "codigo_socio": "Socio 1"}


@pytest.fixture(scope="module")
def registros_brutos():
    return json.loads(FIXTURE.read_text(encoding="utf-8"))


def _fecha(texto):
    return datetime.strptime(texto, "%d/%m/%Y").date()


def _filtrar_como_la_api(registros, filtros):
    resultado = registros
    if "marketingYears" in filtros:
        resultado = [r for r in resultado if r["marketingYear"] == filtros["marketingYears"]]
    if "beginDate" in filtros:
        desde = _fecha(filtros["beginDate"])
        resultado = [r for r in resultado if _fecha(r["beginDate"]) >= desde]
    return resultado


@pytest.fixture
def api_falsa(monkeypatch, registros_brutos):
    """Sustituye la llamada de red por el fixture, aplicando los filtros."""
    llamadas = []

    def falsa(**filtros):
        llamadas.append(filtros)
        return _filtrar_como_la_api(registros_brutos, filtros)

    monkeypatch.setattr(precios, "_consultar_en_la_api", falsa)
    return llamadas


@pytest.fixture
def api_caida(monkeypatch):
    def falsa(**filtros):
        raise precios.PrecioNoDisponible("sin red")

    monkeypatch.setattr(precios, "_consultar_en_la_api", falsa)


@pytest.fixture
def conn():
    conexion = basedatos.conectar()
    basedatos.crear_esquema(conexion)
    yield conexion
    conexion.close()


def _entrar(client):
    assert client.post("/api/v1/registro", json=DATOS).status_code == 201
    token = client.post("/api/v1/login", json=DATOS).json()["token"]
    return {"Authorization": "Bearer " + token}


# --- Parseo y conversión ---------------------------------------------------

def test_precio_en_euros_por_100kg_pasa_a_euros_por_kilo():
    assert precios.precio_en_euros_por_kilo("€445.00") == 4.45
    assert precios.precio_en_euros_por_kilo("€298.00") == 2.98
    assert precios.precio_en_euros_por_kilo(" 1050.5 ") == 10.505


@pytest.mark.parametrize("texto", [None, "", "  ", "€", "n/d", "€abc"])
def test_precio_ilegible_devuelve_none(texto):
    assert precios.precio_en_euros_por_kilo(texto) is None


def test_fecha_de_la_api_pasa_a_iso():
    assert precios.fecha_iso("28/09/2026") == "2026-09-28"
    assert precios.fecha_iso("2026-09-28T00:00:00") == "2026-09-28"
    assert precios.fecha_iso("ayer") is None
    assert precios.fecha_iso(None) is None


# --- Mapeo, filtros y deduplicación ---------------------------------------

def test_solo_se_mapean_las_tres_categorias_y_los_mercados_del_mvp(registros_brutos):
    normalizados = precios.normalizar_registros(registros_brutos)
    assert {r["categoria"] for r in normalizados} == {"aove", "virgen", "lampante"}
    assert {r["mercado"] for r in normalizados} <= set(precios.MERCADOS)
    # Hay refinados y orujo en el fixture y no deben colarse.
    brutos_ignorados = [
        r for r in registros_brutos
        if r["product"] not in precios.PRODUCTO_DE_CATEGORIA
    ]
    assert brutos_ignorados
    assert len(normalizados) <= len(registros_brutos) - len(brutos_ignorados)


def test_el_mapeo_de_producto_y_el_precio_por_kilo_son_los_esperados():
    normalizados = precios.normalizar_registros([{
        "beginDate": "28/09/2026", "endDate": "04/10/2026", "weekNumber": 53,
        "product": "Extra virgin olive oil (up to 0.8%)", "market": "Jaén (ES616)",
        "marketingYear": "2025/2026", "price": "€445.00",
    }])
    assert normalizados == [{
        "categoria": "aove", "mercado": "jaen", "semana": "2026-09-28",
        "semana_fin": "2026-10-04", "numero_semana": 53, "campana": "2025/2026",
        "precio": 4.45,
    }]


def test_se_ignoran_refinados_orujo_mercados_ajenos_y_precios_ilegibles():
    base = {"beginDate": "28/09/2026", "endDate": "04/10/2026", "weekNumber": 1,
            "marketingYear": "2025/2026", "price": "€100.00"}
    registros = [
        {**base, "product": "Refined olive oil (up to 0.3%)", "market": "Jaén (ES616)"},
        {**base, "product": "Crude olive-pomace oil (from 5 to 10%)", "market": "Jaén (ES616)"},
        {**base, "product": "Virgin olive oil (up to 2%)", "market": "Lisboa (PT170)"},
        {**base, "product": "Virgin olive oil (up to 2%)", "market": "Jaén (ES616)", "price": ""},
        {**base, "product": "Virgin olive oil (up to 2%)", "market": "Jaén (ES616)"},
    ]
    assert len(precios.normalizar_registros(registros)) == 1


def test_las_semanas_de_cambio_de_campana_se_deduplican(registros_brutos):
    normalizados = precios.normalizar_registros(registros_brutos)
    claves = [(r["semana"], r["categoria"], r["mercado"]) for r in normalizados]
    assert len(claves) != len(set(claves)), "el fixture debe traer semanas duplicadas"
    unicos = precios.deduplicar(normalizados)
    claves_unicas = [(r["semana"], r["categoria"], r["mercado"]) for r in unicos]
    assert len(claves_unicas) == len(set(claves_unicas)) == len(set(claves))


def test_la_semana_duplicada_sale_con_la_misma_campana_en_las_tres_categorias():
    base = {"beginDate": "28/09/2026", "endDate": "04/10/2026", "weekNumber": 53,
            "market": "Jaén (ES616)", "price": "€300.00"}
    registros = []
    for producto in precios.CATEGORIAS.values():
        for campana in ("2025/2026", "2026/2027"):
            registros.append({**base, "product": producto, "marketingYear": campana})
    unicos = precios.deduplicar(precios.normalizar_registros(registros))
    assert len(unicos) == 3
    assert {r["campana"] for r in unicos} == {"2026/2027"}


# --- Rutas, filtros y caché -----------------------------------------------

def test_las_rutas_exigen_sesion(api_falsa):
    with TestClient(app) as client:
        assert client.get("/api/v1/precios-aceite?campana=2025/2026").status_code == 401
        assert client.get("/api/v1/precios-aceite/ultima-semana").status_code == 401


def test_semanas_de_una_campana_con_precio_por_kilo(api_falsa):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.get(
            "/api/v1/precios-aceite",
            params={"categoria": "aove", "mercado": "jaen", "campana": "2025/2026"},
            headers=cabeceras,
        )
    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert cuerpo["fuente"] == "Comisión Europea · Agri-food Data Portal"
    assert cuerpo["desactualizado"] is False
    assert cuerpo["mercado"] == "jaen"
    semanas = cuerpo["semanas"]
    assert len(semanas) > 20
    # Orden descendente, una sola fila por semana y precios plausibles en €/kg.
    fechas = [s["semana"] for s in semanas]
    assert fechas == sorted(fechas, reverse=True)
    assert len(fechas) == len(set(fechas))
    for semana in semanas:
        assert semana["campana"] == "2025/2026"
        assert 1 < semana["precio"] < 15
        assert semana["precio"] == semana["precios"]["aove"]


def test_sin_campana_o_con_mercado_o_categoria_desconocidos_es_400(api_falsa):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        url = "/api/v1/precios-aceite"
        assert client.get(url, headers=cabeceras).status_code == 400
        assert client.get(url, params={"campana": "2025/2026", "mercado": "lisboa"},
                          headers=cabeceras).status_code == 400
        assert client.get(url, params={"campana": "2025/2026", "categoria": "orujo"},
                          headers=cabeceras).status_code == 400
        assert client.get(url + "/ultima-semana", params={"mercado": "lisboa"},
                          headers=cabeceras).status_code == 400
    assert api_falsa == []


def test_la_consulta_pide_a_la_api_solo_espana_y_la_campana(api_falsa, conn):
    precios.semanas_de_precios(conn, "aove", "media-nacional", "2025/2026")
    assert api_falsa == [{"marketingYears": "2025/2026"}]


def test_ultima_semana_trae_las_tres_categorias_de_la_semana_mas_reciente(api_falsa):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.get(
            "/api/v1/precios-aceite/ultima-semana",
            params={"mercado": "media-nacional"}, headers=cabeceras,
        )
    assert respuesta.status_code == 200
    cuerpo = respuesta.json()
    assert set(cuerpo["precios"]) == {"aove", "virgen", "lampante"}
    assert cuerpo["mercado"] == "media-nacional"
    assert cuerpo["fuente"] == "Comisión Europea · Agri-food Data Portal"
    # Es la semana más reciente que tiene las tres categorías en ese mercado.
    brutos = json.loads(FIXTURE.read_text(encoding="utf-8"))
    semanas = {}
    for registro in precios.deduplicar(precios.normalizar_registros(brutos)):
        if registro["mercado"] == "media-nacional":
            semanas.setdefault(registro["semana"], set()).add(registro["categoria"])
    completas = [s for s, categorias in semanas.items() if len(categorias) == 3]
    assert cuerpo["semana"] == max(completas)
    assert cuerpo["precios"]["aove"] > cuerpo["precios"]["lampante"]


def test_los_resultados_se_guardan_en_la_cache_sqlite(api_falsa, conn):
    precios.semanas_de_precios(conn, "aove", "jaen", "2025/2026")
    filas = conn.execute(
        "SELECT COUNT(*) AS n FROM precio_aceite WHERE mercado = 'jaen'"
    ).fetchone()["n"]
    assert filas > 0
    # Repetir la consulta no duplica filas.
    precios.semanas_de_precios(conn, "aove", "jaen", "2025/2026")
    assert conn.execute(
        "SELECT COUNT(*) AS n FROM precio_aceite WHERE mercado = 'jaen'"
    ).fetchone()["n"] == filas


def test_si_la_api_falla_se_sirve_la_cache_indicando_su_antiguedad(
    monkeypatch, registros_brutos, conn
):
    monkeypatch.setattr(
        precios, "_consultar_en_la_api",
        lambda **f: _filtrar_como_la_api(registros_brutos, f),
    )
    en_vivo = precios.semanas_de_precios(conn, "virgen", "jaen", "2025/2026")
    assert en_vivo["desactualizado"] is False

    def caida(**filtros):
        raise precios.PrecioNoDisponible("sin red")

    monkeypatch.setattr(precios, "_consultar_en_la_api", caida)
    conn.execute("UPDATE precio_aceite SET consultado_en = '2000-01-01 00:00:00'")
    conn.commit()
    de_cache = precios.semanas_de_precios(conn, "virgen", "jaen", "2025/2026")
    assert de_cache["desactualizado"] is True
    assert de_cache["antiguedad_horas"] > 24 * 365
    assert de_cache["semanas"] == en_vivo["semanas"]


def test_la_ultima_semana_tambien_cae_en_la_cache(monkeypatch, registros_brutos, conn):
    monkeypatch.setattr(
        precios, "_consultar_en_la_api",
        lambda **f: _filtrar_como_la_api(registros_brutos, f),
    )
    en_vivo = precios.ultima_semana(conn, "media-nacional")

    def caida(**filtros):
        raise precios.PrecioNoDisponible("sin red")

    monkeypatch.setattr(precios, "_consultar_en_la_api", caida)
    de_cache = precios.ultima_semana(conn, "media-nacional")
    assert de_cache["desactualizado"] is True
    assert de_cache["semana"] == en_vivo["semana"]
    assert de_cache["precios"] == en_vivo["precios"]


def test_sin_api_ni_cache_la_ruta_responde_503_con_mensaje_claro(api_caida):
    with TestClient(app) as client:
        cabeceras = _entrar(client)
        respuesta = client.get(
            "/api/v1/precios-aceite", params={"campana": "2025/2026"}, headers=cabeceras
        )
        ultima = client.get("/api/v1/precios-aceite/ultima-semana", headers=cabeceras)
    for r in (respuesta, ultima):
        assert r.status_code == 503
        assert "escribe el precio a mano" in r.json()["detail"]


# --- Fallos de red de la llamada real (httpx simulado) --------------------

class _Respuesta:
    def __init__(self, codigo=200, cuerpo=None, ilegible=False):
        self.status_code = codigo
        self._cuerpo = cuerpo
        self._ilegible = ilegible

    def json(self):
        if self._ilegible:
            raise ValueError("no es JSON")
        return self._cuerpo


def test_timeout_de_la_api_se_convierte_en_precio_no_disponible(monkeypatch):
    import httpx

    def lenta(*args, **kwargs):
        raise httpx.ReadTimeout("tarda demasiado")

    monkeypatch.setattr(precios.httpx, "get", lenta)
    with pytest.raises(precios.PrecioNoDisponible):
        precios._consultar_en_la_api(marketingYears="2025/2026")


@pytest.mark.parametrize("respuesta", [
    _Respuesta(codigo=500), _Respuesta(ilegible=True), _Respuesta(cuerpo={"error": 1}),
])
def test_respuestas_erroneas_de_la_api_son_precio_no_disponible(monkeypatch, respuesta):
    monkeypatch.setattr(precios.httpx, "get", lambda *a, **k: respuesta)
    with pytest.raises(precios.PrecioNoDisponible):
        precios._consultar_en_la_api(marketingYears="2025/2026")


def test_la_llamada_real_lleva_espana_timeout_y_filtros(monkeypatch):
    recibido = {}

    def espia(url, params, timeout):
        recibido.update(url=url, params=params, timeout=timeout)
        return _Respuesta(cuerpo=[])

    monkeypatch.setattr(precios.httpx, "get", espia)
    assert precios._consultar_en_la_api(marketingYears="2025/2026") == []
    assert recibido["url"] == "https://api.tech.ec.europa.eu/agrifood/api/oliveOil/prices"
    assert recibido["params"] == {"memberStateCodes": "ES", "marketingYears": "2025/2026"}
    assert 0 < recibido["timeout"] <= 15


def test_se_descartan_los_registros_que_no_son_objetos_o_sin_semana():
    """La respuesta de la fuente de precios no es de fiar."""
    bueno = {
        "product": "Extra virgin olive oil (up to 0.8%)",
        "market": "Average national price",
        "beginDate": "05/10/2026",
        "endDate": "11/10/2026",
        "weekNumber": 41,
        "marketingYear": "2026/2027",
        "price": "€298.00",
    }
    registros = precios.normalizar_registros(
        [bueno, "basura", None, 3, dict(bueno, weekNumber=None)]
    )

    assert len(registros) == 1
    assert registros[0]["numero_semana"] == 41


def test_un_fallo_de_la_cache_no_tumba_la_respuesta(api_falsa, conn, monkeypatch):
    """Un fallo al guardar la caché no puede tumbar los precios buenos."""

    def caida(*args, **kwargs):
        raise sqlite3.OperationalError("database is locked")

    monkeypatch.setattr(precios, "_asegurar_tabla", caida)
    resultado = precios.semanas_de_precios(conn, "aove", "jaen", "2025/2026")

    assert resultado["desactualizado"] is False
    assert resultado["semanas"]
