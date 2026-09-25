# Risks and Open Questions — OlivApp

## Blocking Next Phase
- Confirmar en el gateway (`inference.devexpert.io`) el endpoint exacto de transcripción de audio y el modelo de texto a usar, y validar con una nota de voz real en español.
- Definir la lista concreta de campos SIEX por categoría (tratamiento / riego / cosecha) con una fuente: especificación oficial o mínimo razonable documentado.
- Decidir dónde se despliega la demo (requisito: HTTPS + variables de entorno).

## Implementation-Time Questions
- Valor por defecto y editabilidad del ajuste industrial (p. ej. 5 %).
- Límite de duración de una nota de voz y formato de audio.
- Resolución de fechas relativas ("ayer") y comportamiento cuando la fecha quede ambigua.
- Política mínima de contraseñas (sin reset por email en el MVP: limitación documentada).
- Prefill del simulador con los kilos de cosecha registrados en el cuaderno (opcional; solo si es barato).
- Elección de framework, base de datos y hosting.

## Later / Not MVP
- Métrica de precisión de extracción sobre un set de dictados (extensión académica voluntaria).
- Prueba de usabilidad con un agricultor real (perfil sénior en campo).
- Exportación o integración con SIEX; app nativa; roles adicionales; explotaciones compartidas; liquidación real desglosada; más categorías de labor; modo PWA instalable.

## Assumptions
- La demo se hace en un lugar con WiFi o cobertura (la cola offline cubre el campo, no el aula).
- El agricultor maneja un número pequeño de parcelas; el MVP no modela explotaciones ni límites.
- El tribunal valora la demo funcional y la claridad de la tesis más que la robustez industrial.
- El vocabulario del dominio fue confirmado con el usuario (ver `CONTEXT.md`).

## Risks
- Precisión de transcripción/extracción con acentos y vocabulario local: mitigada por el ciclo "IA propone, agricultor confirma" y el set de frases de prueba.
- Dependencia del navegador para el micrófono (permisos, diferencias iOS/Android): probar en el móvil real de la demo cuanto antes.
- Seguridad: la clave API solo en el servidor; cookies de sesión con HTTPS.
- Doble confirmación de un borrador en cola offline podría duplicar labores: idempotencia básica al confirmar.
- RGPD: datos personales mínimos (email). Sin baja de cuenta en el MVP: limitación conocida que habrá que declarar.
- Alcance académico: la app no estará validada con usuarios reales en el MVP (aceptado y declarado).

## Research Tasks
- Probar el endpoint de audio del gateway con una nota de voz real en español (formato, latencia, coste).
- Buscar la estructura de campos del SIEX / cuaderno digital por categoría de labor.
- Preparar el set de 10-20 frases de prueba para la demo (productos, unidades, "a vuelo", "a suelo").
