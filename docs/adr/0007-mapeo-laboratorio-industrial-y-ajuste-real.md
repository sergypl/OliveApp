# ADR 0007: Mapeo laboratorio/industrial en los datos de campaña y ajuste industrial real

## Status
Accepted (2026-10-01). Completa la [ADR 0006](0006-datos-sembrados-de-campana.md) (dataset y regla de coherencia actualizados aquí) y resuelve la pregunta abierta "valor por defecto y editabilidad del ajuste industrial". Basada en datos reales observados en [la referencia de producto](../referencia-sb-portal-socios.md).

## Context
Una cooperativa real registra, por cada entrega, **dos rendimientos y dos aceites**: de laboratorio (análisis de la muestra) e industrial (lo que efectivamente se recibe tras la merma del proceso). Ejemplos reales: 17,893561 % lab → 16,156399 % industrial y 18,08 % lab → 16,324 % industrial; en ambos casos la merma relativa es **9,71 %**.

El modelo del MVP (ADR 0006) tiene solo tres datos oficiales por campaña: kilos entregados, rendimiento graso y aceite obtenido. El simulador ya contempla un "ajuste industrial" configurable cuyo valor por defecto estaba por decidir (se barajaba 5 %).

## Decision
1. **Mapeo declarado sin ampliar el modelo:** *rendimiento graso* = **rendimiento de laboratorio** (el que el socio conoce del análisis) y *aceite obtenido* = **aceite industrial** (lo que realmente se recibe). El matiz se declara en `CONTEXT.md`.
2. **Regla de coherencia del seed en dos etapas** (sustituye a la de la ADR 0006): `aceite obtenido = round(kilos × rendimiento graso × (1 − merma industrial))`, con **merma industrial del 9,7 %** en el dataset (dato real). Que `kilos × rendimiento` no coincida con el aceite no es un error: es la merma real, y es justo lo que explica el simulador.
3. **Valor por defecto del ajuste industrial del simulador: 10 %** (≈ merma real del 9,7 %), **editable** por el socio en el simulador. Así la liquidación en limpio de un año conocido cuadra con el aceite industrial real de la cooperativa.

Dataset de la ADR 0006 recalculado con la nueva regla (fila 2025/2026 del socio 1 sustituida por sus valores reales anonimizados: 5 vales, 10.958 kg):

| Campaña | Socio 1 — kg / rdto lab / aceite industrial kg | Socio 2 — kg / rdto lab / aceite industrial kg |
|---|---|---|
| 2022/2023 (sequía) | 2.650 / 15,4 % / 369 | 1.200 / 16,0 % / 173 |
| 2023/2024 (media) | 4.100 / 18,2 % / 674 | 1.850 / 18,8 % / 314 |
| 2024/2025 (buena) | 6.200 / 21,4 % / 1.198 | 2.750 / 20,6 % / 512 |
| 2025/2026 (actual) | **10.958 / 17,93 % / 1.774** *(real, anonimizado)* | 2.100 / 19,2 % / 364 |

## Alternatives Considered
- **Ampliar el modelo a cuatro datos** (rdto lab, aceite lab, rdto ind, aceite ind): más fiel a la cooperativa real, pero toca modelo, panel y seed sin aportar al MVP, donde el simulador ya explica la diferencia. Descartado.
- **Mapear todo al rendimiento industrial** para que la regla `aceite = kilos × rendimiento` se cumpliese a simple vista: descartado porque el socio real identifica su "rendimiento" con el del análisis de laboratorio, y porque escondería la merma que el simulador quiere explicar.
- **Ajuste industrial por defecto 5 % (valor inicial barajado):** descartado por los datos: la merma real medida es del 9,7 %.

## Consequences
- El simulador tiene una historia de producto sólida para el tribunal: "el ajuste industrial replica la merma real que vemos entre el análisis y lo que recibe el socio" (10 % por defecto, medido en una cooperativa real).
- Un observador que multiplique `kilos × rendimiento` verá una diferencia con el aceite mostrado: queda **declarado** en `CONTEXT.md` y en esta ADR como merma industrial, no como error.
- Si el tribunal pregunta por los números: el dataset del socio 1 de la campaña 2025/2026 son datos reales anonimizados; el resto, calibrado.
- La regla de coherencia del seed queda en dos etapas; el seed debe calcular (o traer) el aceite industrial, no el de laboratorio.
