# Technical Discovery — OlivApp

## Product Surface
- Aplicación web responsive, móvil-first, accesible desde el navegador (ADR 0001). Sin tienda de apps.
- El micrófono del navegador exige contexto seguro (HTTPS o localhost): la demo debe servirse con HTTPS.
- Backend ligero para: servir la web, actuar como proxy de las llamadas de IA (protege la clave API) y exponer la API de datos (auth + entidades).

## Candidate Stack
- Frontend: framework web moderno con componentes (p. ej. Next.js/React, Vue/Nuxt o equivalente).
- Backend/API: rutas de servidor del propio framework (p. ej. Next.js API routes) o un servicio ligero separado (Node/Express, FastAPI...).
- Base de datos: relacional ligera según hosting disponible (PostgreSQL/SQLite).
- La elección concreta de stack queda abierta para la fase de planificación (no es parte del build brief).

## Data and Storage
- Entidades: Usuario (email, hash de contraseña), Parcela (agricultor, nombre), Labor (parcela, fecha, categoría, campos por categoría, origen voz/manual), Nota de voz (audio temporal, transcripción, estado).
- Aislamiento: todas las consultas filtradas por el usuario autenticado; nada global ni compartido.
- Audio: se almacena temporalmente hasta la transcripción (en cola offline vive en el dispositivo). Stance mínima: borrar el audio del servidor tras transcribir.

## Integrations
- Gateway de inferencia compatible con OpenAI (`inference.devexpert.io`, clave `DEVEXPERT_API_KEY` solo en servidor):
  - **Transcripción:** audio del navegador (p. ej. webm/opus vía MediaRecorder) → texto en español.
  - **Extracción:** modelo de texto con salida JSON forzada (fecha, parcela, categoría, campos de la categoría), apoyándose en la lista de parcelas conocidas del usuario para desambiguar.
- Sin más integraciones externas en el MVP (SIEX es solo estructura de campos).

## Authentication and Authorization
- Registro/login con email + contraseña; sesión por cookie de navegador.
- Stance mínima: contraseñas hasheadas y cierre de sesión. Fuera del MVP: reset por email, verificación de email, 2FA, baja de cuenta.

## Deployment and Operations
- Hosting a decidir (implementation-time). Requisitos de la demo: HTTPS, URL pública, variables de entorno para la clave API.
- Day one: lo opera el propio proyecto (estudiante del curso). La cola offline y los errores deben ser visibles en la UI para depurar sin observabilidad avanzada.

## Testing and Verification
- Flujo voz→labor con un set escrito de frases de prueba (10-20 dictados representativos del vocabulario local).
- Simulador: casos con valores conocidos verificados a mano.
- Aislamiento de datos: un usuario no puede ver datos de otro.

## Observability
- Mínimo viable: errores de transcripción/extracción registrados y estados de la cola visibles en la interfaz. Sin APM ni telemetría.

## Constraints
- Cobertura móvil limitada en el campo → cola offline simple obligatoria (nota pendiente, transcribir al reconectar).
- Coste por uso del gateway: grabaciones cortas y solo 3 categorías acotan el consumo.
- Dispositivos sénior: rendimiento y legibilidad en móviles modestos (ver `DESIGN.md`).
