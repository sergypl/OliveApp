# Technical Discovery — OlivApp

## Product Surface
- Aplicación **Android nativa** (Kotlin + Jetpack Compose), instalada o ejecutada desde Android Studio en un dispositivo real (ADR 0002). No hay versión web.
- **Backend ligero obligatorio:** el APK no puede llevar la clave de API de IA. El backend actúa como proxy de las llamadas de IA (protege la clave) y expone la API de datos (auth + entidades).
- La grabación de audio y el micrófono son APIs nativas de Android; no dependen del navegador ni de HTTPS para el permiso (aunque el backend sí debe servirse por HTTPS en la demo).

## Candidate Stack
- App Android: Kotlin, Jetpack Compose (Material 3), Android Studio + Gradle.
- Cliente de red: Retrofit/OkHttp o Ktor Client; serialización JSON (kotlinx.serialization).
- Persistencia local: Room (cuaderno, campañas cacheadas y cola de notas pendientes).
- Backend/API: servicio ligero separado (FastAPI/Python, elegido en la feature `bootstrap-backend`) o equivalente; autenticación por token (JWT).
- Base de datos del servidor: relacional ligera según hosting disponible (PostgreSQL/SQLite).
- La elección concreta de backend y hosting queda abierta para la fase de planificación (no es parte del build brief).

## Data and Storage
- Entidades en servidor: Usuario (email, hash de contraseña, cooperativa), Cooperativa, Campaña (datos oficiales por socio: kilos entregados, rendimiento graso, aceite obtenido), Parcela (socio, nombre), Labor (parcela, campaña, fecha, categoría, campos por categoría, origen voz/manual), Nota de voz (transcripción, estado).
- Persistencia local en el dispositivo (Room): labores y campañas cacheadas para consulta offline; notas de voz pendientes hasta que haya conexión.
- Aislamiento: todas las consultas filtradas por el socio autenticado; los datos oficiales de campaña son de solo lectura y también filtrados por socio.
- Datos de cooperativa sembrados: para la demo se cargan campañas y sus datos oficiales por socio; no hay escritura desde la app.
- Audio: se graba en el dispositivo y se envía al backend solo para transcribir. Stance mínima: borrar el audio del servidor tras transcribir; en cola offline vive en el dispositivo.

## Integrations
- Gateway de inferencia compatible con OpenAI (`inference.devexpert.io`, clave `DEVEXPERT_API_KEY` solo en servidor; ver ADR 0004 para la elección de aliases):
  - **Transcripción:** audio grabado en Android (formato a confirmar en validación, candidato `m4a`) → texto en español, alias `transcribe` (`/audio/transcriptions`).
  - **Extracción:** alias `chat` (`/chat/completions`) con salida JSON forzada (fecha, parcela, categoría, campos de la categoría), apoyándose en la lista de parcelas conocidas del socio para desambiguar; `chat-pro` como escalón de calidad solo para extracción.
- Sin más integraciones externas en el MVP (SIEX es solo estructura de campos; la cooperativa no se integra, sus datos se siembran).

## Authentication and Authorization
- Registro/login con email + contraseña; el backend emite un token (JWT) que la app guarda en almacenamiento seguro del dispositivo.
- Stance mínima: contraseñas hasheadas, token con caducidad y cierre de sesión. Fuera del MVP: reset por email, verificación de email, 2FA, baja de cuenta.
- `GET /api/v1/mi-perfil` (módulo `backend/app/perfil.py`, requiere token): devuelve `{codigo_socio, email, cooperativa, numero_parcelas, campana_actual}` del socio de la cuenta del token (solo lectura; el socio se deduce del token, nunca de la petición, así un socio no ve los datos de otro). Lo consume la pestaña Perfil de la app.
- Un único rol (socio). No hay rol de cooperativa ni permisos diferenciados.

## Deployment and Operations
- El backend se despliega con HTTPS y URL pública; variables de entorno para la clave API y el secreto de firma del token. App: APK de desarrollo para la demo en dispositivo.
- Day one: lo opera el propio proyecto (estudiante del curso). La cola offline y los errores deben ser visibles en la UI para depurar sin observabilidad avanzada.

## Testing and Verification
- Flujo voz→labor con un set escrito de frases de prueba (10-20 dictados representativos del vocabulario local).
- Simulador: casos con valores conocidos verificados a mano.
- Panel de campañas: datos oficiales sembrados se muestran correctamente y separados de las labores del socio.
- Aislamiento de datos: un socio no puede ver datos de otro.

## Observability
- Mínimo viable: errores de transcripción/extracción registrados y estados de la cola visibles en la interfaz. Sin APM ni telemetría.

## Constraints
- Cobertura móvil limitada en el campo → cola offline local obligatoria (nota pendiente, transcribir al reconectar).
- Coste por uso del gateway: grabaciones cortas y solo 3 categorías acotan el consumo.
- Dispositivos sénior: rendimiento y legibilidad en móviles modestos (ver `DESIGN.md`). La app nativa exige gestionar build, firma y dispositivo para la demo.
- Alcance académico: un solo desarrollador, un solo dispositivo objetivo de demo.
