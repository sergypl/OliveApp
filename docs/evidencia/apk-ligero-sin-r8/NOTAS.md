# Evidencia — apk-ligero-sin-r8

Encargo del usuario (2026-10-15): "una versión lo más optimizada a nivel de espacio,
compilada, pero **sin pasar el R8 ni firmarla**", manteniendo el debug como build de
cada día. Aquí queda la medición antes/después y la comprobación en el dispositivo.

## Tamaños medidos (APK de `android/app/build/outputs/apk/`)

| APK | Antes | Después | Ahorro |
|---|---|---|---|
| `release/app-release-unsigned.apk` (el entregable) | 9.458.011 B (9,46 MB) | **8.525.975 B (8,53 MB)** | −9,9 % |
| `debug/app-debug.apk` (el que se instala cada día) | 16.439.654 B (16,44 MB) | **12.277.779 B (12,28 MB)** | −25,3 % |
| `release` con R8 (medido, **no entregado**) | — | 2.037.640 B (2,04 MB) | dato para decidir |

Reparto del release antes de optimizar (24,3 MB descomprimidos): **22 MB de DEX sin podar
(90 %, que solo R8 puede recortar)**, 1,66 MB de fuentes, 0,41 MB de `resources.arsc`.

## Qué se hizo (sin R8 y sin firma)

1. **Fuentes podadas al subconjunto latino** (ADR 0012): los 4 TTF de
   `android/app/src/main/res/font/` pasan de 1.740.964 a 508.888 bytes (−70 %),
   conservando *hinting* y las *features* de composición. En el APK: −506 KB.
2. **Recursos solo en español**: `resourceConfigurations += listOf("es")` en
   `app/build.gradle.kts`. La app no tiene `strings.xml` (los textos van en el código
   Kotlin), así que solo se descartan las traducciones de las librerías:
   `resources.arsc` pasa de 425.316 a 35.820 bytes (−389 KB).
3. **Reempaquetado limpio del debug**: el APK debug arrastraba 3.776.371 bytes de huecos
   muertos del empaquetado incremental de AGP (zipflinger actualiza el zip en sitio y no
   recupera el hueco que dejan las entradas que encogen). Por eso el APK debug medía
   **exactamente lo mismo** antes y después de podar las fuentes: el espacio liberado
   quedaba como hueco. Borrar el APK y volver a empaquetar (`rm app-debug.apk` +
   `assembleDebug`) lo deja compacto (0 huecos, verificado).

Con `isMinifyEnabled=false`, el release y el debug quedan con **0 huecos internos**
(comprobado recorriendo los offsets locales del zip).

## Comprobación tipográfica en el Xiaomi Redmi Note 10 Pro (M2101K6G)

Instalado el `app-debug.apk` optimizado sobre la app ya instalada, con el backend de
desarrollo y `adb reverse tcp:8000 tcp:8000`:

- `xiaomi-panel-tipografia.png`: panel con la sesión iniciada (Socio 1, Campaña
  2025/2026). Montserrat en "Buenos días, Socio 1" y "Campaña 2025/2026", Inter en el
  cuerpo: idéntico a antes de la poda. Tildes ("días") y eñe ("Campaña") correctas.
- `xiaomi-simulador-tipografia.png`: "Liquidación en limpio", signo menos "−0,1",
  "+0,1", comas decimales y "%" correctos.
- `xiaomi-simulador-resultado.png`: cálculo real tecleando 10.000 kg x 18 % (1.620 kg
  netos, "Ajuste industrial (10 %)", "-180 kg"): cifras y unidades correctas.

`logcat -d` sin `FATAL EXCEPTION` ni errores de `AndroidRuntime` tras el recorrido.

## Comprobación de glifos (programática)

Sobre los TTF podados, con `fontTools`: 856 glifos en cada Montserrat y 904 en cada
Inter, y **no falta ninguno** de los probados: `á é í ó ú ü ñ Ñ ¿ ¡ € − … · « » º ª ™ — °`.
Los glifos fuera del subconjunto (p. ej. cirílico) caerían a la fuente del sistema por
*fallback*, no a un hueco.

## Cómo se reproduce la poda de las fuentes

`fonttools` en un entorno virtual (`pip install fonttools`) y, para cada TTF de
`android/app/src/main/res/font/`:

```sh
pyftsubset <fuente>.ttf \
  --unicodes="U+0000-00FF,U+0100-024F,U+0259,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+1E00-1EFF,U+2000-206F,U+2074,U+20A0-20C0,U+2113,U+2122,U+2191,U+2193,U+2212,U+2215,U+2C60-2C7F,U+A720-A7FF,U+FEFF,U+FFFD" \
  --layout-features='kern,liga,calt,ccmp,locl,rlig,clig' \
  --notdef-glyph --name-IDs='*' --output-file=<salida>.ttf
```

Es el subconjunto **latin + latin-ext** de Google Fonts (cualquier texto de España y el
resto de Europa occidental). Se conserva el *hinting* a propósito: la legibilidad sénior
prima sobre los ~60 KB extra por fuente que costaría quitarlo.

## Verificación ejecutada

- `cd android && ./gradlew testDebugUnitTest lint` con el puerto 8000 libre: **285 tests,
  0 fallos, 0 omitidos** y lint sin errores.
- Instalación sobre la app existente en el Xiaomi (la base de datos y las sesiones
  siguen intactas) y recorrido de panel y simulador sin crashes.

## R8: medido y dejado fuera (decisión del usuario)

Se midió una compilación de prueba con `isMinifyEnabled=true` +
`proguard-android-optimize.txt` (revirtiendo el cambio después): **2.037.640 bytes
(2,04 MB)**, es decir 4,2 veces más ligero que los 8,53 MB actuales. R8 no arranca sin
reglas: falla por las anotaciones `com.google.errorprone.annotations.*` que usa Tink
(dependencia de `security-crypto`); la regla que lo resuelve es
`-dontwarn com.google.errorprone.annotations.**`. Queda registrado en
`docs/risks-and-open-questions.md` como la palanca grande pendiente.
