# cuaderno-linea-de-tiempo — NOTAS

## Estado
`in_progress` en la pista: implementada y verificada con build + tests unitarios + lint. Falta la
verificación en dispositivo (la hace la sesión de integración, ver "Pendiente").

## Qué se hizo
- `cuaderno/domain/LineaDeTiempo.kt`: lógica pura de agrupación de las labores de cada campaña por mes
  (más reciente primero) y de filtros por categoría y parcela.
- `cuaderno/presentation/PantallaCuaderno.kt` + `LineaDeTiempo.kt` + `FilasLabor.kt`: Cuaderno como línea
  de tiempo con icono, color Y texto por categoría, cabeceras de mes fijas (`stickyHeader`) dentro de cada
  campaña, chips grandes con texto de filtro (`core/ui/Chips.kt`), deslizar para refrescar
  (`ContenedorRefrescar`), esqueleto al cargar (`EsqueletoDeCarga`), estado vacío "Apunta tu primera
  labor" y botón flotante "Dictar nota" (`BotonMicrofonoFlotante`) que recibe `onDictarNota` por
  parámetro.
- Borrado con **Deshacer**: se mantiene la confirmación previa ("¿Eliminar esta labor?"); al confirmar,
  la labor sale de la lista y aparece el aviso "Labor eliminada de tu cuaderno." con "Deshacer". Solo se
  borra en el backend si el aviso se cierra sin deshacer; al salir de la pantalla con el aviso abierto
  se borra; si el borrado falla la labor vuelve a la lista y se avisa.
- Parcelas: barra superior grande, esqueleto al cargar y estado vacío "Aún no tienes parcelas. Añade la
  primera".
- Nueva labor / edición: barra superior grande y esqueleto mientras cargan las parcelas.
- Notas de voz: barra superior grande y estado vacío (la cola local de Room se lee de forma síncrona,
  así que no hay carga que cubrir con esqueleto). Con una grabación en marcha el botón Volver de la barra
  no sale (igual que antes con el botón inferior, que se escondía).
- Arreglos al trabajo parcial heredado: un typo (`modidad`) en `NuevaLaborViewModel.elegirModalidad`,
  falta de `@OptIn(ExperimentalMaterial3Api)` en las pantallas que usan `PantallaConBarraSuperiorGrande`,
  y `ParcelasUiState.cargando` arranca en `true` para que no parpadee el estado vacío antes de cargar.

## Verificación ejecutada
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint 2>&1 | tail` → BUILD SUCCESSFUL
  (ver resultado final en las NOTAS de grabacion-voz-animada, que repite la verificación completa).
- Tests nuevos: `LineaDeTiempoTest` (agrupación por mes, orden, filtros) y casos nuevos en
  `CuadernoViewModelTest` (eliminar con Deshacer, deshacer, salir con el aviso abierto, fallo de red,
  filtros en el estado).

## Tests cambiados y por qué
- `CuadernoViewModelTest`: `confirmar eliminar borra la labor y recarga el cuaderno` ya no es cierto
  (ahora el borrado se difiere hasta cerrarse el aviso de Deshacer). Se sustituye por
  `confirmar eliminar quita la labor de la lista pero todavia no la borra` y
  `si no se deshace, al cerrarse el mensaje la labor se borra y se recarga`.

## Decisiones tomadas sin consultar
- Se conserva la confirmación de borrado además del Deshacer (la pista lo pedía si ya protegía).
- Notas de voz sin esqueleto (no hay carga asíncrona).

## Pendiente / riesgos
- **Cablear en `Navegacion.kt`** (no lo toco): `PantallaCuaderno(onDictarNota = { irA("notas_voz") })`.
  Hoy el parámetro por defecto es `{}` y el botón flotante no hace nada hasta que se cablee.
- La `BarraSuperiorGrande` de `core/ui` crea el `scrollBehavior` pero no aplica `nestedScroll` al
  contenido, así que no se encoge ni se esconde (cumple "barras que no se esconden", pero el título
  grande ocupa sitio). Si se quiere que se encoja, usar `exitUntilCollapsed` + `nestedScroll`.
- Verificar en dispositivo: cabeceras de mes fijas, chips con fuente grande, horizontal, refresco,
  Deshacer.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- CONTEXT.md: "Línea de tiempo: vista del Cuaderno que agrupa las labores de cada campaña por mes,
  con filtros por categoría y parcela."
- feature_list.json: `cuaderno-linea-de-tiempo` → passing tras verificar en dispositivo; evidencia
  `docs/evidencia/cuaderno-linea-de-tiempo/NOTAS.md`.
