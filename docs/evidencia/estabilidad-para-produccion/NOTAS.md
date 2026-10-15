# Evidencia — estabilidad-para-produccion

Verificación en el **Xiaomi Redmi Note 10 Pro (M2101K6G)** con el APK de desarrollo
compilado por `./init.sh` el 2026-10-15, tras los arreglos de estabilidad de la app
y del backend (11 commits: `b106f10..527d550` en Android y `b8c80f8..0d62120` en backend).

## Comprobaciones de esta carpeta

- `xiaomi-tras-migracion-bienvenida.png`: la app abre sin crash tras la **migración Room
  2→3** de la cola de notas (`dueno`) sobre la base real del dispositivo, que contiene
  notas de sesiones anteriores. La migración se ejecuta al arrancar (la cola se reanuda en
  `AplicacionOlivApp.onCreate()`); `logcat -d` sin `FATAL EXCEPTION`, sin errores de
  `AndroidRuntime` ni `SQLiteException`, y el proceso sigue vivo (PID 8340).
- `xiaomi-panel.png`: inicio de sesión real con la cuenta de verificación
  (`verificacion-init@ejemplo.com`, socio-001) contra el backend de desarrollo: panel con
  la Campaña 2025/2026, gráfico de campañas y campañas anteriores, sin errores.
- `xiaomi-notas-voz-sin-notas-ajenas.png`: la pantalla "Tus notas de voz" muestra el estado
  vacío ("Todavía no has grabado ninguna nota de voz") para la cuenta iniciada, **aunque el
  teléfono tiene notas de sesiones anteriores**: son las notas sin dueño (`dueno = ''`) que,
  por decisión declarada, no se muestran ni se sincronizan con ninguna cuenta hasta que se
  les asigne dueño. Es la comprobación en dispositivo del aislamiento de la cola por cuenta.

## Recorrido verificado en esta sesión

1. `./init.sh` completo: pytest del backend 166/166, healthcheck 200, datos sembrados con
   token OK, APK compilado, instalado y abierto en el Xiaomi.
2. `cd android && ./gradlew testDebugUnitTest lint` con el puerto 8000 libre: 285 tests,
   0 fallos, 0 omitidos; lint sin errores.
3. En el dispositivo: abrir la app tras la migración, iniciar sesión, panel, "Mi cuaderno"
   (línea de tiempo con 9 labores de la campaña actual) y "Tus notas de voz" (estado vacío
   correcto para la cuenta), sin crashes en todo el recorrido.

## Notas

- La verificación de los arreglos concretos es por **tests** (cada arreglo tiene su test que
  reproduce el fallo): rotación con "Deshacer", micrófono al salir del borrador IA, notas
  transcribiendo, cola por cuenta (sobre Room real), parseos JSON, dobles pulsaciones,
  subidas gigantes, respuestas del gateway inesperadas, carrera del registro y caché de
  precios.
- La transcripción de voz de extremo a extremo no se pudo repetir: el gateway de DevExpert
  seguía devolviendo 502 (pendiente desde el 2026-10-15).
