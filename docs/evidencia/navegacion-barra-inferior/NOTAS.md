# navegacion-barra-inferior — NOTAS

## Estado
Implementada y verificada con tests unitarios, `assembleDebug` y `lint` (sin dispositivo: las pistas
no usan adb). **Pendiente de la sesión de integración**: comprobación visual en el Xiaomi (barra con
fuente grande 1,3× y horizontal, rotación en cada pestaña, botón atrás) y paso a `passing`.

## Qué se hizo
- `core/navegacion/Pestanas.kt` (lógica pura, sin Compose): enum `Pestana` (Inicio · Cuaderno ·
  Simulador · Parcelas · Perfil), `barraInferiorVisible`, `pestanaDe`, `pantallaAlElegir` y
  `accionDeAtras` (`AccionAtras.Salir` / `IrA`).
- `core/navegacion/BarraInferior.kt`: `NavigationBar` de M3 con etiqueta siempre visible, indicador
  de pestaña activa (`ContenedorMasAlto`), icono en verde oliva y etiqueta en negrita (no depende
  solo del color). Etiquetas a 14sp con la escala de letra de la barra acotada a 1,2× para que las
  cinco quepan en una línea.
- `Navegacion.kt`: se mantiene el router propio (`irA("...")`) y la tienda de ViewModels por visita.
  La barra se dibuja fija sobre el contenido (fuera del `AnimatedContent`, no se anima ni se esconde
  con el scroll) solo en las pantallas de primer nivel, y se retira con el teclado abierto.
  `BackHandler` con la lógica pura: pestaña distinta de Inicio → Inicio; Inicio y pantallas de acceso
  → salir (el sistema cierra la app); segundo nivel → lo mismo que su botón "Volver".
- `core/ui/PantallaCompleta.kt`: nuevo `LocalBarraInferiorVisible`; `pantallaCompleta()` y
  `PantallaConBarraSuperiorGrande` solo reservan el hueco de 80dp cuando la pantalla lleva barra
  (antes lo reservaban siempre).
- Panel: quitados los botones "Tus parcelas", "Cuaderno de campo" y "Simulador de liquidación en
  limpio" (y sus callbacks). Se mantiene el micrófono y "Cerrar sesión" (pasa al Perfil en
  `perfil-del-socio`).
- Cuaderno, Simulador y Parcelas: quitado su botón "Volver al panel" (y el parámetro `onVolver`) porque
  ahora son pestañas. Segundo nivel (campaña, nueva labor, notas de voz) y acceso conservan "Volver".
- Recursos: `ic_inicio.xml` e `ic_perfil.xml` (Material Symbols Rounded, rejilla de 960 como el resto).
- Pestaña Perfil: `perfil/presentation/PantallaPerfil.kt` provisional (solo el título); lo sustituye
  `perfil-del-socio`.
- Trabajo parcial heredado (`Pestanas.kt`, iconos): revisado, correcto y reutilizado; la carpeta
  `.tmp-medida/` es basura de medición y no se ha incluido en ningún commit.

## Verificación ejecutada
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint 2>&1 | tail -40` → BUILD SUCCESSFUL.
- `PestanasTest`: 12 tests, 0 fallos (orden de pestañas, visibilidad de la barra, pestaña activa,
  pulsar pestaña activa, atrás desde pestaña/Inicio/acceso/segundo nivel, sin historial).
  Total de la suite: 139 tests, 0 fallos.

## Tests cambiados y por qué
Ninguno: no se ha tocado ninguna aserción existente (ningún test cubría la navegación por botones
del panel).

## Decisiones tomadas sin consultar
- El botón atrás en pantallas de segundo nivel hace lo mismo que su "Volver" (antes no había
  `BackHandler` y el sistema cerraba la app). El enunciado solo fijaba el comportamiento en pestañas;
  esto evita salir de la app por accidente desde una pantalla secundaria.
- En las pantallas de acceso el atrás sigue saliendo de la app (comportamiento previo).
- La barra se oculta con el teclado abierto (el formulario gana espacio; coherente con el hueco que
  ya se retiraba).
- Escala de letra de la barra acotada a 1,2× y etiquetas de 14sp (por debajo de los 16sp de
  `TextoEtiqueta`): con cinco pestañas en ~360dp no caben más grandes sin cortar "Simulador".
- "Cerrar sesión" se queda en el panel hasta `perfil-del-socio` para no dejar la app sin forma de
  cerrar sesión entre ambas features.

## Pendiente / riesgos
- Revisar en dispositivo con fuente 1,3× y horizontal que las cinco etiquetas no se cortan y que el
  hueco de 80dp coincide con la barra + inset de gestos.
- La pista `panel` convierte el micrófono en botón flotante: deberá colocarlo por encima de la barra
  (hueco `AlturaBarraInferior`).

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- CONTEXT.md, término **Barra de navegación**: "Barra inferior fija con cinco pestañas (Inicio,
  Cuaderno, Simulador, Parcelas, Perfil), etiqueta siempre visible, visible solo en las pantallas de
  primer nivel tras iniciar sesión. Las pantallas de segundo nivel (detalle de campaña, nueva labor,
  notas de voz) la ocultan y conservan «Volver»."
- feature_list.json `navegacion-barra-inferior`: tras la comprobación en dispositivo, `passing` con
  evidencia "PestanasTest 12/12, suite 139/139, assembleDebug y lint en verde; <capturas en Xiaomi>".
