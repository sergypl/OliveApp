---
name: OlivApp — dirección visual MVP
description: Web responsive móvil-first para olivareros sénior; verde oliva y crema, texto grande, alto contraste, micrófono protagonista.
designAssets:
  sourceOfTruth: []
  generatedConcepts: []
colors:
  primary: "#5A6E3A"
  secondary: "#3F4A2A"
  accent: "#C25E00"
  background: "#FAF7F0"
  surface: "#FFFFFF"
  text: "#2E2A20"
typography:
  h1:
    fontFamily: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif
    fontSize: "28px"
    fontWeight: 700
  body:
    fontFamily: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif
    fontSize: "18px"
rounded:
  sm: "8px"
  md: "12px"
spacing:
  sm: "8px"
  md: "16px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    minHeight: "64px"
  button-microphone:
    backgroundColor: "{colors.accent}"
    textColor: "#FFFFFF"
    shape: circular
    size: "96px"
---

# Design Direction

## Overview
Dirección visual escrita para el MVP de OlivApp. No hay logo ni marca todavía; esta dirección es la referencia para cualquier agente que implemente UI hasta que existan assets propios. No hay conceptos visuales generados: la generación de imágenes no estuvo disponible en la sesión de discovery; si se generan más adelante, se guardarán en `docs/design/concepts/` y se referenciarán aquí.

## Existing Design Assets
Ninguno. Esta dirección escrita es la única fuente de dirección visual.

## Generated Concept Images
Ninguno por ahora (ver Overview).

## Product Feel
Calmado, funcional y "de campo": como una libreta grande y bien ordenada, no como un panel de control. Todo debe ser operable con una mano, a la luz del sol y sin gafas de leer.

## Colors
- Verde oliva `#5A6E3A` como color primario (superficies y botones grandes); verde oscuro `#3F4A2A` para estados activos.
- Naranja cálido `#C25E00` reservado al botón de micrófono: debe verse y encontrarse a la primera.
- Fondo crema `#FAF7F0` y superficies blancas; texto marrón oscuro `#2E2A20`.
- Regla dura: todo texto debe cumplir contraste AA (≥ 4,5:1). Texto principal siempre en `#2E2A20` sobre fondo claro; el verde oliva se usa en superficies grandes, no como color de texto pequeño.

## Typography
- Fuente del sistema (pila `system-ui`): carga rápida en móviles modestos y aspecto familiar.
- Cuerpo 18px mínimo; títulos 28px; números de resultados (simulador) extra grandes.
- Nada de texto por debajo de 16px en el MVP.

## Layout
- Una columna, móvil-first. Una pantalla = una tarea.
- Pantalla de inicio con el micrófono protagonista (botón circular de 96px, centrado, arriba del todo) y dos accesos grandes debajo: Cuaderno y Simulador.
- Sin menús ocultos ni hamburguesa para las funciones core: todo visible como botón grande.
- Espaciado generoso (mínimo 16px entre controles tocables).

## Shapes
- Bordes redondeados suaves (8-12px); botón de micrófono circular.
- Sin sombras complejas: separación por color de superficie.

## Components
- `button-primary`: verde oliva, blanco, altura mínima 64px, texto grande.
- `button-microphone`: naranja, circular, 96px, icono de micrófono grande; estado de grabación con animación sencilla (pulsar/cambiar de color).
- Campos de formulario: altura mínima 64px, etiqueta encima en 18px, sin placeholders como única etiqueta.
- Estados de la nota de voz (pendiente / transcribiendo / a confirmar): tarjetas con una frase en lenguaje de campo ("Grabada sin conexión. Se enviará al llegar a cobertura.").

## Core Screens
1. **Inicio:** micrófono gigante + accesos a Cuaderno y Simulador.
2. **Borrador IA:** ficha legible con los campos extraídos (fecha, parcela, categoría, producto/cantidad o vuelo/suelo+kilos), campos vacíos resaltados para elegir de lista, y dos acciones: Confirmar / Corregir (y Descartar).
3. **Cuaderno:** lista cronológica de labores (fecha + parcela + categoría + detalle), con opción de filtro simple por parcela.
4. **Formulario manual:** para crear o corregir una labor; máximo ~5 campos visibles.
5. **Simulador:** dos entradas grandes (kilos, % rendimiento) + % ajuste industrial editable + resultado del aceite neto en número grande.
6. **Login / registro:** email + contraseña, dos botones grandes, mensajes de error en frases claras.

## Responsive Baseline
- Diseño para móvil (≥ 360px de ancho); en tablet/desktop centra la columna con máximo ~640px y el mismo contenido. No hay layout de escritorio distinto.

## Accessibility Baseline
- Contraste AA en todo texto; objetivos táctiles ≥ 64px (micrófono 96px).
- Sin tecnicismos: "cuaderno", "labor", "parcela", "aceite"; nunca "sincronizar", "payload", "entidad".
- Feedback siempre en frases completas y claras; nunca solo color para comunicar estado.
- Texto ≥ 18px; sin animaciones esenciales para entender el estado.

## Do's and Don'ts
- **Do:** micrófono siempre accesible en el inicio; confirmar acciones destructivas en lenguaje claro; números grandes en el simulador; una tarea por pantalla.
- **Don't:** menús ocultos para funciones core; formularios con más de ~5 campos visibles; texto < 16px; tecnicismos; depender solo del color para el estado; inventar logo o marca (no existe).

## Open Design Questions
- Logo y paleta definitiva (cuando exista marca, será source of truth sobre estos tokens).
- ¿Modo oscuro? Fuera del MVP; las condiciones de uso son a plena luz del día.
- Iconografía: sin decisión aún; siempre acompañada de etiqueta de texto.
