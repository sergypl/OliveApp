---
name: OlivApp — dirección visual MVP (rediseño Stitch)
description: App Android nativa (Material 3) móvil-first para socios de cooperativa sénior; sistema visual "Agro-Modern Olive Systems" (Stitch): verde oliva profundo, naranja de cosecha reservado al micrófono, superficies claras, texto grande y alto contraste.
designAssets:
  sourceOfTruth:
    - docs/design/stitch/agro_modern_olive_systems/DESIGN.md
    - docs/design/stitch/olivapp_socio_inicio_y_patio_de_tolvas/
    - docs/design/stitch/olivapp_socio_simulador_de_cosecha_m_vil/
    - docs/design/stitch/simulador_de_cosecha_y_portal_de_cooperativas/
    - docs/design/stitch/olivapp_logo/
  generatedConcepts: []
colors:
  primary: "#2D5A27"
  primary-pressed: "#154212"
  on-primary: "#FFFFFF"
  accent: "#FE932C"
  on-accent: "#663500"
  background: "#F4F8F0"
  surface: "#FDFFFC"
  surface-container-lowest: "#FDFFFA"
  surface-container-low: "#EEF2E9"
  surface-container: "#E7EBE2"
  surface-container-high: "#E0E5DA"
  surface-container-highest: "#D9DFD3"
  text: "#18200E"
  text-secondary: "#46503A"
  error: "#BA1A1A"
  error-soft-background: "#FFDAD6"
  error-soft-text: "#93000A"
  border: "#C1C6BA"
typography:
  h1:
    fontFamily: Montserrat
    fontSize: "28sp"
    fontWeight: 600
  h2:
    fontFamily: Montserrat
    fontSize: "22sp"
    fontWeight: 600
  metric:
    fontFamily: Montserrat
    fontSize: "40sp"
    fontWeight: 700
  body:
    fontFamily: Inter
    fontSize: "18sp"
    fontWeight: 400
  label:
    fontFamily: Inter
    fontSize: "16sp"
    fontWeight: 600
rounded:
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  full: "pill"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    minHeight: "64dp"
  button-secondary:
    backgroundColor: "transparent"
    borderColor: "{colors.primary}"
    textColor: "{colors.primary}"
    rounded: "{rounded.md}"
    minHeight: "64dp"
  button-danger:
    backgroundColor: "transparent"
    borderColor: "{colors.error}"
    textColor: "{colors.error}"
    rounded: "{rounded.md}"
    minHeight: "64dp"
  button-microphone:
    backgroundColor: "{colors.accent}"
    iconColor: "{colors.on-accent}"
    shape: circular
    size: "96dp"
  card:
    backgroundColor: "{colors.surface}"
    borderColor: "{colors.border}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  section-header:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.lg}"
  field:
    rounded: "{rounded.md}"
    minHeight: "64dp"
    labelSize: "18sp"
---

# Design Direction

## Overview
Dirección visual del MVP de OlivApp tras el rediseño `rediseno-visual-android`: una **app Android nativa (Jetpack Compose, Material 3)** con el sistema visual **"Agro-Modern Olive Systems"** de Stitch. Los mockups de `docs/design/stitch/` son la **fuente de verdad visual**, pero son **orientativos**: donde el mockup muestre contenido que la app no tiene (o falte el que sí tiene), manda la funcionalidad existente. No se añaden funciones, campos ni pantallas para igualar un mockup.

## Existing Design Assets
- `docs/design/stitch/agro_modern_olive_systems/DESIGN.md` — tokens del sistema (colores, tipografía, formas, espaciado, componentes).
- `docs/design/stitch/olivapp_socio_inicio_y_patio_de_tolvas/` — mockup del inicio/patio de tolvas (referencia de estilo para el Panel).
- `docs/design/stitch/olivapp_socio_simulador_de_cosecha_m_vil/` — mockup del simulador móvil (referencia de estilo para el Simulador).
- `docs/design/stitch/simulador_de_cosecha_y_portal_de_cooperativas/` — portal de cooperativas en escritorio: **solo pistas de estilo** (es otra superficie de producto).
- `docs/design/stitch/olivapp_logo/` — logotipo (SVG): icono de la app (adaptive icon) y splash.

## Product Feel
Calmado, funcional y "de campo", con autoridad agrícola: tarjetas estructuradas con borde fino, cabeceras de sección en verde oliva profundo, cifras destacadas y un naranja de cosecha reservado a la voz. Todo operable con una mano, a la luz del sol y sin gafas de leer; ligero y rápido en móviles modestos.

## Colors
Paleta M3 derivada del verde oliva del logo (tono 128° en OKLCH) con superficies en neutros cálidos; se retiró el tinte lavanda de los tokens Stitch (`#FAF8FF`, `#EAEDFF`, `#E2E7FF`) porque acentuaba el azul frente al oliva. Los pares de contraste están calculados con la fórmula WCAG (sRGB).

- **Primario `#2D5A27`** (verde oliva del logo; `#154212` pulsado; contenido blanco): botones principales, cabeceras de sección, acciones destacadas. Blanco sobre primario **8,1:1** ✓ (11,5:1 pulsado).
- **Acento `#FE932C` con contenido `#663500`** (naranja de cosecha): **reservado al micrófono y a las acciones de voz**; debe verse y encontrarse a la primera. El blanco sobre `#FE932C` da 2,2:1 (no pasa AA ni para iconos); `#663500` da **4,5:1** ✓.
- **Superficies** (escala tonal M3 del oliva): fondo `#F4F8F0`, superficie `#FDFFFC`, contenedores `#FDFFFA` / `#EEF2E9` / `#E7EBE2` / `#E0E5DA` / `#D9DFD3` (de `surfaceContainerLowest` a `surfaceContainerHighest`). Se sustituyen los antiguos `PanelDestacado`/`PanelDestacadoFuerte` por `Contenedor`/`ContenedorAlto`.
- **Texto** principal `#18200E`: **15,6:1** sobre fondo, **16,7:1** sobre superficie y entre **12,3:1 y 14,8:1** sobre los contenedores ✓✓. Texto secundario `#46503A`: **7,9:1** sobre fondo, **8,5:1** sobre superficie, **6,6:1** sobre el contenedor más alto ✓.
- **Error `#BA1A1A`** (6,0:1 sobre fondo) y su versión suave `#93000A` sobre `#FFDAD6` (7,2:1) para acciones destructivas y errores.
- **Chips de estado**: verde `#166534`/`#DCFCE7` (6,5:1), ámbar `#92400E`/`#FEF3C7` (6,4:1), rojo `#991B1B`/`#FEE2E2` (6,8:1). Siempre con etiqueta de texto, nunca solo color.
- **Borde `#C1C6BA`** (1,7:1 sobre superficie): es **decorativo** — nunca es la única señal de un control: los campos se identifican por su etiqueta de texto y los botones por su relleno o su contorno grueso de 2dp con el color primario.
- Regla dura: todo texto cumple contraste AA (≥ 4,5:1; ≥ 3:1 para texto grande o iconos) en **todas** las combinaciones anteriores, verificadas con fuente grande y en horizontal además de en vertical. Los datos oficiales de la cooperativa se distinguen de "Mi cuaderno" por superficie **y** etiqueta (no solo por color).

## Typography
- **Montserrat** (títulos y cifras) e **Inter** (cuerpo, etiquetas, botones), empaquetadas como recursos (ADR 0009) y podadas al subconjunto latino para aligerar el APK (ADR 0012), en `sp` para respetar el escalado del dispositivo.
- Jerarquía: h1 28sp Montserrat SemiBold; h2 22sp Montserrat SemiBold; cifra destacada 40sp Montserrat Bold (resultado del simulador); cuerpo 18sp Inter Regular; etiqueta 16sp Inter SemiBold.
- **Regla sénior**: cuerpo ≥ 18sp y nada por debajo de 16sp, aunque los mockups de Stitch usen 12-16px (se escalan por legibilidad, decisión aprobada en la Fase 0).

## Layout
- Una columna, móvil-first. Una pantalla = una tarea.
- Panel tras el login: campaña actual destacada arriba, micrófono circular (96dp) protagonista, y accesos grandes a las herramientas; debajo, campañas anteriores.
- Sin menús ocultos ni cajón de navegación para las funciones core: todo visible como botón grande. La barra de navegación inferior de los mockups no se adopta (cambiaría los flujos).
- Ritmo 4/8dp; padding de tarjeta 24dp; mínimo 16dp entre controles tocables.

## Shapes
- Escala `{8, 12, 16, 24, pill}`: botones y campos 12dp, tarjetas 16dp, diálogos 24dp, chips en píldora.
- Profundidad por capas tonales y borde fino de 1px (sin sombras complejas).

## Components
- `button-primary`: verde `#2D5A27`, blanco, altura mínima 64dp, texto grande, radio 12dp.
- `button-secondary`: contorno verde 1.5dp, texto verde, 64dp.
- `button-danger`: contorno rojo `#BA1A1A`, texto rojo, 64dp (acciones destructivas con confirmación).
- `button-microphone`: **naranja `#FE932C`, circular, 96dp, icono `#663500`**; estado de grabación con animación sencilla (pulsar/cambiar de color).
- `card`: superficie `#FDFFFC` con borde 1px `#C1C6BA`, radio 16dp, padding 24dp.
- `section-header`: banda verde `#2D5A27` con texto blanco (cabeceras tipo "Patio Abierto" de los mockups).
- Campos de formulario: altura mínima 64dp, etiqueta encima en 18sp, sin placeholders como única etiqueta; foco con anillo de 2dp en primario.
- Chips/estados de la nota de voz (pendiente / transcribiendo / transcrita / error): píldoras con etiqueta de texto, nunca solo color.
- Tarjeta de campaña: título de campaña, datos oficiales (kilos entregados, rendimiento, aceite) con etiqueta de "Cooperativa" y, aparte, las labores de "Mi cuaderno".
- Estados de la nota de voz en frases de lenguaje de campo ("Grabada sin conexión. Se enviará al llegar a cobertura.").

## Core Screens
1. **Login / registro:** email + contraseña, dos botones grandes, mensajes de error en frases claras.
2. **Panel:** campaña actual destacada, micrófono + accesos a las herramientas, y acceso a campañas anteriores.
3. **Campaña:** datos oficiales de la cooperativa y, separadas, las labores de esa campaña de "Mi cuaderno".
4. **Mi cuaderno:** lista cronológica de labores (fecha + parcela + categoría + detalle), con sus acciones.
5. **Borrador IA:** ficha legible con los campos extraídos, campos vacíos resaltados, y Confirmar / Descartar.
6. **Formulario manual:** crear o corregir una labor; máximo ~5 campos visibles.
7. **Calculadora (Simulador de liquidación en limpio):** entradas grandes (kilos, % rendimiento, % ajuste industrial) + resultado del aceite neto en número grande, claramente etiquetado como estimación orientativa.

## Responsive Baseline
- Diseño para móvil Android (≥ 360dp de ancho), orientación vertical. En tablet/plegable, centra la columna con máximo ~640dp y el mismo contenido; no hay layout distinto.
- La interfaz respeta el escalado de fuente y de tamaño del sistema (revisión con fuente grande del sistema como parte de la verificación del rediseño).

## Accessibility Baseline
- Contraste AA en todo texto y ≥ 3:1 para iconos; objetivos táctiles ≥ 48dp (botones a 64dp, micrófono 96dp).
- Etiquetas de accesibilidad (`contentDescription`) en iconos y en el botón de micrófono; compatible con TalkBack.
- Sin tecnicismos: "cuaderno", "labor", "parcela", "campaña", "aceite"; nunca "sincronizar", "payload", "entidad".
- Feedback siempre en frases completas y claras; nunca solo color para comunicar estado.
- Texto ≥ 18sp (nada < 16sp); sin animaciones esenciales para entender el estado.

## Do's and Don'ts
- **Do:** micrófono siempre accesible en el panel; separar claramente "Mi cuaderno" de los datos de la cooperativa; confirmar acciones destructivas en lenguaje claro; números grandes en la calculadora; una tarea por pantalla; respetar el vocabulario de `CONTEXT.md` en cada texto.
- **Don't:** menús ocultos para funciones core; formularios con más de ~5 campos visibles; texto < 16sp; tecnicismos; depender solo del color para el estado; mezclar en una misma lista las labores del socio y los datos oficiales; adoptar textos de los mockups que no pasen por `CONTEXT.md` (p. ej. "Dictar Nota SIEX"); añadir campos o pantallas para igualar un mockup (p. ej. precios de mercado, clima, entregas).

## Open Design Questions
- ¿Modo oscuro? Fuera del MVP; las condiciones de uso son a plena luz del día.
- Iconografía: Material Symbols (los actuales), siempre acompañada de etiqueta de texto; sin iconos propios extra por ahora.
- Uso del wordmark "OlivApp" dentro de la app (cabecera de acceso): solo si se solicita; el logo ya vive en el icono de lanzamiento y el splash.
- Adaptación a tablets/plegables: se mantiene la columna centrada; sin diseño específico por ahora.
