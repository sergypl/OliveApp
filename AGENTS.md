# Instrucciones para agentes — OlivApp

OlivApp es una app Android nativa (Kotlin + Jetpack Compose) para socios de cooperativa oleícola: panel de campañas con datos oficiales de la cooperativa, cuaderno de campo con registro por voz asistido por IA y simulador de liquidación en limpio.

## Leer primero

- `CONTEXT.md` — lenguaje del dominio (glosario y términos rechazados).
- `docs/build-brief.md` — problema, objetivos y alcance del MVP.
- `docs/domain-model.md` — entidades, relaciones, estados y casos borde.
- `docs/risks-and-open-questions.md` — riesgos y preguntas abiertas.

Leer solo cuando sea relevante:

- `docs/technical-discovery.md` — stack, backend, gateway de IA, auth, despliegue y verificación.
- `DESIGN.md` — principios de interfaz y usabilidad sénior.
- `docs/adr/` — decisiones aceptadas (p. ej. ADR 0002: app Android nativa).

## Flujo de arranque

Antes de escribir código:

1. Confirmar el directorio de trabajo con `pwd`.
2. Leer `PROGRESS.md` para el estado verificado actual y el siguiente paso.
3. Leer `feature_list.json` y elegir la primera feature sin terminar que esté lista (dependencias cumplidas), en orden de lista.
4. Ejecutar `./init.sh`.
5. Si la verificación base falla, arreglar la base antes de añadir trabajo de features.

## Reglas de trabajo

- Trabajar una feature cada vez.
- No marcar una feature como completada solo porque se añadió código.
- Mantener los cambios dentro del alcance de la feature elegida, salvo un arreglo auxiliar estrecho por bloqueo.
- No cambiar en silencio las reglas de verificación durante la implementación.
- Actualizar los artefactos duraderos del repo en lugar de fiarse de resúmenes de chat.
- Usar el vocabulario de `CONTEXT.md` en código, UI y documentación.

## Artefactos obligatorios

- `feature_list.json`: fuente de verdad del estado de features.
- `PROGRESS.md`: estado verificado actual y registro ligero de sesiones.
- `init.sh`: camino estándar de arranque y verificación.

## Definición de terminado

Una feature está terminada solo cuando:

- el comportamiento objetivo está implementado,
- la verificación requerida se ejecutó realmente,
- la evidencia queda registrada en `feature_list.json` o `PROGRESS.md`,
- el repo sigue siendo reiniciable desde el camino estándar de arranque,
- los docs relevantes están actualizados si cambió el comportamiento del producto, reglas de dominio, API o verificación.

## Fin de sesión

Antes de terminar una sesión:

1. Actualizar `PROGRESS.md`.
2. Actualizar `feature_list.json`.
3. Registrar riesgos o bloqueos sin resolver.
4. Dejar el repo lo bastante limpio para que la siguiente sesión ejecute `./init.sh` sin más.
