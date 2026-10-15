# ADR 0011: Precios de referencia del aceite vía backend (Comisión Europea, con proxy y caché)

- Estado: aceptada (2026-10-15, decidida sin consulta en la sesión nocturna de
  `simulador-avanzado-precio-aceite`; la regla del proyecto es "ninguna dependencia
  nueva sin ADR").
- Contexto: el simulador avanzado (`CONTEXT.md`) necesita un **precio del aceite**
  de referencia en origen a granel, en €/kg de aceite, para estimar un **importe
  estimado**. Hace falta una fuente pública, sin clave y con historial por semanas,
  categorías (AOVE, virgen, lampante) y mercados españoles.

## Decisión

### Fuente: Agri-food Data Portal de la Comisión Europea

API pública y sin clave, verificada el 14/10/2026:

- Base: `https://api.tech.ec.europa.eu/agrifood/api/oliveOil/`
- `GET prices?memberStateCodes=ES&products=…&markets=…&marketingYears=2024/2025&beginDate=dd/MM/yyyy&endDate=dd/MM/yyyy`
  (todos los filtros son opcionales). `GET products` y `GET markets` devuelven los
  catálogos.
- Campos: `memberStateCode, beginDate, endDate, weekNumber, marketingYear, product,
  market, price ("€445.00"), unit ("€/100kg")`. **El precio llega en €/100 kg**: se
  quita el "€" y se **divide entre 100** para obtener €/kg.
- Historial desde 2010/2011, con una o dos semanas de retraso.
- Las semanas de cambio de campaña (septiembre y octubre) salen **duplicadas** con
  dos `marketingYear`: se deduplica por (semana, producto, mercado), dejando la
  campaña más reciente para que las tres categorías de una semana coincidan.
- La campaña de la fuente es el año de comercialización europeo (1 oct – 30 sep) y
  **no coincide** con la campaña del socio (1 nov – 31 oct, `docs/domain-model.md`).
  El simulador la llama "campaña" solo para elegir el historial de precios.

Mapeo de productos: `Extra virgin olive oil (up to 0.8%)` → AOVE,
`Virgin olive oil (up to 2%)` → virgen, `Lampante olive oil (2%)` → lampante. Se
ignoran los refinados y los de orujo. Mercados del MVP: `Average national price`
(media nacional), Jaén (ES616), Córdoba (ES613), Granada (ES614), Sevilla (ES618),
`Málaga(ES617)` (sin espacio, tal cual), Badajoz (ES431), Toledo (ES425) y
Tarragona (ES514).

### Arquitectura: la app nunca llama a la API europea

El backend (módulo `backend/app/precios.py`, registrado en `main.py` con una línea)
hace de proxy:

- `GET /api/v1/precios-aceite?categoria=&mercado=&campana=` → semanas de una campaña
  y un mercado, cada una con las tres categorías en €/kg (la `categoria` fija el
  campo `precio` de cada semana; el reparto por calidad usa las tres a la vez).
- `GET /api/v1/precios-aceite/ultima-semana?mercado=` → la última semana que tiene
  las tres categorías.
- Ambas exigen sesión (como el resto de la API), parsean, mapean, deduplican y
  **cachean en SQLite**, en una tabla nueva (`precio_aceite`) que no toca el
  esquema existente. Timeout de 10 s hacia la fuente.
- **Si la fuente falla o tarda**, se sirve la caché con `desactualizado: true` y su
  `antiguedad_horas`; sin caché responde 503 con un mensaje claro. **Nunca se
  inventa un precio.** La app lo dice con claridad y la entrada manual del precio
  sigue funcionando.
- `httpx` ya estaba en `requirements.txt` (lo usa el gateway de IA); no se añade
  ninguna dependencia al backend.

Por qué proxy y no llamada directa desde la app: una sola caché compartida (la
fuente tiene semanas de retraso y no cambia por horas), un único sitio donde
mapear y deduplicar, tests del backend sin red con respuestas grabadas, y si la
fuente cambia de formato se arregla sin publicar un APK nuevo.

### Android

- Nueva dependencia **`androidx.datastore:datastore-preferences:1.1.1`** para la
  preferencia "Simulador avanzado" activado (una preferencia de usuario, no un dato
  de campaña). La lectura inicial es un booleano que se deja en memoria; la
  escritura va en segundo plano.
- Repositorio en `simulador/data` (OkHttp contra el backend, ya presente en el
  proyecto) y funciones puras de importe en `simulador/domain` con tests.

## Alternativas descartadas

- **Poolred (Fundación del Olivar):** no tiene API pública documentada.
- **Boletín semanal del MAPA (Ministerio):** solo se publica en PDF.
- **Llamar a la API europea directamente desde la app:** sin caché compartida, sin
  mapeo central y atada a cada versión del APK.
- **Precio fijo escrito en el código:** se queda viejo y sería un dato inventado.
- **SharedPreferences en lugar de DataStore:** válido, pero el proyecto pedía
  DataStore para preferencias nuevas y el coste (una dependencia pequeña) es bajo.

## Consecuencias

- Hay una **dependencia externa** nueva (la API de la Comisión): puede cambiar de
  formato, tener retraso o caerse; la caché y la entrada manual lo amortiguan.
- El precio es **de referencia** y distinto del que paga la cooperativa; la app lo
  avisa siempre ("Precio de referencia en origen a granel. No es lo que paga tu
  cooperativa.") con la fuente y la semana usada.
- Ideas sin implementar: compensación REAGP y retención de IRPF agrícola en el
  importe neto.
