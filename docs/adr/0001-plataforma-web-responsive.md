# ADR 0001: OlivApp MVP como web responsive en lugar de app móvil nativa

## Status
Accepted (2026-09-21)

## Context
El briefing describía OlivApp como "aplicación móvil", pero el proyecto es un MVP académico con plazo y alcance acotados. La feature estrella (captura de labores por voz con IA) es viable en un navegador moderno (grabación con MediaRecorder, cola offline simple sin APIs nativas). El usuario tomó la decisión durante la discovery: web responsive accesible desde el navegador.

## Decision
Construir el MVP como aplicación web responsive (móvil-first) accesible desde el navegador, con un backend ligero que sirva la web, proteja la clave de la API de IA y exponga la API de datos.

## Alternatives Considered
- **Expo / React Native:** app real iOS/Android; más fricción de desarrollo, dispositivos y demo para el alcance del curso. Descartada.
- **Nativa (Kotlin/Swift):** máximo acceso al SO; coste inviable para el MVP académico. Descartada.
- **PWA instalable:** variante posterior de la misma web; no se exige instalación en el MVP.

## Consequences
- Demo inmediata en el navegador de cualquier móvil y despliegue simple.
- Micrófono vía APIs del navegador: exige HTTPS y hay diferencias iOS/Android que probar pronto.
- Se pierde integración profunda con el SO (notificaciones, widgets): fuera del alcance del MVP.
- Si el producto creciera hacia una app de tienda, la plataforma habría de replantearse: por eso la cola offline y el backend se diseñan sin asumir el navegador para siempre.
