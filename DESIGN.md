---
name: OlivApp — dirección visual MVP
description: App Android nativa (Material 3) móvil-first para socios de cooperativa sénior; verde oliva y crema, texto grande, alto contraste, micrófono protagonista.
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
    fontFamily: system default
    fontSize: "28sp"
    fontWeight: 700
  body:
    fontFamily: system default
    fontSize: "18sp"
rounded:
  sm: "8dp"
  md: "12dp"
spacing:
  sm: "8dp"
  md: "16dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    minHeight: "64dp"
  button-microphone:
    backgroundColor: "{colors.accent}"
    textColor: "#FFFFFF"
    shape: circular
    size: "96dp"
---

# Design Direction

## Overview
Dirección visual escrita para el MVP de OlivApp, una **app Android nativa (Jetpack Compose, Material 3)**. No hay logo ni marca todavía; esta dirección es la referencia para cualquier agente que implemente UI hasta que existan assets propios. No hay conceptos visuales generados: la generación de imágenes no estuvo disponible en la sesión de discovery; si se generan más adelante, se guardarán en `docs/design/concepts/` y se referenciarán aquí.

## Existing Design Assets
Ninguno. Esta dirección escrita es la única fuente de dirección visual.

## Generated Concept Images
Ninguno por ahora (ver Overview).

## Product Feel
Calmado, funcional y "de campo": como una libreta grande y bien ordenada, no como un panel de control. Todo debe ser operable con una mano, a la luz del sol y sin gafas de leer. La app nativa debe sentirse ligera y rápida en móviles modestos.

## Colors
- Verde oliva `#5A6E3A` como color primario (superficies y botones grandes); verde oscuro `#3F4A2A` para estados activos.
- Naranja cálido `#C25E00` reservado al botón de micrófono: debe verse y encontrarse a la primera.
- Fondo crema `#FAF7F0` y superficies blancas; texto marrón oscuro `#2E2A20`.
- Regla dura: todo texto debe cumplir contraste AA (≥ 4,5:1). Texto principal siempre en `#2E2A20` sobre fondo claro; el verde oliva se usa en superficies grandes, no como color de texto pequeño.
- Los datos oficiales de la cooperativa deben distinguirse visualmente de "Mi cuaderno" por superficie y etiqueta (no solo por color).

## Typography
- Tipografía del sistema Android (Roboto por defecto), en `sp` para respetar el escalado de fuente del dispositivo.
- Cuerpo 18sp mínimo; títulos 28sp; números de resultados (simulador) extra grandes.
- Nada de texto por debajo de 16sp en el MVP.

## Layout
- Una columna, móvil-first. Una pantalla = una tarea.
- Panel tras el login: campaña actual destacada arriba, botón de micrófono circular (96dp) protagonista, y dos accesos grandes: Mi cuaderno y Calculadora; debajo, acceso a campañas anteriores.
- Sin menús ocultos ni cajón de navegación para las funciones core: todo visible como botón grande.
- Espaciado generoso (mínimo 16dp entre controles tocables).

## Shapes
- Bordes redondeados suaves (8-12dp); botón de micrófono circular (Material `FloatingActionButton` grande o equivalente).
- Sin sombras complejas: separación por color de superficie.

## Components
- `button-primary`: verde oliva, blanco, altura mínima 64dp, texto grande.
- `button-microphone`: naranja, circular, 96dp, icono de micrófono grande; estado de grabación con animación sencilla (pulsar/cambiar de color).
- Campos de formulario: altura mínima 64dp, etiqueta encima en 18sp, sin placeholders como única etiqueta.
- Tarjeta de campaña: título de campaña, datos oficiales (kilos entregados, rendimiento, aceite) con etiqueta de "Cooperativa" y, aparte, el acceso a las labores de esa campaña.
- Estados de la nota de voz (pendiente / transcribiendo / a confirmar): tarjetas con una frase en lenguaje de campo ("Grabada sin conexión. Se enviará al llegar a cobertura.").

## Core Screens
1. **Login / registro:** email + contraseña, dos botones grandes, mensajes de error en frases claras.
2. **Panel:** campaña actual destacada, botón de micrófono + accesos a Mi cuaderno y Calculadora, y acceso a campañas anteriores.
3. **Campaña:** datos oficiales de la cooperativa (kilos entregados, rendimiento, aceite) y, separadas, las labores de esa campaña de "Mi cuaderno".
4. **Mi cuaderno:** lista cronológica de labores (fecha + parcela + categoría + detalle), con filtro simple por parcela.
5. **Borrador IA:** ficha legible con los campos extraídos (fecha, parcela, categoría, producto/cantidad o vuelo/suelo+kilos), campos vacíos resaltados para elegir de lista, y dos acciones: Confirmar / Corregir (y Descartar).
6. **Formulario manual:** para crear o corregir una labor; máximo ~5 campos visibles.
7. **Calculadora:** dos entradas grandes (kilos, % rendimiento) + % ajuste industrial editable + resultado del aceite neto en número grande.

## Responsive Baseline
- Diseño para móvil Android (≥ 360dp de ancho), orientación vertical. En tablet/plegable, centra la columna con máximo ~640dp y el mismo contenido; no hay layout distinto.
- La interfaz debe respetar el escalado de fuente y de tamaño del sistema.

## Accessibility Baseline
- Contraste AA en todo texto; objetivos táctiles ≥ 48dp (botones a 64dp, micrófono 96dp).
- Etiquetas de accesibilidad (`contentDescription`) en iconos y en el botón de micrófono; compatible con TalkBack.
- Sin tecnicismos: "cuaderno", "labor", "parcela", "campaña", "aceite"; nunca "sincronizar", "payload", "entidad".
- Feedback siempre en frases completas y claras; nunca solo color para comunicar estado.
- Texto ≥ 18sp; sin animaciones esenciales para entender el estado.

## Do's and Don'ts
- **Do:** micrófono siempre accesible en el panel; separar claramente "Mi cuaderno" de los datos de la cooperativa; confirmar acciones destructivas en lenguaje claro; números grandes en la calculadora; una tarea por pantalla.
- **Don't:** menús ocultos para funciones core; formularios con más de ~5 campos visibles; texto < 16sp; tecnicismos; depender solo del color para el estado; mezclar en una misma lista las labores del socio y los datos oficiales; inventar logo o marca (no existe).

## Open Design Questions
- Logo y paleta definitiva (cuando exista marca, será source of truth sobre estos tokens).
- ¿Modo oscuro? Fuera del MVP; las condiciones de uso son a plena luz del día.
- Iconografía (Material Symbols vs. propia): sin decisión aún; siempre acompañada de etiqueta de texto.
- Adaptación a tablets/plegables: se mantiene la columna centrada; sin diseño específico por ahora.
