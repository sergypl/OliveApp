# ADR 0012: APK ligero sin R8 — fuentes podadas al subconjunto latino y recursos solo en español

- Estado: aceptada (2026-10-15) como parte del encargo del usuario "tener una versión lo
  más optimizada a nivel de espacio, compilada, pero sin pasar el R8 ni firmarla".
  Decisión tomada sin consultar y reversible (los TTF originales están en el historial de
  git); R8 y firma quedan fuera por decisión expresa del usuario.
- Contexto: el APK release sin firma pesaba 9,46 MB. El reparto decía dónde se podía
  recortar sin R8: **22 MB de DEX sin podar (90 % del paquete descomprimido, que solo R8
  puede recortar)**, 1,66 MB de fuentes TTF y 0,41 MB de `resources.arsc` con las
  traducciones de las librerías. El resto ya estaba optimizado. Regla del proyecto: nada
  de dependencias nuevas sin ADR, y los cambios de diseño se registran.

## Decisión

- **Podar los cuatro TTF** de `android/app/src/main/res/font/` (Montserrat SemiBold/Bold e
  Inter Regular/SemiBold, ADR 0009) al subconjunto **latin + latin-ext** de Google Fonts,
  conservando el *hinting* y las *features* de composición (`kern, liga, calt, ccmp, locl,
  rlig, clig`). Pasan de 1.740.964 a 508.888 bytes (−70 %). Se descartan las *features*
  tipográficas que la app no usa (variantes, versalitas…), que pesaban más que las letras.
- **Compilar solo los recursos en español**: `resourceConfigurations += listOf("es")` en
  `app/build.gradle.kts`. La app es solo en español y no tiene `strings.xml` (los textos
  visibles van en el código Kotlin), así que esto solo descarta las traducciones de las
  librerías: `resources.arsc` pasa de 425.316 a 35.820 bytes.
- **R8 y firma siguen fuera** (`isMinifyEnabled=false`, sin `signingConfig`), como decidió
  el usuario. Se midió el ahorro que aportaría R8 para dejarlo como dato: **2,04 MB**
  (frente a los 8,53 MB actuales), compilable con una única regla
  (`-dontwarn com.google.errorprone.annotations.**`) más un smoke test.

## Cómo se reproduce

- Poda de fuentes con `fonttools` (`pip install fonttools`), para cada TTF:

  ```sh
  pyftsubset <fuente>.ttf \
    --unicodes="U+0000-00FF,U+0100-024F,U+0259,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+1E00-1EFF,U+2000-206F,U+2074,U+20A0-20C0,U+2113,U+2122,U+2191,U+2193,U+2212,U+2215,U+2C60-2C7F,U+A720-A7FF,U+FEFF,U+FFFD" \
    --layout-features='kern,liga,calt,ccmp,locl,rlig,clig' \
    --notdef-glyph --name-IDs='*' --output-file=<salida>.ttf
  ```

- El entorno y la medición antes/después quedan en
  `docs/evidencia/apk-ligero-sin-r8/NOTAS.md`.

## Consecuencias

- APK release sin firmar: 9,46 → **8,53 MB (−9,9 %)**; el debug que se instala cada día:
  16,44 → **12,28 MB** (de los que 3,78 MB eran huecos muertos del empaquetado incremental
  de AGP: para reempaquetar limpio hay que borrar el APK antes de volver a construir).
- La tipografía se ve idéntica (verificado en el Xiaomi con panel, simulador y un cálculo
  real). Todos los glifos del español siguen presentes (tildes, eñe, ¿¡, €, −, …, ·, « »);
  un glifo fuera del subconjunto cae a la fuente del sistema por *fallback*, no a un hueco.
  Si aparece un idioma de Europa oriental, cirílico o griego en datos de usuario, esa letra
  se verá en la fuente del sistema.
- Si la app se traduce algún día, hay que ampliar el filtro de locales (o quitarlo) y podar
  las fuentes a los alfabetos nuevos.
- Si se añade un peso de fuente nuevo (ADR 0009), hay que podarlo igual y registrarlo aquí.
- Queda fuera de esta decisión la palanca grande (R8: 2,04 MB) y la firma del APK, ambas
  pendientes en `docs/risks-and-open-questions.md`.
