# ADR 0005: Campos por categoría alineados con SIEX (modelo híbrido) y relleno conversacional del borrador

## Status
Accepted (2026-09-28). Desbloquea el punto "lista concreta de campos SIEX por categoría" de `docs/risks-and-open-questions.md`. Contrasta con fuentes oficiales (ver *Fuentes*).

## Context
OlivApp imita la estructura de campos del SIEX para facilitar un volcado posterior, sin exportación ni integración en el MVP. El usuario es un socio sénior (pocos pasos, sin tecnicismos) y la feature estrella extrae la ficha de la labor por voz, así que cada campo adicional multiplica el error del borrador IA. A la vez, el usuario pidió que el relleno sea **conversacional**: el agricultor no dicta la ficha completa a la primera, y es el asistente quien pregunta lo que falte hasta completarla.

Contraste con fuentes oficiales:
- El **mínimo obligatorio oficial** de registro de tratamientos fitosanitarios (FEGA) es: nombre de los productos fitosanitarios, plaga a controlar, fecha, dosis de aplicación, zona y cultivo, aplicador y, en su caso, asesor, equipos de aplicación y una valoración del tratamiento. El resto (nº de registro del producto, volumen de caldo, nº de aplicaciones, plazo de seguridad…) son campos opcionales del CUE.
- El **riego es actividad propia del CUE-SIEX** (registro de riego con parcela y, en su caso, comunidad de regantes y plantillas): la estructura de riego de OlivApp sí imita SIEX.
- La **cosecha** se registra también como tarea del CUE. Sus datos habituales: fecha, parcela, cantidad (kg), método/tipo de aceituna y destino (almazara).
- La interoperabilidad SIEX se apoya en más de 140 catálogos públicos de códigos: apoya el uso de listas cerradas en OlivApp.

## Decision
Modelo **híbrido**: núcleo de campos por categoría + `observaciones` opcional en toda labor + `plaga a controlar` obligatoria en tratamiento. Listas cerradas donde sea posible; el resto, texto libre con sugerencias.

Campos por categoría (además de fecha y parcela, comunes a toda labor):

| Categoría | Campos OlivApp | Tipo |
|---|---|---|
| Tratamiento fitosanitario | producto (texto libre con sugerencias), **plaga a controlar** (lista cerrada: mosca del olivo, repilo, prays, tuberculosis, cochinilla, malas hierbas, otra), cantidad + unidad (lista cerrada de unidades) | obligatorios |
| Riego | cantidad + unidad (horas, m³, L — lista cerrada) | obligatorio |
| Cosecha | modalidad (a vuelo / a suelo — lista cerrada, vocabulario del usuario), kilos | obligatorios |
| Toda labor | observaciones (texto libre: valoración del tratamiento u otros detalles) | opcional |

Ocultos a propósito y documentados en el mapeo como perfil o constante: cultivo (siempre olivar), aplicador (el socio, con su carné/ROPO en el perfil si procede), destino de la cosecha (la cooperativa única del socio), comunidad de regantes. Opcionales del CUE que quedan dentro de `observaciones`: volumen de caldo, nº de aplicaciones, equipos de aplicación, nº de registro del producto, plazo de seguridad.

**Relleno conversacional:** el flujo de voz es "extraer lo dictado → preguntar por los campos obligatorios que falten → confirmar". El asistente formula las preguntas de forma natural (una por respuesta) y el socio contesta por voz; el borrador IA se completa con cada respuesta. La confirmación manual sigue existiendo como red de seguridad.

## Fuentes
- FEGA / MAPA — *El Cuaderno Digital de Explotación Agrícola (CUE)*, nota informativa 2025: https://www.fega.gob.es/sites/default/files/files/document/2025.11.06-vf_nota_informativa_sobre_CUADERNO_DIGITAL_LIMPIO.pdf
- Manual CUE SIEX (uso del cuaderno digital interoperable con SIEX, riego, tratamientos y cosecha): https://memorandumsiex.com/wp-content/uploads/2026/02/MANUAL_CUE_SIEX_20260130_compressed.pdf

## Alternatives Considered
- **Mínimo documentado propio (sin plaga ni observaciones):** lo ya reflejado en `CONTEXT.md`; descartado porque omitía un campo obligatorio oficial (plaga a controlar) y carecía de válvula de escape para los campos opcionales del CUE.
- **Fidelidad SIEX completa por labor** (nº de registro, volumen de caldo, plazo de seguridad, superficie tratada, equipo ROMA, aplicador/carné por labor…): descartada; formularios inasumibles para el perfil sénior y extracción por voz demasiado frágil, para un MVP sin exportación donde la normativa no exige más que el mínimo.
- **Todo texto libre:** descartada por los errores de escritura y de extracción, y porque los catálogos SIEX apoyan vocabularios cerrados.

## Consequences
- La frase "estructura de campos SIEX" queda justificada: cubrimos el mínimo obligatorio oficial (más el núcleo de cosecha y riego del CUE) y documentamos cada omisión con su motivo.
- El borrador IA tiene 4-6 campos obligatorios como máximo por labor; los opcionales viven en `observaciones`, sin presión sobre la extracción.
- El flujo de voz pasa a ser conversacional (más de un turno): el pipeline de ADR 0004 sigue siendo válido, pero el alias `chat` también genera las preguntas por campo faltante. Añade una feature nueva al plan: `completar-borrador-por-preguntas`.
- La demo ante el tribunal gana un argumento: "IA propone, pregunta lo que falta y el socio confirma", que es el ciclo que mitiga el riesgo de precisión de transcripción/extracción.
- Si en el futuro se quisiera exportar a SIEX real, el mapeo de esta ADR es el punto de partida y los campos opcionales ya están conceptualmente ubicados.
