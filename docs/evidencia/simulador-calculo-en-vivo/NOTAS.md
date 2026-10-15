# simulador-calculo-en-vivo — NOTAS

## Estado
Terminada en la pista: comportamiento implementado y verificado con la verificación de pista de
`comun.md` en verde. Quedan las comprobaciones en el dispositivo para la sesión de integración
(ver "Pendiente / riesgos").

## Qué se hizo
- **Cálculo en vivo, sin botón "Simular"**: `SimuladorViewModel` recalcula la simulación en cada
  cambio de kilos, rendimiento o ajuste (`calcular()`, que usa `simularLiquidacion`). Se mantiene
  `recalcularConAjuste` para el cambio de ajuste con resultado ya calculado (solo cambia la
  deducción) y `simularLiquidacion` para el cálculo completo, tal como pedía la feature.
- **Estado neutro, sin errores por faltar datos**: con kilos o rendimiento vacíos o ilegibles se
  muestra "Escribe los kilos y el rendimiento" y no hay error. Los valores **fuera de rango**
  siguen avisando con los mensajes de siempre (kilos ≤ 0, rendimiento fuera de 0–100, ajuste
  fuera de 0–100). El ajuste vacío o ilegible usa el 10 % por defecto (ADR 0007).
- **Unidades dentro de los campos** ("kg" en kilos, "%" en rendimiento y ajuste): parámetro nuevo
  `sufijo` de `CampoDeTexto` (`core/ui/Campos.kt`). Teclado numérico (`KeyboardType.Decimal`).
- **Miles formateados al teclear**: `formatearNumeroDeCampo` en `simulador/domain/Simulacion.kt`
  agrupa los millares con punto ("1.000") y deja la coma decimal ("1.000,5"); lo ilegible (letras)
  se deja tal cual, sin formatear ni dar error. El texto formateado se vuelve a leer con
  `numeroDeCampo` sin perder valor (tests de ida y vuelta). `numeroDeCampo` acepta ahora punto de
  millar con **tres o más** cifras detrás ("1.0000" → 10.000) para que seguir tecleando detrás de
  un millar sume cifras al número en vez de romper el decimal.
- **Rendimiento graso sincronizado en tres controles**: campo de teclado, botones "−0,1" y "+0,1"
  (`BotonDePaso`, 64dp de alto y mínimo de ancho, con texto visible y `contentDescription` de
  "Subir/Bajar el rendimiento graso 0,1 por ciento") y deslizador de 10 a 30 % con paso de 0,1
  (`Slider` con `steps = 199`). Los tres escriben el mismo texto del campo.
- **Cifra animada al cambiar** (180 → 195 kg): componente nuevo `CifraAnimada` en `core/ui`
  (`animateFloatAsState`, 350 ms, con `formatearCantidad` y la unidad dentro del texto).
- **Tarjeta de resultado siempre visible**: hueco nuevo `fijo` de `PantallaConBarraSuperiorGrande`
  (`core/ui/BarraSuperior.kt`), fuera del scroll, donde va la tarjeta con la cifra animada, el
  aviso "Estimación orientativa: la liquidación oficial es la de la cooperativa." y el desglose
  (aceite según el rendimiento y ajuste industrial). Con campos vacíos la tarjeta se queda visible
  con el estado neutro en lugar de la cifra.
- **Barra superior grande** (componente de oleada 0) con el título "Liquidación en limpio" y botón
  de volver. Además, el hueco de la futura barra inferior se retira con el teclado abierto, igual
  que hace `Modifier.pantallaCompleta()`.

## Verificación ejecutada (comandos y resultado)
- `cd backend && ./.venv/bin/python -m pytest -q` — verde (sin cambios de backend en esta feature).
- `cd android && ./gradlew testDebugUnitTest` — **32 tests del simulador en verde**
  (`SimuladorCaracterizacionTest` 8, `SimulacionTest` 8, `SimuladorViewModelTest` 16). Salen
  11 fallos **de `ServicioCuentasCaracterizacionTest`, que no toca esta pista**: se comprobó
  repitiendo la suite con los cambios guardados en `git stash` (HEAD limpio) y fallan igual. Motivo:
  el backend de desarrollo ocupa el 127.0.0.1:8000 (proceso Python escuchando) y el servidor de
  pruebas de esa caracterización no captura el puerto (escucha en IPv6, el cliente va a IPv4), así
  que las respuestas llegan del backend real en vez del simulado.
- `cd android && ./gradlew assembleDebug lint` — `BUILD SUCCESSFUL`, lint sin incidencias.

## Tests cambiados y por qué
En `SimuladorViewModelTest` (el botón "Simular" ya no existe, así que los tests escriben los campos
y miran el estado). `SimuladorCaracterizacionTest` no se toca (solo dominio):
- `simular calcula el aceite neto` → `escribir kilos y rendimiento calcula el aceite neto al
  instante`: se quita la llamada a `simular()`; las aserciones de valores son las mismas.
- `los campos aceptan coma decimal y puntos de millar`: se quita `simular()`; misma aserción.
- `simular sin kilos mayores que cero avisa` → `sin kilos se muestra el estado neutro, sin error`:
  el campo vacío deja de ser error (comportamiento nuevo de la feature). Se añade `kilos en cero
  avisa`, que conserva el mensaje de error de siempre para kilos ≤ 0.
- `cambiar kilos o rendimiento borra el resultado` → `cambiar kilos o rendimiento recalcula el
  resultado al instante`: ya no se borra, se recalcula (comportamiento nuevo).
- `sin resultado, cambiar el ajuste no calcula nada` → `sin datos tecleados cambiar el ajuste no
  calcula nada`: teclear kilos y rendimiento ya calcula, así que el caso sin datos es el que no
  calcula.
- `un ajuste ilegible no recalcula ni borra el resultado` → `un ajuste ilegible deja el resultado
  con el 10 % por defecto`: se conservan las dos aserciones originales (el campo queda en "abc" y
  el neto en 180 kg) y se añade `ajustePorcentaje == 10`, que documenta la regla nueva.
- Nuevos tests: formateo de millares al teclear, botones de paso de 0,1 con sus límites y
  deslizador sincronizado.
- Nuevo `simulador/domain/SimulacionTest` para `formatearNumeroDeCampo` (agrupación, coma decimal,
  punto decimal tecleado, separador recién tecleado, ceros a la izquierda, ilegibles y ida y vuelta
  con `numeroDeCampo`).

## Decisiones tomadas sin consultar
1. El estado neutro es siempre "Escribe los kilos y el rendimiento" (el texto literal de la
   feature) cuando kilos o rendimiento faltan o son ilegibles, en vez de un mensaje por campo.
2. Los valores fuera de rango siguen siendo error (mensaje rojo) con los textos de siempre: la
   feature solo pide neutralidad para campos vacíos o ilegibles.
3. El ajuste industrial vacío o ilegible se calcula con el 10 % por defecto sin aviso (es un campo
   con valor por defecto); fuera de rango avisa.
4. Los botones de paso operan en el rango del deslizador (10 a 30 %); el teclado sigue aceptando
   0–100 como hasta ahora. Si el valor tecleado está fuera de 10–30, el deslizador queda en el
   extremo y el primer toque de un botón lo lleva al extremo más cercano.
5. Punto con una o dos cifras detrás = decimal (se escribe con coma); con tres o más cifras =
   millar. Así, seguir tecleando detrás de "1.000" da "10.000". Es la misma regla de lectura que
   `numeroDeCampo`, salvo que ahí el umbral pasa a ser "tres o más".
6. La tarjeta de resultado se fija con un hueco `fijo` nuevo y opcional en
   `PantallaConBarraSuperiorGrande` (cambio aditivo: las demás pantallas no cambian).
7. Los botones de paso llevan texto visible "−0,1"/"+0,1" (nada de iconos sin texto) y
   `contentDescription` para TalkBack.
8. Quien llame a `PantallaConBarraSuperiorGrande` necesita `@OptIn(ExperimentalMaterial3Api::class)`
   por el tipo `TopAppBarScrollBehavior` de su firma (así está `PantallaSimulador`).

## Pendiente / riesgos
- **Integración (dispositivo Xiaomi 2cf893c0)**: comprobar que el resultado se recalcula al teclear,
  que con campos vacíos se ve el estado neutro, que la tarjeta de resultado no se va de pantalla con
  el teclado abierto, que los botones −0,1/+0,1 tienen zona táctil grande y están sincronizados con
  el deslizador y el teclado, la animación de la cifra, el comportamiento con fuente grande del
  sistema y en horizontal. También el formato de miles tecleando con el teclado del sistema.
- `ServicioCuentasCaracterizacionTest` (11 tests) falla en este entorno por el backend de desarrollo
  en el puerto 8000; no es de esta pista y falla igual en HEAD.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- `CONTEXT.md`, entrada **Simulación** (sustituir el texto actual, que habla de "cálculo bajo
  demanda"): "Cálculo en vivo de una liquidación en limpio en el simulador: se recalcula al teclear,
  sin botón, y no se guarda como dato de campaña ni compite con las cifras oficiales de la
  cooperativa. Con campos vacíos o ilegibles no hay error: se muestra el estado neutro
  («Escribe los kilos y el rendimiento»)."
- `feature_list.json`, feature `simulador-calculo-en-vivo`: estado `passing` cuando la integración
  dé el visto bueno al dispositivo; evidencia a añadir: "`cd android && ./gradlew assembleDebug
  testDebugUnitTest lint` en verde (32 tests del simulador; los 11 fallos de
  `ServicioCuentasCaracterizacionTest` son del backend de desarrollo en el puerto 8000 y fallan
  igual en HEAD)", "Tests unitarios de cálculo en vivo y de `numeroDeCampo`/`formatearNumeroDeCampo`
  (coma decimal y puntos de millar) sin depender del botón Simular" y, tras la comprobación en el
  dispositivo, las verificaciones de recálculo al teclear, estado neutro, tarjeta de resultado
  siempre visible y controles de rendimiento sincronizados.
- `PROGRESS.md`: "simulador-calculo-en-vivo implementada en la pista `simulador` (rama
  `feature/mod-simulador`): cálculo en vivo sin botón, estado neutro, unidades en los campos, miles
  formateados al teclear, rendimiento con botones/deslizador/teclado sincronizados, cifra animada y
  tarjeta de resultado fijada. Pendiente de comprobación en dispositivo en la integración."

## Reverificación al reanudar (2026-10-15)
Al reanudar la pista se repitió `./gradlew assembleDebug testDebugUnitTest lint` sobre la rama:
`BUILD SUCCESSFUL`, sin fallos de tests (los 11 de `ServicioCuentasCaracterizacionTest` citados
arriba no se reprodujeron: el puerto 8000 estaba libre) y lint sin incidencias.
