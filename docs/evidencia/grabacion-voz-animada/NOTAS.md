# grabacion-voz-animada — NOTAS

## Estado
`in_progress` en la pista: implementada y verificada con build + tests unitarios + lint. Falta la
verificación en dispositivo (la hace la sesión de integración; no se ha probado con micrófono real).

## Qué se hizo
- `voz/domain/GrabacionAnimada.kt` (lógica pura): `normalizarAmplitud` (0..32767 → 0..1 en escala de
  decibelios, silencio por debajo de −45 dB), `agregarNivelDeOnda`/`ondaEnSilencio` (ventana de 24
  muestras), `segundosRestantes` y `fraccionRestante` (anillo).
- `GrabadorDeNotas.amplitud(): Int` con implementación por defecto `0` y, en `GrabadorNotas`,
  `MediaRecorder.maxAmplitude` (con try/catch). Es el cambio mínimo: `empezar`/`parar` no se tocan.
- `NotasVozViewModel`: un medidor (`INTERVALO_AMPLITUD_MS` = 80 ms) llena `nivelesOnda` mientras se
  graba; se cancela al parar, al salir de la pantalla y al llegar al límite. No toca cola, Room,
  transcripción, sincronizador ni borrador IA.
- `voz/presentation/GrabacionAnimada.kt` (`ControlDeGrabacion`): anillo de cuenta atrás de 60 s
  alrededor del botón Grabar/Parar (se vacía de forma continua), onda de barras con la amplitud real,
  texto "Grabando… quedan N de 60 segundos" (el contador va siempre en texto) y vibración al empezar y
  al terminar con `LocalHapticFeedback` (`LongPress`), sin permiso VIBRATE. No vibra al abrir la
  pantalla ni al rotar.
- **Animaciones reducidas**: si `Settings.Global.ANIMATOR_DURATION_SCALE == 0`, no hay onda; se muestra
  un indicador estático "● Grabando", el contador en texto y el anillo a saltos de un segundo.

## Verificación ejecutada
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint` → BUILD SUCCESSFUL (la batería
  completa en verde, sin tocar backend, así que no se ejecutó pytest).
- Tests nuevos: `voz/domain/GrabacionAnimadaTest` (10: normalización, onda, cuenta atrás, anillo) y 2 en
  `NotasVozViewModelTest` (la onda sigue la amplitud mientras se graba; vuelve al silencio al parar).
- Fake: `FakeGrabadorDeNotas.amplitudActual`.

## Tests cambiados y por qué
- Ninguno de los existentes cambió sus aserciones.

## Decisiones tomadas sin consultar
- Escala logarítmica con suelo en −45 dB para que la voz normal se vea (la lineal casi no se movería).
- Las animaciones reducidas se detectan con `ANIMATOR_DURATION_SCALE` (no hay otra señal estándar
  en Android para "reduce motion").
- La vibración usa `LongPress` en ambos momentos (es el único tipo con vibración fiable en todas las
  versiones de Compose del proyecto).

## Pendiente / riesgos
- Verificar en el Xiaomi: la onda se mueve al hablar, el anillo llega a 0 a los 60 s, vibra al empezar y
  al parar, con fuente grande y en horizontal (la pantalla hace scroll), y con la escala de animaciones a 0.
- `getMaxAmplitude` devuelve 0 en la primera consulta tras `start()`: es normal.
- `BorradorIA.kt` ("repetir dictado") usa su propio botón de grabar y no tiene la onda; no era de esta
  feature.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- feature_list.json: `grabacion-voz-animada` → passing tras verificar en dispositivo; evidencia
  `docs/evidencia/grabacion-voz-animada/NOTAS.md`.
- docs/technical-discovery.md: "La amplitud de la grabación se lee con `MediaRecorder.getMaxAmplitude`
  cada 80 ms y se normaliza en decibelios para la onda."
