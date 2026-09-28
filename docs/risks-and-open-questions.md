# Risks and Open Questions — OlivApp

## Blocking Next Phase
- Confirmar en el gateway (`inference.devexpert.io`) el endpoint exacto de transcripción de audio y el modelo de texto a usar, y validar con una nota de voz real en español grabada desde Android (formato, latencia, coste).
- Definir la lista concreta de campos SIEX por categoría (tratamiento / riego / cosecha) con una fuente: especificación oficial o mínimo razonable documentado.
- Definir el formato de los datos de campaña sembrados (kilos entregados, rendimiento graso, aceite obtenido) y de dónde salen los valores de la demo.
- Decidir dónde se despliega el backend (requisito: HTTPS + variables de entorno) y cómo se instala el APK en el dispositivo de demo.

## Implementation-Time Questions
- Modelo de campaña: ¿el año agrícola se introduce a mano, se deriva de la fecha de la labor o se hereda de la campaña activa?
- ¿Cómo se determina la "campaña actual" (fecha del sistema, campaña marcada como abierta en el backend)?
- Valor por defecto y editabilidad del ajuste industrial (p. ej. 5 %).
- Límite de duración de una nota de voz y formato de audio nativo.
- Resolución de fechas relativas ("ayer") y comportamiento cuando la fecha quede ambigua.
- Política mínima de contraseñas (sin reset por email en el MVP: limitación documentada).
- Almacenamiento seguro del token en Android (EncryptedSharedPreferences/Keystore).
- Prefill del simulador con los kilos de la campaña o con los kilos entregados a la cooperativa (opcional; solo si es barato).
- Elección de backend, base de datos y hosting.

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
- **Salto a app nativa:** build, firma, Gradle y dispositivo real añaden fricción frente a la web del plan anterior; mitigar fijando un único dispositivo objetivo y un APK de desarrollo cuanto antes.
- Precisión de transcripción/extracción con acentos y vocabulario local: mitigada por el ciclo "IA propone, socio confirma" y el set de frases de prueba.
- Dependencia del micrófono y permisos en Android (versiones de SO, fabricantes): probar en el móvil real de la demo cuanto antes.
- Datos de cooperativa sembrados que no cuadren con la realidad: mitigado declarándolos como datos de demo y de solo lectura.
- Sincronización offline: conflictos o doble confirmación al reconectar podrían duplicar labores; idempotencia básica al confirmar.
- Seguridad: la clave API solo en el servidor; token JWT con caducidad y transporte HTTPS.
- RGPD: datos personales mínimos (email) y datos de campaña del socio. Sin baja de cuenta en el MVP: limitación conocida que habrá que declarar.
- Alcance académico: la app no estará validada con usuarios reales en el MVP (aceptado y declarado); el replanteo a nativa + campañas amplía el alcance frente al plan inicial.

## Research Tasks
- Probar el endpoint de audio del gateway con una nota de voz real en español grabada en Android (formato, latencia, coste).
- Buscar la estructura de campos del SIEX / cuaderno digital por categoría de labor.
- Definir un juego realista de campañas y datos oficiales sembrados para la demo.
- Preparar el set de 10-20 frases de prueba para la demo (productos, unidades, "a vuelo", "a suelo").
