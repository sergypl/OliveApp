# Evidencia — r8-minify

El usuario decidió (2026-10-15) activar R8 en la release después de medir su ahorro:
**8,53 MB → 2,04 MB**. Aquí queda la compilación, el smoke test en el dispositivo y lo
que R8 exige para arrancar.

## Qué se cambió

- `android/app/build.gradle.kts`: `isMinifyEnabled = true` en `release` +
  `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
  "proguard-rules.pro")`. El build de desarrollo no se toca.
- `android/app/proguard-rules.pro` (nuevo), con lo único que hace falta:
  - `-dontwarn com.google.errorprone.annotations.**` — **sin esta regla R8 no llega a
    compilar**: Tink (dependencia de `androidx.security:security-crypto`, el almacén de
    sesión cifrado) referencia anotaciones de errorprone que no se empaquetan. El error
    real era `Missing class com.google.errorprone.annotations.CanIgnoreReturnValue`
    (y 52 contextos más).
  - `-keepattributes SourceFile,LineNumberTable` y `-renamesourcefileattribute SourceFile`
    para que los crashes de la release sigan siendo legibles (fichero y línea) aunque los
    nombres se ofusquen.
- Hilt, Room y OkHttp no necesitan reglas propias: traen sus reglas de consumidor.

## Medida

| APK | Sin R8 | Con R8 |
|---|---|---|
| `release/app-release-unsigned.apk` | 8.525.975 B (8,53 MB) | **2.037.640 B (2,04 MB)** |

R8 deja además sus ficheros de trazabilidad en `android/app/build/outputs/mapping/release/`
(`mapping.txt` para leer los stack traces ofuscados, `usage.txt` de lo podado).

## Smoke test en el Xiaomi Redmi Note 10 Pro (M2101K6G)

La release no se puede instalar sin firma (Android la exige), así que **para la prueba**
se firmó una copia temporal con la clave de depuración (`apksigner sign --ks
~/.android/debug.keystore ...`), fuera del repo y sin tocar la configuración del proyecto:
el entregable sigue siendo `app-release-unsigned.apk`. Al compartir firma con el
`app-debug.apk` ya instalado, la instalación fue sobre la misma app (datos y sesiones
conservados).

1. `xiaomi-r8-panel-sesion-intacta.png`: la release con R8 arranca y **conserva la sesión
   del socio** (Socio 1) sin pasar por el login. Es la comprobación del punto delicado:
   la sesión se guarda cifrada con Tink (`EncryptedSharedPreferences`), que es justo la
   dependencia que forzó la regla de `errorprone`. Carga también los datos reales del
   backend (10.958 kg, 17,93 %).
2. `xiaomi-r8-cuaderno.png`: "Mi cuaderno" con 9 labores de octubre de 2026, filtros por
   categoría y por parcela (Cañada Honda, La Noria…): OkHttp + parseo JSON + navegación
   funcionan ofuscados.
3. `xiaomi-r8-simulador.png`: cálculo real tecleando 10.000 kg x 18 % → 1.620 kg netos
   (1.800 kg − 180 kg de ajuste industrial): la lógica de dominio y el formato de cifras
   dan el mismo resultado que sin R8.

`logcat -d` en todo el recorrido sin `FATAL EXCEPTION`, sin `AndroidRuntime`, sin
`ClassNotFound`, `NoSuchMethod` ni `NoSuchField`. Room se toca al arrancar (la cola de
notas de voz se reanuda en `AplicacionOlivApp.onCreate()`), así que su funcionamiento bajo
R8 queda cubierto por los arranques sin crash.

## Verificación ejecutada

- `cd android && ./gradlew assembleRelease` → `BUILD SUCCESSFUL`, APK de 2.037.640 bytes.
- `cd android && ./gradlew testDebugUnitTest lint` con el puerto 8000 libre → **285 tests,
  0 fallos, 0 omitidos** y lint sin errores (el build de desarrollo no cambia).
- `./init.sh` completo en verde: pytest **166/166**, healthcheck 200, datos sembrados con
  token OK, APK de desarrollo compilado, instalado y abierto en el Xiaomi.

## Pendiente relacionado

La **firma del APK release** sigue sin hacerse (decisión del usuario): sin ella el APK
release no se puede instalar en un móvil. El smoke test de arriba usa la clave de
depuración solo como herramienta de prueba.
