# Build Brief — OlivApp

Proyecto práctico para un curso de Inteligencia Artificial. Discovery en español.

## Problem
Los olivareros —con un perfil mayoritario sénior y poca afinidad tecnológica— deben mantener el cuaderno de campo de sus parcelas (tratamientos fitosanitarios, riegos, cosecha). Hoy ese registro se hace en papel en el campo y se transcribe después a mano en SIEX: es lento, se olvida y se pospone. Además, antes de llevar la aceituna a la almazara, el agricultor no tiene una forma rápida de saber cuánto aceite puede esperar de su cosecha y qué deducción industrial le aplicarán.

La tesis del proyecto: la IA (transcripción por voz + extracción estructurada) reduce la fricción del registro en el campo, y un simulador simple da visibilidad inmediata de la rentabilidad esperada.

## Current Workaround / Existing System
- Cuaderno de campo en papel en el campo; transcripción manual posterior a SIEX desde un ordenador.
- Cálculos mentales u hojas de cálculo sueltas para estimar el rendimiento graso.

## Target Users
- Agricultor de olivar (España, mayoritariamente Andalucía), perfil sénior: texto grande, alto contraste, pocos pasos, sin tecnicismos.
- Cada agricultor registrado ve y edita solo sus propios datos. Acceso: registro y login con email + contraseña; sin roles ni explotaciones compartidas en el MVP.
- El tribunal del curso es evaluador, no usuario de la aplicación.

## Goals
1. Validar el uso de IA para capturar labores por voz en el campo: dictar → ficha estructurada (estructura SIEX) → confirmación con un toque.
2. Ofrecer un cuaderno de campo simple, cronológico y legible, con formulario manual auxiliar para correcciones.
3. Estimar la liquidación en limpio: kilos × rendimiento graso − ajuste industrial configurable.
4. Cumplir la usabilidad sénior: micrófono protagonista, alto contraste, texto grande, sin menús complejos.
5. Mantener un alcance de desarrollo acotado a un entorno académico.

## Non-Goals
- Integración o exportación real con SIEX (solo se imita su estructura de campos).
- Sensores IoT en tiempo real (almazara o maquinaria) ni mapas satelitales / teledetección.
- Módulos B2B de gestión de bodega o básculas de cooperativa.
- App nativa de tienda (se decide web responsive: ADR 0001) ni offline completo con transcripción local.
- Roles técnicos, explotaciones compartidas, pagos o suscripciones.
- Categorías de labor adicionales (poda, abonado, siega...) y liquidación real desglosada (humedad, impurezas, costes de molturación).

## MVP Slice
Aplicación web responsive (navegador móvil-first) con backend ligero para proteger la clave de la API de IA.

1. **Registro por voz (feature estrella).** Botón de micrófono grande en el inicio → grabación corta → si no hay cobertura queda como nota pendiente → transcripción vía gateway de inferencia → el LLM extrae un JSON {fecha, parcela, categoría, campos de la categoría} → borrador IA → confirmar/corregir con un toque → labor en el cuaderno. Categorías: tratamiento fitosanitario, riego, cosecha (a vuelo / a suelo).
2. **Cuaderno de campo.** Historial cronológico de labores con campos tipo SIEX; formulario manual auxiliar para crear o corregir registros.
3. **Simulador de liquidación en limpio.** Kilos de aceituna × % rendimiento graso (entrada del usuario) − % ajuste industrial configurable → aceite neto estimado.
4. **Acceso.** Registro y login con email + contraseña; datos privados por usuario.

## Validation Plan
Demo end-to-end ante el tribunal: dictar una labor real por voz, confirmar el borrador IA y ver la labor en el cuaderno; mostrar el resultado del simulador en al menos un caso con valores verificados a mano. (Métricas de precisión y prueba con agricultor real quedan como extensión voluntaria; ver riesgos.)

## Success Criteria
- En la demo, un dictado por voz se convierte en labor confirmada en el cuaderno con los campos clave correctos (fecha, parcela, categoría, producto/cantidad).
- El simulador produce el aceite estimado correcto para un caso con valores conocidos.
- El flujo completo se usa en un móvil con las interacciones grandes (micrófono) sin ayuda técnica.

## Notes
- Pipeline de IA: audio del usuario → transcripción → modelo de texto con salida JSON, vía gateway compatible con OpenAI (`inference.devexpert.io`). La clave API vive solo en el backend (detalles en `docs/technical-discovery.md`).
- La ausencia de cobertura es un caso de primera clase: cola offline simple con estados visibles (detalles en `docs/domain-model.md`).
