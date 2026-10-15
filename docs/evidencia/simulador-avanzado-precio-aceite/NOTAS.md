# simulador-avanzado-precio-aceite — NOTAS

## Estado
Terminada en la pista: backend, Android, tests, ADR y docs hechos, con la verificación de
pista de `comun.md` en verde. **No verificada en el dispositivo** (las pistas no usan adb):
la sesión de integración debe revisar la pantalla (ver "Pendiente / riesgos"). Para
`feature_list.json` la propuesta es `in_progress` hasta esa comprobación en el móvil y
`passing` después.

## Qué se hizo
### Backend (`backend/app/precios.py`, módulo nuevo; en `main.py` solo `import` + `include_router`)
- Proxy de la API del Agri-food Data Portal de la Comisión Europea (sin clave): parsea
  `"€445.00"` (€/100 kg), divide entre 100 → €/kg, mapea los tres productos y los nueve
  mercados del MVP (se ignoran refinados, orujo y mercados ajenos), deduplica por
  (semana, categoría, mercado) dejando la campaña más reciente.
- `GET /api/v1/precios-aceite?categoria=&mercado=&campana=` y
  `GET /api/v1/precios-aceite/ultima-semana?mercado=`; ambas con sesión. Parámetro desconocido →
  400; sin fuente ni caché → 503 con mensaje ("…escribe el precio a mano").
- Caché SQLite en la tabla **nueva** `precio_aceite` (clave: categoría, mercado, semana, campaña;
  `consultado_en`), creada con `CREATE TABLE IF NOT EXISTS` en la primera consulta; no toca
  el esquema existente. Si la fuente falla (timeout de 10 s, HTTP ≠ 200, JSON ilegible) se sirve
  la caché con `desactualizado: true` y `antiguedad_horas`.
- Se parte del trabajo sin commitear de la sesión anterior (`precios.py` y el fixture) y se
  corrigió: (1) `ultima_semana` recorría las semanas de la más antigua a la más nueva (bug: daba
  la primera semana completa en lugar de la última); (2) la caché guardaba la fila ya
  deduplicada, con lo que una categoría podía quedar con otra campaña y desaparecer al servir
  desde caché; ahora se guarda tal cual y se deduplica al servir.
- Fixture `backend/tests/fixtures/precios_api_bruto.json`: respuesta real de `GET prices`
  (memberStateCodes=ES, 1.827 registros, campañas 2024/2025 a 2026/2027) grabada en la sesión
  anterior; en esta sesión **no se ha vuelto a llamar a la API real**.

### Android
- `simulador/domain`: `PrecioAceite.kt` (categorías, mercados, semanas, interfaces del repositorio
  y de las preferencias, campañas disponibles), `ImporteAvanzado.kt` (reparto por calidad,
  precio ponderado, importe bruto/gastos/neto y €/kg de aceituna) y `FormatoImportes.kt`
  (formato español con `formatearNumero` de `core/formatos`).
- `simulador/data`: `PreciosAceiteRepositorioImpl` (OkHttp contra el backend, callbacks en el
  hilo principal como el resto de repositorios), `InterpretePrecios`,
  `PreferenciasSimuladorDataStore` y `ModuloSimulador` (Hilt).
- `simulador/presentation`: `SimuladorAvanzadoViewModel` (nuevo; el `SimuladorViewModel`
  básico **no cambia**) y `SeccionAvanzada.kt` (interruptor "Simulador avanzado", precio
  consultado o a mano, gastos, reparto por calidad e "Importe estimado"). `PantallaSimulador`
  solo añade el interruptor y la sección debajo de los campos básicos.
- Interruptor "Simulador avanzado": fila entera pulsable de 64 dp con texto; la preferencia se guarda
  en **DataStore** (dependencia nueva `androidx.datastore:datastore-preferences:1.1.1`, ADR 0011).
- Precio consultado: categoría, mercado (media nacional por defecto) y campaña en desplegables,
  atajo "Última semana disponible" y **lista** de semanas (8 y "Ver más semanas") con su precio
  al lado y la elegida marcada con borde y la etiqueta "Elegida". Al activar el avanzado se elige
  sola la última semana. Precio a mano: campo en €/kg con `numeroDeCampo` (coma decimal) y el
  reparto se oculta.
- Gastos 0,20 €/kg editables. Reparto por calidad: 100 % de la categoría elegida por defecto; si
  no suma 100, aviso en rojo y sin importe.
- Resultado: importe bruto estimado, gastos, importe neto estimado y neto por kilo de aceituna,
  más "Estimación orientativa…", el aviso "Precio de referencia en origen a granel. No es lo que
  paga tu cooperativa.", la fuente ("Comisión Europea · Agri-food Data Portal") y la semana y
  mercado del precio usado. Con precio a mano pone "Precio escrito por ti.".
- Si el backend o la fuente fallan se dice con claridad (con la antigüedad si es caché) y el
  precio a mano sigue funcionando.

### Documentación
ADR 0011, `docs/technical-discovery.md` (Integrations), `docs/domain-model.md` (conceptos y casos
borde).

## Verificación ejecutada (comandos y resultado)
- `cd backend && ./.venv/bin/python -m pytest -q` → **151 passed** (26 tests nuevos en
  `tests/test_precios_aceite.py`, sin red: parseo, €/kg, mapeo, filtros, deduplicación, rutas con
  sesión, 400/503, uso de la caché, antigüedad, timeout y respuestas erróneas de la fuente).
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint` → `BUILD SUCCESSFUL`, **177 tests,
  0 fallos** (nuevos: `ImporteAvanzadoTest` 12, `FormatoImportesTest` 2, `InterpretePreciosTest` 6,
  `SimuladorAvanzadoViewModelTest` 16), lint sin incidencias.
- Cálculo de ejemplo comprobado a mano (y codificado en `ImporteAvanzadoTest`): 12.000 kg al 20 %
  = 2.400 kg; ajuste industrial 10 % → **2.160 kg** de aceite neto. AOVE, media nacional, semana
  del 28/09/2026 (fixture real: 343,19 €/100 kg) = **3,4319 €/kg**. Importe bruto 2.160 × 3,4319 =
  **7.412,90 €**; gastos 2.160 × 0,20 = **432,00 €**; importe neto **6.980,90 €**; por kilo de
  aceituna 6.980,904 / 12.000 = **0,582 €/kg**. Con reparto 60/30/10 y los precios de esa semana
  (3,4319 / 3,1757 / 3,0229): precio ponderado **3,31414 €/kg**.

## Tests cambiados y por qué
Ninguno de los existentes: el simulador básico no cambia y el `SimuladorViewModel` conserva su
constructor, así que `SimuladorViewModelTest`, `SimuladorCaracterizacionTest` y `SimulacionTest`
siguen igual.

## Decisiones tomadas sin consultar
1. **Dos ViewModels en la pantalla**: la parte avanzada vive en `SimuladorAvanzadoViewModel` y la
   pantalla le pasa el aceite neto y los kilos del básico; así el básico y sus tests no se tocan.
2. **Las rutas exigen sesión** aunque el precio sea público: evita que el backend sea un proxy
   abierto a la API europea.
3. **Campaña de la fuente**: el selector usa el año de comercialización de la Comisión
   (1 oct – 30 sep) y ofrece la campaña nueva desde septiembre (las semanas de cambio ya salen con
   ella). No coincide con la campaña del socio (1 nov – 31 oct) y está dicho en el ADR.
4. **Precio 0 = sin precio**: no se calcula importe (se pide precio) en vez de enseñar un importe
   de 0 €. Los gastos mayores que el precio sí calculan, con neto negativo y aviso.
5. **Un cambio de categoría reinicia el reparto** al 100 % de la nueva categoría (pisa lo que el
   socio hubiera tecleado).
6. **La lista de semanas es una pila de filas con "Ver más semanas"** (8 de cada vez) y no una
   lista con scroll propio, para no anidar scrolls dentro de la pantalla.
7. **Al elegir otra campaña o mercado se elige sola la semana más reciente** de la lista (si no,
   quedaría sin precio y sin importe).
8. **Lectura de la preferencia en DataStore con `runBlocking` una vez** (un booleano, luego en
   memoria) para que el ViewModel arranque ya con el interruptor en su sitio; la escritura es
   asíncrona.
9. **La tarjeta de importe va dentro del scroll**, debajo de la parte avanzada; la tarjeta fijada
   arriba sigue siendo la del aceite neto básico (si no, con el teclado abierto no quedaría sitio).
10. ADR **0011**: siguiente número libre en esta rama. Si otra pista también creó un 0011, la
    integración debe renumerar uno de los dos.

## Pendiente / riesgos
- **Comprobar en el móvil** (integración): la sección avanzada con fuente grande del sistema y en
  horizontal (filas de semana con precio a la derecha, desplegables, teclado sobre los campos de
  reparto), el interruptor 64 dp, la animación al cambiar y que la preferencia sobrevive a cerrar la
  app. El backend en el móvil necesita red hacia la API europea; sin ella se ve el aviso y la caché.
- Los desplegables (`Desplegable`) son menús de Material: con 17 campañas el menú es largo pero
  desplaza; revisar que se maneja bien con el dedo.
- Dependencia externa (API de la Comisión): formato o disponibilidad pueden cambiar; mitigado con
  caché y precio a mano. Retraso de 1-2 semanas en los datos.
- La caché solo guarda lo que se ha consultado: en un backend recién desplegado y sin red no hay
  precios hasta la primera consulta con éxito.
- `ServicioCuentasCaracterizacionTest` puede fallar si el backend de desarrollo ocupa el puerto
  8000 (ver NOTAS de `simulador-calculo-en-vivo`); en esta sesión pasó sin fallos.
- Sin implementar (ideas): compensación REAGP y retención de IRPF agrícola; actualización
  periódica de la caché en segundo plano.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- **CONTEXT.md**: ya contiene Simulador avanzado, Precio del aceite, Categoría de aceite, Gastos de la
  cooperativa, Reparto por calidad e Importe estimado. Se propone añadir **Precio de referencia
  (semana)**: «Precio semanal de una categoría de aceite en un mercado español, de la Comisión Europea
  (en €/kg de aceite). Es orientativo y distinto de lo que paga la cooperativa». Y a «Campaña»: «La
  campaña de la fuente de precios (1 oct – 30 sep) no coincide con la del socio».
- **docs/risks-and-open-questions.md** (Risks): «**Dependencia de la API de precios de la Comisión
  Europea:** puede cambiar de formato, tener 1-2 semanas de retraso o caerse; mitigado con proxy y
  caché SQLite en el backend (ADR 0011) y con la entrada manual del precio. **Precio de referencia ≠
  precio de la cooperativa:** el importe estimado usa un precio de mercado en origen a granel, no lo
  que liquida la cooperativa; la app lo avisa siempre. **Ideas sin implementar:** compensación REAGP
  y retención de IRPF agrícola en el importe neto.» Y en Research Tasks: «Contrastar el importe
  estimado con una liquidación real de la cooperativa del socio».
- **feature_list.json** (`simulador-avanzado-precio-aceite`): estado `in_progress` con el motivo
  «falta verificar en el dispositivo (fuente grande, horizontal, preferencia persistente)»;
  evidencia: backend 151 tests, Android 177 tests, lint y assembleDebug verdes; ejemplo a mano en
  estas NOTAS; ADR 0011.
