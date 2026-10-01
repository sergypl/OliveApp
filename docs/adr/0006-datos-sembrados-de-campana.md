# ADR 0006: Datos sembrados de campaña — formato y valores de la demo

## Status
Accepted (2026-09-28). Desbloquea el último punto de `docs/risks-and-open-questions.md` ("formato de los datos de campaña sembrados y origen de los valores"). Los valores calibrados aquí son sustituibles por datos reales anonimizados sin cambiar el modelo.

## Context
Los datos oficiales de la cooperativa (kilos entregados, rendimiento graso y aceite obtenido por socio y campaña) son de solo lectura en el MVP y entran sembrados: no hay administración de cooperativa (non-goal declarado). Hacen falta datos creíbles para la demo ante el tribunal, coherentes entre sí, con volumen suficiente para mostrar campaña actual + anteriores y para verificar el aislamiento entre socios.

Anclaje con datos reales consultados:
- Informe REDES TECO del MAPA (olivar de almazara, campaña 2024/2025): producciones de 0,02 a 1,12 t/ha según sistema y sequía; ejemplo de productor que entrega 4 t de aceituna con 18 % de rendimiento.
- Liquidaciones publicadas por agricultores (campañas 23/24): rendimientos grasos de 14,24 % (aceituna de vuelo, Jaén) y 19 % (cornicabra, Toledo); año malo de sequía vs. año récord como variación normal entre campañas.

## Decision
1. **Formato:** un **fichero de seed versionado en el repo (JSON)** con cooperativa, campañas, socios, datos oficiales y parcelas, más un **script del backend** que lo carga si la base está vacía (revisable y reejecutable).
2. **Volumen:** 1 cooperativa, **4 campañas** (2022/2023 → 2025/2026), **2 socios** con datos propios y 3-4 parcelas sembradas cada uno. La campaña **actual es 2025/2026, con datos oficiales completos**; las anteriores son cerradas y de solo consulta.
3. **Valores:** dataset inventado pero calibrado con los datos reales anteriores. Regla de coherencia declarada: **aceite obtenido = round(kilos × rendimiento graso)**, en kg. Serie no plana: año malo (sequía), año medio, año bueno (récord) y año actual normalizado.

| Campaña | Socio 1 (≈5 ha secano) — kg / rend. / aceite kg | Socio 2 (≈2 ha) — kg / rend. / aceite kg |
|---|---|---|
| 2022/2023 (sequía) | 2.650 / 15,4 % / 408 | 1.200 / 16,0 % / 192 |
| 2023/2024 (media) | 4.100 / 18,2 % / 746 | 1.850 / 18,8 % / 348 |
| 2024/2025 (buena) | 6.200 / 21,4 % / 1.327 | 2.750 / 20,6 % / 567 |
| 2025/2026 (actual) | 4.850 / 19,6 % / 951 | 2.100 / 19,2 % / 403 |

4. Los datos se declaran como **datos de demo** en la propia app y documentación. Si más adelante se consiguen **datos reales anonimizados** (p. ej. una hoja de liquidación), solo se sustituyen los valores del fichero de seed sin tocar el modelo.

## Fuentes
- MAPA — *Informe de resultados REDES TECO Olivar de Almazara 2025*: https://www.mapa.gob.es/dam/mapa/contenido/agricultura/temas/producciones-agricolas/frutas-y-hortalizas/aceite-de-oliva-y-aceituna-de-mesa/redes-teco-olivar/infomes/informe-resultados-redesteco-olivar-almazara-2025.pdf
- Liquidaciones publicadas por agricultores, campaña 23/24 (Foro del Olivar, Jaén/Toledo).

## Alternatives Considered
- **Datos reales anonimizados desde el principio:** máxima credibilidad, pero exige conseguir la documentación antes del seed. Se adopta como mejora posterior opcional, compatible con el formato elegido.
- **Migración SQL inicial:** directa, pero poco legible y frágil de tocar. Descartada.
- **Endpoint o pantalla de carga de datos de cooperativa:** contradice el non-goal de administración de cooperativa. Descartada.
- **Dataset más pequeño (3 campañas, 2 socios mínimos):** cubría la demo, pero dejaba el panel pobre y la variabilidad entre campañas poco creíble. Descartado.

## Consequences
- El seed es revisable por el tribunal: puede abrirse el JSON y comprobar que las cifras cuadran con la regla de coherencia.
- `aislamiento-de-datos-por-socio` queda verificable con los dos socios del seed; `consulta-campanas-anteriores`, con las 3 campañas cerradas.
- Las parcelas sembradas permiten ensayar la demo del dictado sin dar de alta parcelas a mano.
- La elección de "2025/2026 como campaña actual" es una simplificación de demo (el calendario real de campañas se puede desplazar si el tribunal pregunta): queda declarada aquí.
- Cambiar a datos reales anonimizados es un commit de un solo fichero (el de seed) más el ajuste de la regla de coherencia si la fuente redondea distinto.

## Actualización (2026-10-01)
La [ADR 0007](0007-mapeo-laboratorio-industrial-y-ajuste-real.md) sustituye la regla de coherencia por su versión en dos etapas (merma industrial del 9,7 %), recalcula la tabla del dataset y sustituye la fila 2025/2026 del socio 1 por datos reales anonimizados extraídos de [la referencia de producto](../referencia-sb-portal-socios.md).
