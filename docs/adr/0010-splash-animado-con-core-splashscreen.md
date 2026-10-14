# ADR 0010: Splash animado con la API de splash (core-splashscreen)

- Estado: aceptada (2026-10-15, decidida sin consulta en la sesión nocturna de
  `fundamentos-visuales-modernos`; la regla del proyecto es "ninguna dependencia
  nueva sin ADR").
- Contexto: el splash ya existía (commit 1a37391) con los atributos de plataforma
  `android:windowSplashScreenBackground` y `android:windowSplashScreenAnimatedIcon`
  solo en `values-v31`, es decir, Android 12+. En API 26-30 no había splash de
  sistema y el logo no entraba animado. La feature pide animar la entrada del
  logo con la API de splash **sin alargar el arranque**.

## Decisión

- Añadir la dependencia oficial de AndroidX **`androidx.core:core-splashscreen:1.0.1`**
  (~20 KB, sin más transitivas nuevas) y usar su API de splash en todas las
  versiones soportadas (minSdk 26):
  - tema `Theme.OlivApp` con padre `Theme.SplashScreen`, `windowSplashScreenBackground`
    (fondo de marca), `windowSplashScreenAnimatedIcon` (el logo del `olivapp_logo`)
    y `postSplashScreenTheme` con el tema normal de la app;
  - `installSplashScreen()` en `MainActivity.onCreate`, antes de `super.onCreate`;
  - en API 31+, `android:windowSplashScreenAnimationDuration` a 400 ms para que la
    entrada del logo se vea pero sea corta.
- **Sin esperas artificiales**: el splash se retira al primer frame de la app
  (`setKeepOnScreenCondition` no se usa), de modo que el arranque no se alarga.

## Alternativas descartadas

- Dejar solo los atributos de plataforma: en API 26-30 no hay splash ni animación
  y habría que mantener dos caminos.
- Alargar el splash con un temporizador para "ver el logo": penaliza el arranque,
  que es justo lo que la feature prohíbe.
- Animar el logo dentro de la app al arrancar: habría un salto entre la ventana de
  splash y la primera pantalla, peor que la animación nativa.

## Consecuencias

- Una dependencia nueva de AndroidX, declarada aquí por la regla del proyecto.
- El splash se ve igual que antes (fondo de marca y logo) pero con la entrada del
  logo animada en todas las versiones soportadas y sin cambios de comportamiento
  en los flujos de la app.
