# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `/Users/<usuario>/Projects/OliveApp`
- Camino estándar de arranque: `./init.sh`
- Camino estándar de verificación: `./init.sh` (backend: pytest + healthcheck; Android: compilación del APK e instalación/apertura automática si hay dispositivo por USB)
- Siguiente feature lista actual: `aislamiento-de-datos-por-socio` (primera sin terminar con dependencias cumplidas; a continuación `cola-offline-de-notas` y `reintento-de-transcripcion-fallida`)
- Bloqueo actual: ninguno (las 4 decisiones bloqueantes quedaron resueltas en ADR 0003-0006; ADR 0007 cerró el mapeo lab/industrial y el ajuste industrial por defecto; la validación empírica del pipeline de voz quedó cerrada en la sesión 019)
- Última verificación: 2026-10-13 (`./init.sh` completo: pytest 74/74, healthcheck 200, datos sembrados con token OK, APK instalado y abierto; eliminación de labor verificada desde la app con diálogo de confirmación y borrado directo comprobado en API)

## Registro de sesiones

### Sesión 001

- Fecha: 2026-09-28
- Objetivo: crear el harness de arranque mínimo.
- Completado: `AGENTS.md`, `init.sh`, `PROGRESS.md` y `feature_list.json` creados.
- Verificación ejecutada: validación de JSON de `feature_list.json`, comprobación de `depends_on` (sin ciclos ni auto-referencias) y permiso de ejecución de `init.sh`.
- Evidencia capturada: los cuatro artefactos existen en la raíz y el script de arranque corre sin error en fase pre-bootstrap.
- Ficheros o artefactos actualizados: `AGENTS.md`, `init.sh`, `PROGRESS.md`, `feature_list.json`.
- Riesgo conocido o tema sin resolver: `init.sh` es provisional hasta el bootstrap técnico; siguen abiertas las cuatro decisiones bloqueantes de `docs/risks-and-open-questions.md`.
- Mejor siguiente paso: resolver las decisiones bloqueantes y ejecutar `bootstrap-backend` (primer bootstrap técnico) según `feature_list.json`.

### Sesión 002

- Fecha: 2026-09-28
- Objetivo: resolver la decisión bloqueante sobre despliegue del backend e instalación del APK.
- Completado: análisis de opciones (PaaS gratuito / Cloud Run / VPS / serverless / túnel local; APK por archivo / adb / tiendas) y decisión acordada con el usuario.
- Verificación ejecutada: ninguna técnica (decisión de diseño, sin código).
- Evidencia capturada: `docs/adr/0003-backend-paas-gratuito-y-apk-por-archivo.md`.
- Ficheros o artefactos actualizados: `docs/adr/0003-backend-paas-gratuito-y-apk-por-archivo.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: cold start del free tier (mitigado con "despertar" el backend antes de la demo); verificar cuota/sleep del proveedor al elegirlo. Quedan 3 decisiones bloqueantes: endpoint de audio del gateway, campos SIEX y datos sembrados.
- Mejor siguiente paso: resolver la decisión 1 (endpoint de audio y modelo de texto en `inference.devexpert.io`) con el usuario.

### Sesión 003

- Fecha: 2026-09-28
- Objetivo: resolver la decisión bloqueante sobre el pipeline de voz en el gateway de inferencia.
- Completado: análisis de opciones a partir de la referencia real del gateway (aliases `transcribe`, `chat`, `chat-pro`) y decisión acordada con el usuario: empezar con los modelos baratos y subir solo donde falle.
- Verificación ejecutada: ninguna técnica (decisión de diseño, sin código).
- Evidencia capturada: `docs/adr/0004-transcripcion-y-extraccion-via-gateway.md`.
- Ficheros o artefactos actualizados: `docs/adr/0004-transcripcion-y-extraccion-via-gateway.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: pendiente la validación empírica del pipeline (formato de audio, latencia, coste, JSON forzado) antes de implementar la feature de voz. Quedan 2 decisiones bloqueantes: campos SIEX y datos sembrados.
- Mejor siguiente paso: resolver la decisión 2 (campos SIEX por categoría de labor) con el usuario.

### Sesión 004

- Fecha: 2026-09-28
- Objetivo: resolver la decisión bloqueante sobre los campos SIEX por categoría de labor.
- Completado: contrastación con fuentes oficiales (nota FEGA del Cuaderno Digital y Manual CUE SIEX), decisión de modelo híbrido acordada con el usuario (núcleo por categoría + plaga a controlar + observaciones, listas cerradas) y matiz nuevo: el relleno del borrador IA es conversacional (el asistente pregunta por los campos que faltan).
- Verificación ejecutada: ninguna técnica (decisión de diseño, sin código).
- Evidencia capturada: `docs/adr/0005-campos-por-categoria-alineados-siex.md` (incluye mapeo SIEX ↔ OlivApp y fuentes).
- Ficheros o artefactos actualizados: `docs/adr/0005-campos-por-categoria-alineados-siex.md`, `CONTEXT.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `feature_list.json` (nueva feature `completar-borrador-por-preguntas`, 27 en total), `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: el flujo de voz pasa a ser conversacional (más de un turno), lo que suma complejidad al borrador IA; queda 1 decisión bloqueante: formato y valores de los datos sembrados.
- Mejor siguiente paso: resolver la decisión 3 (datos sembrados de campaña) con el usuario.

### Sesión 005

- Fecha: 2026-09-28
- Objetivo: resolver la última decisión bloqueante: formato y valores de los datos de campaña sembrados.
- Completado: investigación de datos reales del sector (informe REDES TECO del MAPA y liquidaciones publicadas de campañas 23/24) y decisión acordada con el usuario: seed JSON versionado + script, 4 campañas (actual 2025/2026 completa) y 2 socios, con dataset calibrado sustituible por datos reales anonimizados.
- Verificación ejecutada: ninguna técnica (decisión de diseño, sin código).
- Evidencia capturada: `docs/adr/0006-datos-sembrados-de-campana.md` (tabla de dataset y fuentes).
- Ficheros o artefactos actualizados: `docs/adr/0006-datos-sembrados-de-campana.md`, `docs/risks-and-open-questions.md`, `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: los valores del seed son inventados calibrados (declarados como demo); pendiente la validación empírica del pipeline de voz (ADR 0004) antes de implementar la feature de voz.
- Mejor siguiente paso: bootstrap técnico (`bootstrap-backend` o `bootstrap-android` según `feature_list.json`), que desbloquea toda la implementación.

### Sesión 006

- Fecha: 2026-10-01
- Objetivo: aprovechar las capturas reales de SB Portal Socios como referencia de producto y decidir el mapeo laboratorio/industrial.
- Completado: análisis de la app real (arquitectura de información, mapeo de conceptos, lecciones de UX, sin datos personales), validación de la regla de coherencia con datos reales y decisiones acordadas con el usuario: mapeo declarado sin ampliar el modelo (rendimiento = laboratorio, aceite = industrial), ajuste industrial por defecto 10 % (merma real medida 9,7 %) y dataset con la campaña 2025/2026 del socio 1 con datos reales anonimizados (5 vales, 10.958 kg).
- Verificación ejecutada: validación de JSON de `feature_list.json` y comprobación aritmética de la merma lab→industrial sobre los datos reales (9,71 % en dos agregados).
- Evidencia capturada: `docs/referencia-sb-portal-socios.md` y `docs/adr/0007-mapeo-laboratorio-industrial-y-ajuste-real.md`.
- Ficheros o artefactos actualizados: `docs/referencia-sb-portal-socios.md`, `docs/adr/0007-mapeo-laboratorio-industrial-y-ajuste-real.md`, `docs/adr/0006-datos-sembrados-de-campana.md`, `CONTEXT.md`, `docs/risks-and-open-questions.md`, `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: un observador puede notar que kilos × rendimiento no cuadra con el aceite mostrado (merma industrial); queda declarado en CONTEXT.md y ADR 0007. Pendiente la validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: bootstrap técnico (`bootstrap-backend` o `bootstrap-android`) según `feature_list.json`.

### Sesión 007

- Fecha: 2026-10-01
- Objetivo: bootstrap técnico del backend (feature `bootstrap-backend`).
- Completado: esqueleto del backend en `backend/` con FastAPI (Python 3.9 del sistema, código compatible 3.9): endpoint `GET /health` y validación estricta de `DEVEXPERT_API_KEY` y `JWT_SECRET` por variables de entorno (falla explícita si faltan); tests base; `init.sh` con los comandos reales de instalación, verificación y arranque.
- Verificación ejecutada: `./init.sh` completo (instalación, pytest 3/3, healthcheck real 200) y arranque real sin variables obligatorias (RuntimeError con el nombre de las que faltan, exit 3).
- Evidencia capturada: `feature_list.json` (`bootstrap-backend` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/` (requirements, app, tests), `init.sh`, `.gitignore`, `feature_list.json`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: base de datos sin elegir (SQLite para desarrollo vs PostgreSQL en el PaaS); pendiente la validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `bootstrap-android` (proyecto Kotlin + Compose que compila a APK y se instala en el móvil).

### Sesión 008

- Fecha: 2026-10-05
- Objetivo: bootstrap técnico de la app Android (feature `bootstrap-android`).
- Completado: proyecto Kotlin + Jetpack Compose (Material 3) en `android/` (Gradle 8.10.2, AGP 8.7.3, Kotlin 2.0.21, minSdk 26 / compileSdk 35) con pantalla mínima de bienvenida según `DESIGN.md` (paleta crema/oliva, título 28sp, cuerpo 18sp); wrapper de Gradle oficial; `init.sh` ampliado con los comandos reales de Android (compilación e instalación/apertura automática si hay dispositivo por USB).
- Verificación ejecutada: `./init.sh` completo y limpio (pytest 3/3, healthcheck 200, `assembleDebug` OK → `app-debug.apk` 9 MB) y, en el dispositivo, `adb install -r` → Success y `am start` abriendo la app; captura de pantalla por `adb` confirmando la pantalla mínima.
- Evidencia capturada: `feature_list.json` (`bootstrap-android` → `passing`, con evidencias) y captura revisada en sesión (no versionada: binario).
- Ficheros o artefactos actualizados: `android/` (proyecto completo), `init.sh`, `.gitignore`, `feature_list.json`, `docs/risks-and-open-questions.md`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: dispositivo verificado fue un **emulador Android 14** (`emulator-5554`), no el móvil físico del usuario: fijar el móvil definitivo de la demo (mismos comandos `adb`) y verificar la firma del APK release cuando toque la distribución; base de datos sin elegir; pendiente la validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `datos-sembrados-cooperativa` (sembrar el dataset de ADR 0006/0007 en el backend).

### Sesión 009

- Fecha: 2026-10-05
- Objetivo: verificar que los PRs de los dos bootstraps quedaron bien fusionados en `main` y limpiar el repo.
- Completado: `main` (`c879509`) contiene los PRs `#1` (backend) y `#2` (Android) con la historia intacta; verificación completa desde `main` con el camino estándar; ramas `feature/*` borradas (locales y de GitHub).
- Verificación ejecutada: `./init.sh` desde `main` limpio → pytest 3/3, healthcheck 200, `assembleDebug` OK (9 MB) y `adb install` Success + apertura en el dispositivo conectado (emulador Android 14).
- Evidencia capturada: salida de `./init.sh` en sesión; `feature_list.json` conserva las evidencias de las dos features (`passing`).
- Ficheros o artefactos actualizados: `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: sin cambios respecto a la sesión 008 (móvil físico de la demo por fijar, firma del APK release pendiente, base de datos sin elegir, validación empírica del pipeline de voz pendiente).
- Mejor siguiente paso: `datos-sembrados-cooperativa` (sembrar el dataset de ADR 0006/0007 en el backend).

### Sesión 010

- Fecha: 2026-10-05
- Objetivo: feature `datos-sembrados-cooperativa` (dataset de campañas en el backend).
- Completado: seed versionado `backend/data/seed.json` (ADR 0006/0007: 1 cooperativa, 4 campañas con 2025/2026 actual, 2 socios, 8 datos oficiales — socio 1 de 25/26 con datos reales anonimizados — y 7 parcelas) con carga automática e idempotente si la base está vacía; esquema SQLite (`backend/app/db.py`); API de solo lectura (`GET /api/v1/cooperativa`, `/socios`, `/socios/{id}/campanas`, `/socios/{id}/parcelas`) con declaración de datos de demostración.
- Verificación ejecutada: `./init.sh` completo (pytest 11/11 — 8 nuevos: volumen, campaña actual + anteriores, regla de coherencia en dos etapas en las 8 filas, datos reales de 25/26, parcelas, rechazo de escrituras con 405, socio desconocido 404 e idempotencia del seed —, healthcheck 200 y `GET /api/v1/socios` OK) y comprobación en vivo de la API con el dataset real y del 405 ante POST.
- Evidencia capturada: `feature_list.json` (`datos-sembrados-cooperativa` → `passing`, con evidencias) y respuestas de la API en sesión.
- Ficheros o artefactos actualizados: `backend/data/seed.json`, `backend/app/db.py`, `backend/app/seed.py`, `backend/app/main.py`, `backend/tests/` (conftest + tests del seed), `init.sh`, `.gitignore`, `feature_list.json`, `docs/risks-and-open-questions.md`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: SQLite puede no persistir en el free tier con disco efímero (anotado en riesgos; alternativa PostgreSQL del PaaS); pendientes: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `registro-cuenta-socio` (alta con email y contraseña asociada a la cooperativa sembrada).

### Sesión 011

- Fecha: 2026-10-09
- Objetivo: feature `registro-cuenta-socio` (alta de cuenta con email y contraseña).
- Completado: backend con `POST /api/v1/registro` + módulo `cuentas.py` (validación de email y contraseña mínima de 8 caracteres, email único sin distinguir mayúsculas, hasheado PBKDF2-HMAC-SHA256 con sal y 200.000 iteraciones, cuenta asociada a coop-demo) y pantalla "Crear cuenta" en la app (campos con etiqueta grande, botón de 64dp, mensajes en frases claras) llamando al backend por OkHttp. Decisiones: política de contraseñas (registrada en riesgos) y vocabulario "Cuenta" añadido a `CONTEXT.md`.
- Verificación ejecutada: `./init.sh` completo (pytest 17/17 con 6 tests nuevos, healthcheck, datos sembrados, APK instalado y abierto) y verificación real desde la app en el emulador: registro de socio.nuevo@ejemplo.com → "Cuenta creada…", fila en `cuenta` con hash `pbkdf2_sha256$…` (sin texto claro) y `cooperativa_id = coop-demo`; segundo registro del mismo email → "Ese email ya está registrado. Usa el inicio de sesión." sin fila duplicada. Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`registro-cuenta-socio` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/cuentas.py`, `backend/app/db.py` (tabla `cuenta`), `backend/app/main.py`, `backend/tests/test_registro.py`, `android/` (pantallas + OkHttp + permiso INTERNET), `init.sh` (detección de dispositivo robustecida), `CONTEXT.md`, `docs/risks-and-open-questions.md`, `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: incidencias de entorno resueltas en sesión (ANR del AVD nuevo por carga del sistema en el primer boot con render por software; `avdmanager` legacy incompatible con JDK 22 → cmdline-tools modernos instalados; AVD dedicado `OlivApp_API_34` creado como dispositivo objetivo pero la verificación se hizo en el emulador `Pixel_9_API_34` lanzado desde Android Studio, que quedó como único dispositivo). Sigue pendiente fijar el móvil físico de la demo; y queda abierta la vinculación entre la cuenta registrada y los datos oficiales del socio del seed (se decidirá en `panel-campana-actual`).
- Mejor siguiente paso: `inicio-de-sesion` (email + contraseña → sesión con token JWT).

### Sesión 012

- Fecha: 2026-10-09
- Objetivo: feature `inicio-de-sesion` (login con sesión persistente y llamadas autenticadas).
- Completado: backend con `POST /api/v1/login` (JWT HS256 vía PyJWT, caducidad 30 días, error único "Email o contraseña incorrectos." que no revela qué falla), verificación de contraseña PBKDF2 en `cuentas.py`, endpoints de datos protegidos con Bearer (`/api/v1/mi-cuenta` nuevo como prueba de llamada autenticada; públicos `/health`, `/api/v1/registro`, `/api/v1/login`) y app con pantalla "Iniciar sesión", pantalla "Sesión iniciada" y token en EncryptedSharedPreferences (clave del Keystore, AES-256) que mantiene la sesión tras reiniciar. Decisiones: almacenamiento del token resuelto (riesgos) y nota operativa `JWT_SECRET` ≥ 32 bytes.
- Cambio de verificación declarado (no en silencio): la comprobación de datos sembrados de `init.sh` ahora crea o reutiliza una cuenta de verificación y consulta `/api/v1/socios` con token, porque desde esta feature las llamadas de datos van autenticadas (comportamiento objetivo de la propia feature).
- Verificación ejecutada: `./init.sh` completo (pytest 24/24 con 7 tests nuevos de sesión, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: login de socio.nuevo@ejemplo.com → "Sesión iniciada" con email obtenido del endpoint protegido; en vivo `GET /api/v1/socios/socio-001/campanas` → 401 sin token, 401 con token inventado, 200 con token de sesión; app cerrada en seco y reabierta → sesión sigue abierta, con el token cifrado en preferencias (0 apariciones de JWT en claro). Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`inicio-de-sesion` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/sesiones.py`, `backend/app/cuentas.py`, `backend/app/main.py`, `backend/requirements.txt` (PyJWT), `backend/tests/test_sesiones.py`, `backend/tests/test_datos_sembrados.py` (autenticación en los tests), `android/` (pantallas de login y sesión + EncryptedSharedPreferences), `init.sh` (comprobación de seed autenticada), `docs/risks-and-open-questions.md` (token resuelto), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: PR #4 (`registro-cuenta-socio`) aún pendiente de fusionar en main (esta rama está apilada sobre él); queda fuera de alcance el cierre de sesión (siguiente feature), por lo que ahora mismo la sesión solo se abandona desinstalando o limpiando datos; pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `cierre-de-sesion` (abandonar la sesión y borrar el token guardado).

### Sesión 013

- Fecha: 2026-10-09
- Objetivo: feature `cierre-de-sesion` (abandonar la sesión desde la app).
- Completado: botón "Cerrar sesión" en la pantalla de sesión que borra el token de EncryptedSharedPreferences y vuelve a la pantalla de acceso. Sin cambios en el backend (el alcance es del dispositivo: el token JWT caduca por sí mismo en 30 días). Aclaración registrada: cierre de sesión ≠ baja de cuenta (la baja sigue fuera del MVP).
- Verificación ejecutada: `./init.sh` completo (pytest 24/24, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: "Cerrar sesión" → pantalla de acceso, preferencias cifradas sin token (0 apariciones de JWT) y, tras cerrar y reabrir la app, se exige iniciar sesión de nuevo. Captura en sesión (no versionada).
- Evidencia capturada: `feature_list.json` (`cierre-de-sesion` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `android/.../MainActivity.kt` (`AlmacenSesiones.borrar`, botón "Cerrar sesión"), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: el cierre es solo local (el token emitido sigue siendo válido hasta caducar; aceptable en el MVP y coherente con JWT sin lista de revocación); pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `panel-campana-actual` (panel con las cifras oficiales de la campaña en curso).

### Sesión 014

- Fecha: 2026-10-09
- Objetivo: feature `panel-campana-actual` (panel de aterrizaje con la campaña actual y los datos oficiales).
- Completado: `GET /api/v1/mi-panel` (autenticado) que sirve la campaña actual con los tres datos oficiales del socio de la cuenta, y pantalla "Panel" como aterrizaje tras entrar: "Campaña 2025/2026" con bloque "Datos oficiales de la cooperativa" (kilos entregados, rendimiento graso y aceite obtenido, formato español), nota de separación de las labores y acceso a "Cuaderno de campo" y "Simulador de liquidación" (con aviso de disponibilidad futura). Decisiones tomadas y registradas en riesgos: la campaña actual es la marcada en los datos de la cooperativa (no por fecha del sistema) y cada cuenta nueva queda vinculada al socio de demostración `socio-001` (columna `cuenta.socio_id` con migración de bases antiguas).
- Verificación ejecutada: `./init.sh` completo (pytest 30/30 con 6 tests nuevos del panel, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: entrada como socio → panel con "Campaña 2025/2026" y los tres datos oficiales sembrados tal cual ("10.958 kg", "17,93 %", "1.774 kg") en bloque propio, separados de las labores (nota visible) y con los dos accesos; el acceso al cuaderno responde "El cuaderno de campo estará disponible próximamente." Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`panel-campana-actual` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/db.py` (`cuenta.socio_id` + migración), `backend/app/cuentas.py` (`SOCIO_DE_DEMOSTRACION`), `backend/app/main.py` (`GET /api/v1/mi-panel`), `backend/tests/test_panel.py`, `android/.../MainActivity.kt` (pantalla Panel con formato español), `docs/risks-and-open-questions.md` (campaña actual y vinculación cuenta↔socio resueltas), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: la vinculación cuenta ↔ socio es la de demostración (`socio-001` para todas las cuentas); la separación real por socio queda para `aislamiento-de-datos-por-socio`. Pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `consulta-campanas-anteriores` (abrir una campaña anterior desde el panel).

### Sesión 015

- Fecha: 2026-10-09
- Objetivo: feature `consulta-campanas-anteriores` (abrir una campaña cerrada desde el panel, solo consulta).
- Completado: `GET /api/v1/mi-campanas` (listado con estado `actual`/`cerrada` según docs/domain-model.md) y `GET /api/v1/mi-campanas/{id}` (detalle con `datos_oficiales` y `labores` en claves separadas; la sección de labores llega vacía hasta que exista el cuaderno de campo, separación ya modelada en API y UI). App: sección "Campañas anteriores" en el panel y pantalla de campaña con aviso "Campaña cerrada: solo consulta. No se pueden añadir labores.", bloque "Datos oficiales de la cooperativa" y bloque "Mi cuaderno" aparte, sin control para añadir labores.
- Verificación ejecutada: `./init.sh` completo (pytest 35/35 con 5 tests nuevos, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: desde el panel se abre "Campaña 2024/2025" y muestra los datos sembrados (6.200 kg / 21,40 % / 1.198 kg) y "Mi cuaderno" por separado, en solo consulta; en vivo, POST a la ruta de la campaña → 405 (solo lectura). Captura en sesión (no versionada).
- Evidencia capturada: `feature_list.json` (`consulta-campanas-anteriores` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/main.py` (`/api/v1/mi-campanas` y detalle), `backend/tests/test_campanas_anteriores.py`, `android/.../MainActivity.kt` (sección de campañas anteriores y pantalla de campaña), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: la sección de labores está vacía hasta que llegue el cuaderno de campo (comportamiento esperado en esta etapa, documentado); pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `alta-de-parcela` (crear una parcela con nombre propio).

### Sesión 016

- Fecha: 2026-10-09
- Objetivo: feature `alta-de-parcela` (alta de parcelas con nombre propio).
- Completado: módulo `parcelas.py` (nombre obligatorio, máximo 60 caracteres, sin borrado ni archivo en el MVP) con `GET/POST /api/v1/mis-parcelas` autenticados (la parcela se asocia al socio de la cuenta) y pantalla "Tus parcelas" en la app (alta por nombre + lista del socio + aviso "Las parcelas no se borran en esta versión."), accesible desde el panel. Sin cambios de reglas: se confirmó mantener el flujo secuencial de una feature cada vez.
- Verificación ejecutada: `./init.sh` completo (pytest 43/43 con 8 tests nuevos, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: alta de "La Noria" → "Parcela creada." y visible en la lista del socio (4 parcelas sembradas + nueva); aislamiento en vivo: la lista de `socio-001` la contiene y la de `socio-002` no (tampoco al revés), con `socio_id = socio-001` en la base. Captura en sesión (no versionada).
- Evidencia capturada: `feature_list.json` (`alta-de-parcela` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/parcelas.py`, `backend/app/main.py` (rutas de parcelas), `backend/tests/test_parcelas.py`, `android/.../MainActivity.kt` (pantalla Tus parcelas), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) artefacto de pilotaje anotado: `adb input text` no admite espacios (usa `%s`); un intento creó la fila "La", borrada de la base de desarrollo como error de tecleado antes de la captura final; (2) observación de entorno: en un arranque en frío del backend la app mostró "No se pudieron cargar tus datos." una vez (transitorio; sin reintento automático en la app, comportamiento a revisar si se repite). Pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `alta-manual-labor` (crear una labor con el formulario manual).

### Sesión 017

- Fecha: 2026-10-09
- Objetivo: feature `alta-manual-labor` (alta de labor con el formulario manual).
- Completado: módulo `labores.py` con campos por categoría y listas cerradas (ADR 0005: tratamiento con producto, plaga a controlar, cantidad y unidad; riego con cantidad y unidad; cosecha con modalidad y kilos; observaciones opcionales en toda labor), tabla `labor` en el esquema y `POST /api/v1/mis-labores` autenticado que guarda la labor en la campaña que corresponde a su fecha y rellena `labores` en el detalle de campaña. Decisión registrada: la campaña de una labor se deriva de su fecha (campaña oleícola del 1 de noviembre al 31 de octubre, p. ej. 15/12/2025 → 2025/2026), resolviendo la pregunta abierta del modelo de campaña; solo admite labores la campaña actual (las cerradas son de solo consulta). App: "Cuaderno de campo" abre el formulario "Apuntar una labor" (fecha, parcela en lista, categoría y sus campos en listas cerradas, observaciones), con confirmación "Labor guardada en la campaña 2025/2026." y acceso a "Ver Mi cuaderno de esa campaña"; el bloque "Mi cuaderno" del detalle de campaña lista las labores por fecha, separadas de los datos oficiales.
- Verificación ejecutada: `./init.sh` completo (pytest 53/53 con 10 tests nuevos de labores, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: una labor de cada categoría creada por el formulario con sus campos (tratamiento fitosanitario: producto, plaga de la lista, cantidad con unidad, observaciones; riego: cantidad con unidad; cosecha: modalidad y kilos) y las tres listadas en "Mi cuaderno" de la campaña 2025/2026 —la que corresponde a sus fechas (10/12/2025, 20/01/2026, 15/02/2026)—, separadas de los datos oficiales; límites cubiertos por tests (fecha de campaña cerrada o fuera de toda campaña rechazada, listas cerradas, parcela de otro socio sin acceso). Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`alta-manual-labor` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/labores.py`, `backend/app/db.py` (tabla `labor`), `backend/app/main.py` (`POST /api/v1/mis-labores` y `labores` en el detalle de campaña), `backend/tests/test_labores.py`, `android/.../MainActivity.kt` (formulario "Apuntar una labor" con listas desplegables y bloque "Mi cuaderno" con labores), `docs/domain-model.md` (regla de campaña por fecha), `docs/risks-and-open-questions.md` (modelo de campaña resuelto), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) artefactos de pilotaje: el teclado del emulador mete autocorrección en `adb input text` (una "y" suelta en un intento, corregida reescribiendo) y desplaza la pantalla al abrirse (cerrar con `keyevent 4` antes de pulsar botones); (2) una cuarta labor de prueba (riego de hoy) creada para verificar el formato de cantidades fue borrada de la base de desarrollo antes de la captura final, dejando solo las tres labores de demo; (3) el detalle de la campaña actual solo es alcanzable desde "Ver Mi cuaderno de esa campaña" tras guardar una labor (la lista del panel son solo campañas anteriores), matiz de navegación a revisar si hace falta. Pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `grabacion-de-nota-voz`, con la validación empírica del pipeline de voz (ADR 0004) como paso previo declarado; si se prefiere evitar ese bloqueo, `calculo-de-liquidacion-en-limpio` está lista.

### Sesión 018

- Fecha: 2026-10-09
- Objetivo: feature `grabacion-de-nota-voz` (el socio dicta una nota de voz con el botón de micrófono).
- Completado: permiso `RECORD_AUDIO` en el manifiesto con petición en tiempo de ejecución, grabadora nativa `MediaRecorder` (MPEG-4 con audio AAC a 16 kHz, fichero `.m4a`) con límite de 60 segundos y parada automática, cola local `AlmacenNotasVoz` (fichero de audio + entrada con fecha, segundos y estado `pendiente` en `files/notas-voz.json`) y pantalla "Notas de voz" con botón grande Grabar/Parar, contador visible, estado de cada nota ("Pendiente de transcripción") y aviso de audio demasiado corto (menos de 1 segundo: se descarta y se pide repetir, según el modelo de dominio). El panel suma el botón grande "Dictar una nota de voz". Decisiones registradas: registro local hasta la transcripción (coherente con "pendiente: grabada sin conexión" del modelo), límite 60 s y formato MPEG-4/AAC, resolviendo esa pregunta abierta. Sin cambios en el backend.
- Verificación ejecutada: `./init.sh` completo (pytest 53/53, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador: permiso de micrófono concedido en el diálogo del sistema ("¿Permitir que OlivApp grabe audio?" → "Mientras se usa la aplicación"), grabación con contador ("Grabando… 2 de 60 segundos"), dos notas (6 s y 3 s) registradas y listadas como "Pendiente de transcripción", cada una con su audio asociado en el dispositivo (ficheros `.m4a` no vacíos de 13 KB y 7,9 KB con contenedor ISO Media MP4 v2 = MPEG-4/AAC, y entrada en la cola con fichero, segundos, fecha y estado) y caso de grabación demasiado corta comprobado en vivo (mensaje de repetir, sin registrar). Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`grabacion-de-nota-voz` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `android/.../AndroidManifest.xml` (permiso de micrófono), `android/.../MainActivity.kt` (pantalla "Notas de voz", `AlmacenNotasVoz`, `GrabadorNotas`, botón grande del panel), `docs/domain-model.md` (nota de voz con límite y formato, caso de audio demasiado largo), `CONTEXT.md` (máximo 60 segundos), `docs/risks-and-open-questions.md` (límite y formato resueltos), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) la verificación de grabación se hizo en el emulador `Pixel_9_API_34` (micrófono virtual): el riesgo de permisos y fabricantes reales sigue abierto y pide probar en el móvil físico de la demo pronto; (2) las notas grabadas viven solo en el dispositivo (sin sincronizar al backend hasta la transcripción): si se desinstala la app se pierden, aceptable en el MVP y coherente con la cola offline pendiente; (3) el pilotaje por `uiautomator` exige taps con coordenadas del estado actual (el teclado del sistema desplaza la pantalla), ya conocido de sesiones anteriores. Pendientes históricos: móvil físico de la demo, firma del APK release, validación empírica del pipeline de voz (ADR 0004).
- Mejor siguiente paso: `consulta-cuaderno-por-campana` (primera sin terminar con dependencias cumplidas en `feature_list.json`); `transcripcion-de-nota-voz` exige antes validar el pipeline de voz del ADR 0004.

### Sesión 019

- Fecha: 2026-10-09
- Objetivo: feature `transcripcion-de-nota-voz` (el backend transcribe la nota de voz al español), con la validación empírica del pipeline de voz (ADR 0004) como paso previo.
- Completado: (1) **Validación empírica del ADR 0004** contra `inference.devexpert.io` con la clave real del gateway (solo en variables de entorno del servidor, nunca en el repo): alias `transcribe` acepta el formato nativo de Android (MPEG-4/AAC 16 kHz) tal cual y devuelve el español con 1,7 s de latencia y 85 tokens; alias `chat` con `response_format: json_object` devuelve JSON válido de extracción en 5,7 s (desambigua "Lanoria" → "La Noria" con la lista de parcelas); resultados registrados en el ADR y en `docs/risks-and-open-questions.md` (research task cerrada). (2) Backend: `app/transcripcion.py` + `POST /api/v1/mis-notas-voz/transcripcion` autenticado (multipart) que transcribe con el alias `transcribe` y **borra el audio del servidor** (solo vive en un fichero temporal durante la llamada); errores claros: formato no válido, audio demasiado grande, "No se ha entendido el audio. Vuelve a grabar la nota." y 502 reintentable. (3) App: la nota se envía sola al backend tras grabarla (y también las pendientes al abrir la pantalla), con estados visibles "Pendiente de transcripción" → "Transcribiendo…" → "Transcrita" (con el texto de la nota visible) o error con botón "Reintentar".
- Verificación ejecutada: `./init.sh` completo (pytest 61/61 con 8 tests nuevos de transcripción —incluido el borrado del audio en el servidor—, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador `OlivApp_API_34` (AVD dedicado de la demo, imagen google_apis): una nota de voz real en español del usuario (6 s, "He hecho un tratamiento en la parcela La Noria contra la mosca del olivo con dos litros de producto el veinte de enero.") registrada en la app con el formato y la estructura exactos de una nota grabada se transcribió por la ruta completa (app → backend → gateway) y quedó en estado "Transcrita" con el texto visible; la ruta de error quedó verificada en vivo con el gateway real (audio sin voz → "No se ha entendido el audio…" con error visible y Reintentar). Captura en sesión (no versionada).
- Evidencia capturada: `feature_list.json` (`transcripcion-de-nota-voz` → `passing`, con evidencias), ADR 0004 con la tabla de latencias y costes.
- Ficheros o artefactos actualizados: `backend/app/transcripcion.py`, `backend/app/main.py` (endpoint de transcripción), `backend/requirements.txt` (`python-multipart`), `backend/tests/test_transcripcion.py` (8 tests), `android/.../MainActivity.kt` (envío automático, estados, texto visible y Reintentar), `docs/adr/0004-transcripcion-y-extraccion-via-gateway.md` (validación empírica), `docs/risks-and-open-questions.md`, `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) el micrófono virtual del emulador solo captura silencio (confirmado con el intento real del usuario): la nota de voz real se inyectó con root en el AVD google_apis con el mismo formato y estructura que las grabadas; para la literalidad completa queda pendiente una grabación con micrófono real (móvil físico de la demo, o micrófono del host habilitado para el emulador); (2) parte del comportamiento de `reintento-de-transcripcion-fallida` (error visible + Reintentar) y de `sincronizacion-pendientes-al-reconectar` (envío automático de pendientes al abrir la pantalla) quedó implementada aquí por necesidad de la feature; sus verificaciones propias siguen pendientes; (3) al pilotar con `uiautomator` los taps deben calcularse del estado actual (un tap a coordenadas antiguas cerró la sesión y obligó a reentrar). Pendientes históricos: móvil físico de la demo, firma del APK release.
- Mejor siguiente paso: `consulta-cuaderno-por-campana` (primera sin terminar con dependencias cumplidas en `feature_list.json`, en orden de lista); la extracción del borrador IA ya tiene validado su paso de gateway (`chat` con JSON forzado).

### Sesión 020

- Fecha: 2026-10-09
- Objetivo: feature `consulta-cuaderno-por-campana` ("Mi cuaderno" como historial cronológico agrupado por campaña).
- Completado: endpoint `GET /api/v1/mis-labores` que devuelve las labores del socio agrupadas por campaña (`labores_del_socio_por_campana` en `labores.py`): de la campaña más reciente a la más antigua y, dentro de cada campaña, de la labor más reciente a la más antigua, solo labores vigentes y del socio autenticado. App: pantalla "Mi cuaderno" (desde "Cuaderno de campo" del panel, que antes abría directamente el formulario) con botón "Apuntar una labor", grupos por campaña ("Campaña 2025/2026 · Campaña actual") y la ficha de cada labor con los campos tipo SIEX etiquetados y legibles de un vistazo (Parcela, Producto, Plaga a controlar, Cantidad, Modalidad, Kilos, observaciones); el bloque "Mi cuaderno" del detalle de campaña usa la misma ficha. Ajuste de navegación: "Volver a Mi cuaderno" desde el formulario (el detalle de campaña sigue alcanzable por "Ver Mi cuaderno de esa campaña").
- Verificación ejecutada: `./init.sh` completo (pytest 65/65 con 4 tests nuevos del historial —agrupación y orden cronológico, campos SIEX, aislamiento por socio, historial vacío—, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador `OlivApp_API_34` con labores en dos campañas: grupos "Campaña 2025/2026 · Campaña actual" (15/02/2026 cosecha → 20/01/2026 riego → 10/12/2025 tratamiento) y "Campaña 2024/2025" (20/01/2025 cosecha → 05/12/2024 tratamiento), con los campos de cada labor legibles línea a línea. Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`consulta-cuaderno-por-campana` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/labores.py` (`labores_del_socio_por_campana`), `backend/app/main.py` (`GET /api/v1/mis-labores`), `backend/tests/test_labores.py` (4 tests nuevos), `android/.../MainActivity.kt` (pantalla "Mi cuaderno", `DatosCampanaDeLabores`, ficha de labor con campos etiquetados, `cargarMisLabores`, navegación), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) las dos labores de la campaña cerrada 2024/2025 se insertaron como datos de demostración en la base de desarrollo (la API solo admite la campaña actual por regla de dominio): si se recrea la base desde cero, el historial de la demo mostrará solo la campaña actual; (2) se limpió de la base de desarrollo una parcela "prueba" huérfana de pilotajes anteriores (5 parcelas y 5 labores de demo quedan para `socio-001`); (3) los tests que crean labores con unidades fuera de la lista cerrada fallaban en silencio (la API las rechazaba y la aserción posterior lo descubría): los tests nuevos crean con assert de estado para que un rechazo no pase desapercibido. Pendientes históricos: móvil físico de la demo, firma del APK release.
- Mejor siguiente paso: `edicion-de-labor` (siguiente en orden de lista con dependencias cumplidas).

### Sesión 021

- Fecha: 2026-10-09
- Objetivo: feature `edicion-de-labor` (el socio corrige cualquier campo de una labor).
- Completado: endpoint `PUT /api/v1/mis-labores/{id}` que edita cualquier campo de una labor propia con la misma validación que el alta (compartida en `_preparar_labor`): campos por categoría (ADR 0005), campaña derivada de la fecha —si la fecha cambia, la campaña cambia con ella, y solo la campaña actual admite labores—, parcela del socio y labor ajena como 404 (`LaborAjena`). App: cada labor de "Mi cuaderno" tiene botón "Editar" que abre el mismo formulario precargado ("Editar labor" con fecha, parcela, categoría, campos y observaciones) y al guardar muestra "Cambios guardados en tu cuaderno."; el listado refleja los cambios y reordena la cronología sola.
- Verificación ejecutada: `./init.sh` completo (pytest 70/70 con 5 tests nuevos de edición, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador `OlivApp_API_34`: edición de la labor de cosecha con cambio de fecha (15/02/2026 → 05/03/2026), de parcela (La Noria → La Umbría, por el desplegable) y de kilos (850 → 920) → "Cambios guardados en tu cuaderno." → el listado enseña los tres cambios y la labor pasa al primer puesto de la cronología de la campaña. Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`edicion-de-labor` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/labores.py` (`_preparar_labor` compartida, `actualizar_labor`, `LaborAjena`), `backend/app/main.py` (`PUT /api/v1/mis-labores/{id}`), `backend/tests/test_labores.py` (5 tests nuevos), `android/.../MainActivity.kt` (modo edición del formulario con precarga, `LaborEditable`, `actualizarLabor`, botón "Editar" en cada labor), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) artefacto de pilotaje ya conocido agravado en este AVD: `adb input text` trunca caracteres y el teclado desplaza la pantalla (un intento dejó "0920" en el campo de fecha; corregido tecleando campo a campo con verificación tras cada uno); (2) la labor de demostración editada en la verificación se queda con los valores nuevos (05/03/2026, La Umbría, 920 kg) como estado de la demo; (3) la edición se lanza solo desde "Mi cuaderno" (el bloque del detalle de campaña sigue en solo consulta): si se pide editar desde ahí, es ampliación natural. Pendientes históricos: móvil físico de la demo, firma del APK release.
- Mejor siguiente paso: `eliminacion-de-labor` (siguiente en orden de lista con dependencias cumplidas).

### Sesión 022

- Fecha: 2026-10-13
- Objetivo: feature `eliminacion-de-labor` (el socio elimina una labor: borrado directo, sin papelera).
- Completado: se retomó el trabajo en curso de la rama `feature/eliminacion-de-labor` (código sin commitear de una sesión interrumpida) y se llevó a verificación y registro: `eliminar_labor` en `labores.py` (borrado directo de labores propias, `LaborAjena` como 404), `DELETE /api/v1/mis-labores/{labor_id}` autenticado en `main.py`, 4 tests nuevos y app con botón "Eliminar" (acción destructiva en rojo) en cada labor de "Mi cuaderno", diálogo de confirmación "¿Eliminar esta labor?" con la labor identificada y el aviso "Se borrará de tu cuaderno. No se puede deshacer.", llamada `ServicioCuentas.eliminarLabor` y mensaje "Labor eliminada de tu cuaderno." con recarga del listado.
- Verificación ejecutada: `./init.sh` completo (pytest 74/74 con 4 tests nuevos de eliminación, healthcheck, datos sembrados con token, APK instalado y abierto) y verificación real desde la app en el emulador `OlivApp_API_34`: se creó una labor temporal por API (riego 15/12/2025, 2 horas, "Labor temporal para verificar el borrado.") y se eliminó desde la app con el diálogo de confirmación → desapareció del cuaderno con el mensaje visible y las demás labores intactas; comprobación por API: fuera de `GET /api/v1/mis-labores` y del detalle de la campaña 2025/2026, y segundo `DELETE` de la misma labor → 404 (borrado directo, sin papelera). Capturas en sesión (no versionadas).
- Evidencia capturada: `feature_list.json` (`eliminacion-de-labor` → `passing`, con evidencias).
- Ficheros o artefactos actualizados: `backend/app/labores.py` (`eliminar_labor`), `backend/app/main.py` (`DELETE /api/v1/mis-labores/{labor_id}`), `backend/tests/test_labores.py` (4 tests nuevos), `android/.../MainActivity.kt` (botón "Eliminar" + `BotonPeligro`, diálogo de confirmación, `ServicioCuentas.eliminarLabor`), `feature_list.json`, `PROGRESS.md`.
- Riesgo conocido o tema sin resolver: (1) el borrado es solo del lado del socio autenticado y definitivo (sin papelera, según `docs/domain-model.md`): el diálogo de confirmación es la única salvaguarda; (2) en la verificación se vio un dispositivo físico por USB en estado `authorizing` (pendiente de aceptar la depuración USB) además del emulador: posible móvil físico de la demo, pendiente de confirmar con el usuario; (3) la edición y eliminación se lanzan solo desde "Mi cuaderno" (el bloque del detalle de campaña sigue en solo consulta). Pendientes históricos: móvil físico de la demo, firma del APK release.
- Mejor siguiente paso: `aislamiento-de-datos-por-socio` (siguiente en orden de lista con dependencias cumplidas).
