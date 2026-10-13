# Risks and Open Questions — OlivApp

## Blocking Next Phase
Ninguna: las cuatro decisiones bloqueantes quedaron resueltas en [ADR 0003](adr/0003-backend-paas-gratuito-y-apk-por-archivo.md) a [ADR 0006](adr/0006-datos-sembrados-de-campana.md). El proyecto puede entrar en bootstrap técnico e implementación.

## Decided
- Despliegue del backend e instalación del APK: resuelto en [ADR 0003](adr/0003-backend-paas-gratuito-y-apk-por-archivo.md). Backend en PaaS gratuito con HTTPS y secretos en variables de entorno (plan B: local + túnel `cloudflared`/`ngrok`); APK release firmado distribuido por archivo en la demo (plan B y desarrollo: `adb install`). La forma concreta de levantar el backend puede revisarse sin cambiar esta decisión. Pendiente de verificar al elegir proveedor: cuota y tiempo de sleep/cold start del free tier.
- Endpoint de audio y modelo de texto del gateway: resuelto en [ADR 0004](adr/0004-transcripcion-y-extraccion-via-gateway.md). Transcripción con alias `transcribe` y extracción con alias `chat` (salida JSON forzada), subiendo a `chat-pro` solo para extracción si falla el set de frases de prueba. **Validado empíricamente (2026-10-09):** formato nativo de Android aceptado, JSON forzado soportado, 1,7 s la transcripción y 5,7 s la extracción (datos en el ADR).
- Campos SIEX por categoría de labor: resuelto en [ADR 0005](adr/0005-campos-por-categoria-alineados-siex.md). Modelo híbrido (núcleo por categoría + plaga a controlar en tratamiento + observaciones opcionales), listas cerradas donde sea posible y relleno conversacional del borrador IA, contrastado con fuentes oficiales (FEGA y Manual CUE SIEX).
- Datos de campaña sembrados: resuelto en [ADR 0006](adr/0006-datos-sembrados-de-campana.md). Fichero de seed versionado (JSON) + script de carga, 4 campañas (actual 2025/2026 con datos completos), 2 socios con parcelas, valores calibrados con datos reales y regla de coherencia `aceite = round(kilos × rendimiento graso)`.
- Distinción laboratorio/industrial y ajuste industrial: resueltos en [ADR 0007](adr/0007-mapeo-laboratorio-industrial-y-ajuste-real.md). Rendimiento graso = laboratorio, aceite obtenido = industrial, merma industrial del 9,7 % en el dataset (regla de coherencia en dos etapas) y ajuste industrial del simulador por defecto 10 % (medido en la cooperativa real de la [referencia de producto](referencia-sb-portal-socios.md)). El socio 1 de la campaña 2025/2026 usa datos reales anonimizados.

## Implementation-Time Questions
- Modelo de campaña: resuelto en la feature `alta-manual-labor` (la campaña de una labor se deriva de su fecha: la campaña oleícola va del 1 de noviembre al 31 de octubre, p. ej. 15/12/2025 → 2025/2026). Una fecha fuera de toda campaña conocida o de una campaña cerrada rechaza el alta, con mensaje claro.
- ¿Cómo se determina la "campaña actual"?: resuelto en la feature `panel-campana-actual` (es la campaña marcada como actual en los datos de la cooperativa; no se deriva de la fecha del sistema, para que la demo siempre muestre la campaña sembrada).
- Valor por defecto y editabilidad del ajuste industrial: resueltos en [ADR 0007](adr/0007-mapeo-laboratorio-industrial-y-ajuste-real.md) (por defecto 10 %, editable).
- Límite de duración de una nota de voz y formato de audio nativo: resueltos en la feature `grabacion-de-nota-voz` (máximo 60 segundos con parada automática y aviso, MPEG-4 con audio AAC a 16 kHz, fichero `.m4a`; cola local en el dispositivo con estado `pendiente` hasta que la transcripción la envíe al backend; las grabaciones de menos de 1 segundo se descartan pidiendo repetir).
- Resolución de fechas relativas ("ayer") y comportamiento cuando la fecha quede ambigua.
- Política mínima de contraseñas: resuelta en la feature `registro-cuenta-socio` (mínimo 8 caracteres, email único sin distinguir mayúsculas, hasheado PBKDF2-HMAC-SHA256 con sal). Sin reset por email en el MVP: limitación documentada.
- Almacenamiento seguro del token en Android: resuelto en la feature `inicio-de-sesion` (EncryptedSharedPreferences con clave del Keystore del dispositivo, AES-256; token JWT HS256 con caducidad de 30 días). Nota operativa: `JWT_SECRET` debe tener 32 bytes o más (recomendación RFC 7518 para HS256).
- Vinculación cuenta ↔ socio del seed: resuelto definitivamente en la feature `aislamiento-de-datos-por-socio`: el registro pide el **código de socio** que da la cooperativa (en el seed: "Socio 1", "Socio 2") y la cuenta queda vinculada a ese socio, con error claro si el código no existe. Las cuentas creadas antes (sin código) conservan su vínculo a `socio-001` por la migración de la base.
- Pantalla "Crear cuenta" sin salida tras un error (observado en la verificación de `aislamiento-de-datos-por-socio`): **resuelto** como arreglo auxiliar en `cola-offline-de-notas`, con el botón "Volver al inicio" siempre visible en "Crear cuenta" e "Iniciar sesión" (verificado en el móvil físico).
- Prefill del simulador con los kilos de la campaña o con los kilos entregados a la cooperativa (opcional; solo si es barato).
- Elección concreta de base de datos dentro del PaaS elegido (ver ADR 0003). El backend ya está elegido: FastAPI/Python con **SQLite** para desarrollo y MVP (feature `datos-sembrados-cooperativa`, ruta configurable por `OLIVAPP_DB`). Pendiente al desplegar: confirmar persistencia del fichero en el free tier (disco efímero en algunos PaaS; si no, PostgreSQL gratuito del propio PaaS).

## Later / Not MVP
- Administración por parte de la cooperativa (rol y pantallas propios) e importación real de sus datos.
- Comparativa avanzada entre "lo que apunté" y "lo que dice la cooperativa" (desviaciones, avisos).
- Métrica de precisión de extracción sobre un set de dictados (extensión académica voluntaria).
- Prueba de usabilidad con un socio real (perfil sénior en campo).
- Exportación o integración con SIEX; iOS; multi-cooperativa; roles adicionales; explotaciones compartidas; liquidación real desglosada; más categorías de labor.

## Assumptions
- La demo se hace en un lugar con WiFi o cobertura (la cola offline cubre el campo, no el aula).
- Existe una única cooperativa sembrada y un puñado de campañas de ejemplo para la demo.
- El socio maneja un número pequeño de parcelas; el MVP no modela explotaciones ni límites.
- El tribunal valora la demo funcional y la claridad de la tesis más que la robustez industrial.
- El vocabulario del dominio fue confirmado con el usuario (ver `CONTEXT.md`).

## Risks
- **Salto a app nativa:** build, firma, Gradle y dispositivo real añaden fricción frente a la web del plan anterior; mitigar fijando un único dispositivo objetivo y un APK de desarrollo cuanto antes. **Mitigado (2026-10-13):** el APK de desarrollo compila, se instala y se abre también en el móvil físico de la demo, fijado como **Xiaomi Redmi Note 10 Pro (M2101K6G)** y usado ya como dispositivo de verificación; queda verificar la firma del APK release. Nota operativa: la app llama al backend por `127.0.0.1:8000` y `init.sh` configura `adb reverse tcp:8000 tcp:8000` en cada dispositivo conectado (si se conecta un dispositivo después, hay que lanzar ese comando a mano).
- Precisión de transcripción/extracción con acentos y vocabulario local: mitigada por el ciclo "IA propone, socio confirma" y el set de frases de prueba.
- Dependencia del micrófono y permisos en Android (versiones de SO, fabricantes): probar en el móvil real de la demo cuanto antes.
- Datos de cooperativa sembrados que no cuadren con la realidad: mitigado declarándolos como datos de demo y de solo lectura.
- Sincronización offline: conflictos o doble confirmación al reconectar podrían duplicar labores; idempotencia básica al confirmar.
- Seguridad: la clave API solo en el servidor; token JWT con caducidad y transporte HTTPS.
- RGPD: datos personales mínimos (email) y datos de campaña del socio. Sin baja de cuenta en el MVP: limitación conocida que habrá que declarar.
- Alcance académico: la app no estará validada con usuarios reales en el MVP (aceptado y declarado); el replanteo a nativa + campañas amplía el alcance frente al plan inicial.

## Research Tasks
- ~~Validar el pipeline de voz según ADR 0004~~: **hecho (2026-10-09)**, ver *Validación empírica* en el ADR (formato, latencia, coste y JSON forzado comprobados).
- Preparar el set de 10-20 frases de prueba para la demo (productos, unidades, "a vuelo", "a suelo", plaga a controlar).
- (Opcional) Ampliar los datos reales anonimizados del seed con más campañas del socio real (ADR 0007 ya incorporó la 2025/2026).
