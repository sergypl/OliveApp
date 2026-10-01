# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `/Users/sergioperezlopez/Projects/OliveApp`
- Camino estándar de arranque: `./init.sh`
- Camino estándar de verificación: provisional — aún no hay bootstrap (pendiente tras elegir la primera feature)
- Siguiente feature lista actual: `bootstrap-backend`
- Bloqueo actual: ninguno (las 4 decisiones bloqueantes quedaron resueltas en ADR 0003-0006; ADR 0007 cerró el mapeo lab/industrial y el ajuste industrial por defecto; pendiente solo la validación empírica del pipeline de voz antes de implementar la feature de voz)
- Última verificación: 2026-10-01 (validación de JSON de `feature_list.json`, no del producto)

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
