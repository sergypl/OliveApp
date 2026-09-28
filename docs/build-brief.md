# Build Brief — OlivApp

Proyecto práctico para un curso de Inteligencia Artificial. Discovery en español.

## Problem
El socio de una cooperativa oleícola necesita, por un lado, saber qué ha pasado con su cosecha: cuántos kilos entregó a la cooperativa, con qué rendimiento graso y cuánto aceite obtuvo en cada campaña. Hoy esa información está en la cooperativa, en papel o en una web pensada para gestores, y el socio no la tiene a mano en el móvil. Por otro lado, debe mantener su cuaderno de campo (tratamientos fitosanitarios, riegos, cosecha), que hoy hace a mano: es lento, se olvida y se pospone. Además, antes de llevar la aceituna a la almazara no tiene una forma rápida de estimar cuánto aceite puede esperar y qué deducción industrial le aplicarán.

La tesis del proyecto: la IA (transcripción por voz + extracción estructurada) reduce la fricción del registro en el campo, y un panel de campañas con los datos fieles de la cooperativa más un simulador simple dan visibilidad inmediata al socio.

## Current Workaround / Existing System
- Datos de campaña consultados en la cooperativa (papel, web de gestor o preguntando en oficina).
- Cuaderno de campo en papel en el campo; transcripción manual posterior a SIEX desde un ordenador.
- Cálculos mentales u hojas de cálculo sueltas para estimar el rendimiento graso.

## Target Users
- Socio de cooperativa oleícola (España, mayoritariamente Andalucía), perfil sénior: texto grande, alto contraste, pocos pasos, sin tecnicismos.
- Un único rol en el MVP: el socio. Cada socio ve y edita solo sus propios datos; nunca los de otro. Acceso: registro y login con email + contraseña.
- La cooperativa no es usuaria de la app: sus datos de campaña entran precargados y son de solo lectura.
- El tribunal del curso es evaluador, no usuario de la aplicación.

## Goals
1. Dar al socio un panel donde consulta la campaña actual y las anteriores con los datos fieles de su cooperativa (kilos, rendimiento, aceite).
2. Validar el uso de IA para capturar labores por voz en el campo: dictar → ficha estructurada (estructura SIEX) → confirmación con un toque.
3. Ofrecer un cuaderno de campo simple, cronológico y legible por campaña, con formulario manual auxiliar para correcciones.
4. Estimar la liquidación en limpio: kilos × rendimiento graso − ajuste industrial configurable.
5. Cumplir la usabilidad sénior en Android: micrófono protagonista, alto contraste, texto grande, sin menús complejos.
6. Mantener un alcance de desarrollo acotado a un entorno académico.

## Non-Goals
- Administración por parte de la cooperativa: en el MVP no hay rol ni pantallas de cooperativa; sus datos se siembran/cargan y son de solo lectura.
- Integración o exportación real con SIEX (solo se imita su estructura de campos).
- Comparativas avanzadas entre lo registrado por el socio y el dato oficial (más allá de mostrarlos por separado).
- Sensores IoT en tiempo real (almazara o maquinaria) ni mapas satelitales / teledetección.
- Módulos B2B de gestión de bodega o básculas de cooperativa.
- Multi-cooperativa: el MVP asume una única cooperativa sembrada.
- Roles técnicos, explotaciones compartidas, pagos o suscripciones.
- Categorías de labor adicionales (poda, abonado, siega...) y liquidación real desglosada (humedad, impurezas, costes de molturación).

## MVP Slice
Aplicación Android nativa (ADR 0002) con backend ligero para proteger la clave de la API de IA y exponer la API de datos.

1. **Acceso y panel.** Registro/login con email + contraseña. Tras entrar, el panel muestra la campaña actual y da acceso a las campañas anteriores, al cuaderno y al simulador. El socio pertenece a una cooperativa sembrada, cuyos datos de campaña ve de solo lectura.
2. **Campañas.** Vista de campaña con los datos oficiales de la cooperativa (kilos entregados, rendimiento graso, aceite obtenido) y, separadas, las labores de esa campaña de "Mi cuaderno". Las campañas anteriores se consultan desde el panel.
3. **Registro por voz (feature estrella).** Botón de micrófono grande en el panel → grabación corta → si no hay cobertura queda como nota pendiente → transcripción vía gateway de inferencia → el LLM extrae un JSON {fecha, parcela, categoría, campos de la categoría} → borrador IA → confirmar/corregir con un toque → labor en el cuaderno. Categorías: tratamiento fitosanitario, riego, cosecha (a vuelo / a suelo).
4. **Cuaderno de campo ("Mi cuaderno").** Historial cronológico de labores por campaña con campos tipo SIEX; formulario manual auxiliar para crear o corregir registros.
5. **Simulador de liquidación en limpio.** Kilos de aceituna × % rendimiento graso (entrada del socio) − % ajuste industrial configurable → aceite neto estimado.

## Validation Plan
Demo end-to-end ante el tribunal en un móvil Android: abrir el panel, consultar una campaña (dato oficial de cooperativa) y una campaña anterior; dictar una labor real por voz, confirmar el borrador IA y ver la labor en el cuaderno; mostrar el resultado del simulador en al menos un caso con valores verificados a mano. (Métricas de precisión y prueba con socio real quedan como extensión voluntaria; ver riesgos.)

## Success Criteria
- En la demo, el socio ve los datos oficiales de su cooperativa para la campaña actual y al menos una anterior.
- Un dictado por voz se convierte en labor confirmada en el cuaderno con los campos clave correctos (fecha, parcela, categoría, producto/cantidad).
- El simulador produce el aceite estimado correcto para un caso con valores conocidos.
- El flujo completo se usa en un móvil Android con las interacciones grandes (micrófono) sin ayuda técnica.

## Notes
- Pipeline de IA: audio del socio → transcripción → modelo de texto con salida JSON, vía gateway compatible con OpenAI (`inference.devexpert.io`). La clave API vive solo en el backend (detalles en `docs/technical-discovery.md`).
- Los datos de campaña de la cooperativa se siembran para la demo (no hay administración de cooperativa en el MVP).
- La ausencia de cobertura es un caso de primera clase: cola offline local con estados visibles (detalles en `docs/domain-model.md`).
