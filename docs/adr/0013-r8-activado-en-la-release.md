# ADR 0013: R8 activado en la release (minify y ofuscación)

- Estado: aceptada (2026-10-15), decidida por el usuario tras medir el ahorro real
  (la release pasa de 8,53 MB a 2,04 MB). Sustituye parcialmente al ADR 0012, que dejó
  R8 fuera "de momento".
- Contexto: sin R8, el 90 % del APK era DEX sin podar y ya no quedaba nada por recortar
  (las fuentes y los recursos están resueltos en el ADR 0012). R8 poda el código que no se
  usa y ofusca los nombres, y es la única palanca de tamaño que quedaba. Riesgo: quitar
  algo que se usa por reflexión (sesión cifrada con Tink, Room, Hilt).

## Decisión

- `isMinifyEnabled = true` en el build `release`, con
  `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
  El build de desarrollo no cambia.
- `android/app/proguard-rules.pro` se queda en lo mínimo:
  - `-dontwarn com.google.errorprone.annotations.**` (obligatorio: sin esta regla R8 no
    compila, por las anotaciones de errorprone que usa Tink vía `security-crypto`).
  - `-keepattributes SourceFile,LineNumberTable` y `-renamesourcefileattribute SourceFile`
    para que los crashes sigan indicando fichero y línea.
  - Nada más: Hilt, Room y OkHttp aportan sus propias reglas de consumidor.
- Los ficheros de trazabilidad de R8 (`mapping.txt`, `usage.txt`) quedan en
  `android/app/build/outputs/mapping/release/` y hacen falta para leer los stack traces
  ofuscados de una release.

## Consecuencias

- `release/app-release-unsigned.apk`: **2.037.640 bytes (2,04 MB)**, 4,2 veces más ligero
  que sin R8. Smoke test en el Xiaomi en verde (sesión cifrada intacta, cuaderno con
  datos, cálculo del simulador idéntico), ver `docs/evidencia/r8-minify/`.
- Los nombres de clases y métodos aparecen ofuscados en los stack traces: hay que
  conservar el `mapping.txt` de cada release para poder leerlos.
- Si se añade una librería con reflexión y sin reglas de consumidor, puede romperse solo
  en la release: por eso el smoke test de release es parte de la verificación y no basta
  con los tests del build de desarrollo.
- Sigue pendiente la firma del APK release (sin firma no se puede instalar en un móvil);
  el smoke test usó una copia temporal firmada con la clave de depuración, fuera del repo.
