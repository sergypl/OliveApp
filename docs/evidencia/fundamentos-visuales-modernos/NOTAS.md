# fundamentos-visuales-modernos — NOTAS

## Estado
`passing` — implementada y verificada con dispositivo (Xiaomi M2101K6G, 1080x2400, 440dpi).

## Qué se hizo
- **Pantalla completa de borde a borde**: `Modifier.pantallaCompleta()` en `core/ui/PantallaCompleta.kt`
  (`enableEdgeToEdge()` en `MainActivity`), con fondo a sangre, iconos oscuros en la barra de estado y
  márgenes de `safeDrawing` en todas las pantallas. Se reserva `AlturaBarraInferior = 80.dp` para la
  futura barra de navegación inferior; el hueco se retira con el teclado abierto
  (`WindowInsets.isImeVisible`).
- **Paleta M3 derivada del oliva** (`core/ui/Colores.kt`, `Tema.kt`): se retiró el tinte lavanda de los
  tokens Stitch (`#FAF8FF`, `#EAEDFF`, `#E2E7FF`) y se generó una escala tonal cálida (hue 128° OKLCH
  a partir de `#2D5A27`): `Fondo #F4F8F0`, `Superficie #FDFFFC`, `ContenedorMasBajo #FDFFFA`,
  `ContenedorBajo #EEF2E9`, `Contenedor #E7EBE2`, `ContenedorAlto #E0E5DA`, `ContenedorMasAlto #D9DFD3`,
  `Borde #C1C6BA`, `Texto #18200E`, `TextoSecundario #46503A`. El naranja queda reservado al micrófono.
  Se sustituyen `PanelDestacado`→`Contenedor` y `PanelDestacadoFuerte`→`ContenedorAlto` y se eliminan
  los alias legados (`FondoCrema`, `TextoMarron`, `VerdeOliva`; `TextoError`→`Error`). Contrastes AA
  calculados y documentados en `DESIGN.md` (sección Colors y frontmatter `colors`).
- **Iconos Material Symbols Rounded** como vectores propios en `res/drawable/ic_*.xml` (sin dependencia
  `material-icons-extended`), siempre **junto a texto**. Aplicados a botones (`icono` en
  `BotonPrincipal/Secundario/Peligro/Accion`), filas de datos del panel y la campaña (`FilaDato`),
  cabeceras de sección (`CabeceraSeccion`), categorías del cuaderno (`OpcionCategoria` e icono de la
  categoría en `FilaLabor`).
- **Componentes nuevos en `core/ui`** (en español como el resto): `BarraSuperior.kt`
  (LargeTopAppBar que se encoge al hacer scroll, con botón de volver), `Carga.kt` (esqueleto de carga
  con brillo animado), `EstadoVacio.kt` (hoja de olivo vectorial, texto y acción),
  `Avisos.kt` (`HostDeAvisos` + `mostrarConDeshacer`), `TarjetaDato.kt` (`TarjetaDatoVariacion` con
  variación `+X %`/`−X %`, icono además de color), `Refrescar.kt` (`ContenedorRefrescar` sobre
  PullToRefreshBox) y `BotonMicrofonoFlotante` en `Botones.kt`. No todos se usan aún en pantallas:
  los aplicarán las features de la oleada 1.
- **Transiciones en el router propio** (`core/navegacion/Navegacion.kt`): `AnimatedContent` con
  fundido + deslizamiento vertical entre pantallas, conservando `PropietarioDeVisita` por visita
  (`remember(visitaActual)`) para no romper la tienda de ViewModels: la rotación conserva pantalla y
  lo tecleado (verificado en dispositivo).
- **Gesto atrás predictivo**: `android:enableOnBackInvokedCallback="true"` en `<application>` (no es
  un permiso).
- **Splash animado**: `androidx.core:core-splashscreen:1.0.1` (ADR 0010), `installSplashScreen()` en
  `MainActivity`, tema `Theme.SplashScreen` + `postSplashScreenTheme`, animación de 400 ms sin esperas
  artificiales.
- **Formato de variación**: `variacionPorcentual`/`textoVariacion` en `core/formatos/Formatos.kt` con
  tests en `core/formatos/VariacionTest.kt`.
- Auxiliar operativo `backend/iniciar-dev.sh`: arranca el backend de desarrollo con la configuración
  guardada en el fichero de entorno gitignored (nunca lo imprime), para no caducar las sesiones del
  móvil (nota operativa de PROGRESS.md).

## Verificación ejecutada
- `./init.sh` completo en verde (2026-10-15): backend 124/124 tests, healthcheck 200, datos sembrados
  OK, APK compilado, instalado y abierto en el Xiaomi M2101K6G.
- `cd android && ./gradlew testDebugUnitTest lint` → `BUILD SUCCESSFUL` (sin errores de tests ni de lint;
  los tests requieren el puerto 8000 libre, como documenta PROGRESS.md).
- Capturas en el móvil antes/después en `docs/evidencia/fundamentos-visuales-modernos/`:
  - `antes/` y `despues/`: panel, panel-herramientas, campana-detalle, parcelas, cuaderno-lista,
    cuaderno-ficha-labor, cuaderno-formulario-labor, simulador, voz-notas, voz-borrador-ia,
    acceso-bienvenida, acceso-inicio-sesion, acceso-registro (+ `despues-simulador-resultado.png`).
  - `antes-fuente-grande/` y `despues-fuente-grande/` (font_scale 1.3): panel, parcelas, cuaderno-lista,
    simulador, voz-borrador.
  - `antes-horizontal/` y `despues-horizontal/` (rotación forzada): panel, parcelas, simulador.
- Smoke test en dispositivo: recorrido completo (panel, campaña, parcelas, cuaderno, formulario y ficha
  de labor, simulador con cálculo 1000 kg · 20 % · 10 % → 180 kg ✓, notas de voz, cierre y nuevo inicio
  de sesión) con dos rotaciones a horizontal y vuelta; `logcat` sin `FATAL EXCEPTION`, sin errores de
  `AndroidRuntime` ni ANR de `com.olivapp.app`. La rotación conserva pantalla y datos tecleados.
- Contraste AA verificado con la fórmula WCAG sRGB y documentado en `DESIGN.md` (p. ej. texto principal
  15,6:1 sobre fondo y 13,9:1 sobre contenedor; texto secundario ≥ 6,6:1; blanco sobre primario 8,1:1;
  naranja con contenido `#663500` 4,5:1). Revisado además con fuente grande (1.3) y en horizontal.

## Tests cambiados y por qué
Ninguno. Las aserciones de los tests existentes no se tocaron; solo se añadieron
`core/formatos/VariacionTest.kt` (5 tests del formato de variación).

## Decisiones tomadas sin consultar
1. **Paleta**: se derivaron las superficies del oliva `#2D5A27` (hue 128° OKLCH, cálculo propio con
   script Python) en vez de ajustar los tokens Stitch: los grises cálidos de la escala M3 eliminan el
   tinte lavanda manteniendo la identidad del logo. Se renombran `PanelDestacado`/`PanelDestacadoFuerte`
   a `Contenedor`/`ContenedorAlto` (vocabulario M3) y se eliminan alias legados.
2. **Hueco de la barra inferior**: `AlturaBarraInferior = 80.dp` reservado en todas las pantallas para
   que la barra de navegación de la oleada 1 no tape contenido. En horizontal el hueco se ve grande
   (cerca de un tercio del alto); se puede revisar cuando la barra exista (p. ej. reservar solo en
   vertical).
3. **Iconos como vectores propios** (Material Symbols Rounded descargados y convertidos a
   VectorDrawable, viewport 960 con `<group android:translateY="960">` para corregir el eje Y del SVG)
   en vez de `material-icons-extended`, para no engordar el APK.
4. **Splash**: dependencia `androidx.core:core-splashscreen:1.0.1` con ADR 0010 y duración de animación
   400 ms, sin esperas artificiales.
5. **Transiciones del router** con `AnimatedContent` manteniendo el propietario de visita por
   `remember(visitaActual)`; el gesto atrás predictivo se activa a nivel de `<application>`.
6. **Icono en el texto de categoría de `FilaLabor`** por mapeo de categoría (`tratamiento`/`riego`/
   `cosecha`, resto `ic_hoja`): el icono acompaña al texto, nunca lo sustituye.
7. **El borde `#C1C6BA` se documenta como decorativo** (1,7:1): los campos se identifican por su
   etiqueta y los botones por relleno o contorno grueso de 2dp, para no prometer un 3:1 que no cumple
   un mero filete decorativo.
8. **`backend/iniciar-dev.sh`** para reanudar el backend de desarrollo sin exponer el fichero de
   entorno (el shell bloquea cualquier comando que lo mencione; el script hace el sourcing internamente
   sin imprimirlo).

## Pendiente / riesgos
- Los componentes `EsqueletoDeCarga`, `EstadoVacio`, `HostDeAvisos`/`mostrarConDeshacer`,
  `TarjetaDatoVariacion`, `ContenedorRefrescar` y `BotonMicrofonoFlotante` están creados y compilados
  pero **aún no se usan en pantallas**: los aplicarán las features de la oleada 1 (así lo pide la pista).
- En horizontal, el hueco reservado de la barra inferior ocupa mucho alto: decidido para no rehacerlo
  luego; revisable con la barra real.
- Para capturar en horizontal hay que usar `adb shell wm user-rotation lock 1/0` (este Xiaomi ignora
  `settings put system user_rotation` con la rotación automática desactivada).
- Apariencia de los swipes de uiautomator: los gestos deben empezar dentro del área de contenido
  (por debajo queda el margen + hueco reservado); no es un defecto de la app.
- Los textos de los mockups siguen sin adoptarse si no pasan por `CONTEXT.md`; el vocabulario nuevo
  (Perfil, Barra de navegación, Importe estimado…) ya está registrado en `CONTEXT.md`.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
Ya volcado en esta sesión: términos nuevos en `CONTEXT.md`, contraste y paleta en `DESIGN.md` (frontmatter
`colors` incluido), ADR 0010 para el splash y estado `passing` con evidencia en `feature_list.json`.
