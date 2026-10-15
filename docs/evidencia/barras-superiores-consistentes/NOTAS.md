# Barras superiores consistentes y espaciado vertical equilibrado

Tarea del usuario (2026-10-15): *"la toolbar de la pantalla de cuaderno con la de simulador y el resto, no son consistentes en toda la app en algunas ni hay, y además hay mucho espacio por arriba y poco por abajo y queda mal, revisalo y corrígelo en una rama nueva"*.

## Diagnóstico (medido en capturas reales del Xiaomi, 1080x2400 = 392,7dp de ancho)

| Pantalla | Antes: hueco sobre el título | Barra superior |
|---|---|---|
| Panel | 32dp | ninguna (título en el contenido) |
| Perfil | 30dp | ninguna (título en el contenido) |
| Campaña | — | ninguna (solo botón "Volver al panel") |
| Cuaderno | 70dp | `LargeTopAppBar` de 96dp con el título alineado abajo |
| Simulador | 69dp | ídem |
| Parcelas | 70dp | ídem |
| Notas de voz / Nueva labor | — | `LargeTopAppBar` de **152dp** con el título alineado abajo |

Causas del "mucho espacio por arriba y poco por abajo":

1. `LargeTopAppBar` de Material alinea el título **al borde inferior** de una barra de 96dp (y 152dp con flecha de volver): 70dp de vacío encima del título y ~12dp por debajo.
2. Dos formas de barra distintas (con y sin `LargeTopAppBar`) y tres pantallas sin barra alguna: el título estaba a 30dp en unas y a 70dp en otras, y en el Panel/Perfil/Campaña se deslizaba bajo la barra de estado al hacer scroll.
3. Hueco inferior del contenido inconsistente: `pantallaCompleta()` reservaba 80dp + 24dp de margen y las pantallas con barra reservaban **exactamente 80dp** (la altura de la barra inferior), de modo que la última tarjeta quedaba pegada a la barra de navegación.
4. **Bug encontrado por el camino**: el botón flotante "Dictar nota" de "Mi cuaderno" estaba **completamente tapado** por la barra de navegación inferior (el `Scaffold` de Material lo colocaba en el borde inferior de la pantalla y el router dibuja encima la barra de navegación). El mismo problema afectaba al aviso con "Deshacer". Verificado en `antes-cuaderno.png`: no hay ni un píxel naranja del botón.

## Qué cambia

- **`BarraSuperior` única** (`core/ui/BarraSuperior.kt`) en **todas** las pantallas: 72dp de alto, título (`TextoTitulo1`, 28sp) centrado verticalmente con el mismo hueco arriba y abajo, alineado con el margen de contenido de 24dp, flecha de volver opcional con diana táctil de 48dp y el icono a 24dp (mismo margen que el contenido). No se encoge ni se esconde al hacer scroll: el título siempre está a la misma altura.
- **Esqueleto de pantalla único** (`core/ui/PantallaCompleta.kt`): `PantallaConBarraSuperior` (contenido sencillo que se desplaza con la pantalla) y `PantallaConBarraSuperiorEnLista` (contenido con su propio desplazamiento: línea de tiempo, refresco, formularios, composición centrada). Los dos comparten barra, márgenes y hueco inferior.
- **Ritmo vertical**: 8dp entre la barra y el contenido (≈24dp entre las cajas de texto del título y del contenido, el mismo ritmo que entre bloques de contenido), 24dp de margen lateral y **24dp + 80dp** de hueco inferior cuando hay barra de navegación (antes 80dp a secas en las pantallas con barra).
- **Panel**: el saludo ("Buenos días, Socio 1") pasa a ser el título de la barra (una línea), la cooperativa y el avatar de perfil quedan como primer bloque del contenido y el botón "Dictar nota" se coloca con el hueco común.
- **Campaña, Inicio de sesión, Registro y Bienvenida** reciben barra con su título (y flecha de volver donde ya había "Volver"): antes no tenían ninguna.
- **Botón flotante y aviso "Deshacer"** se dibujan **encima** del hueco de la barra de navegación (16dp de separación), no debajo de ella.

## Medida antes/después (primera tinta tras la barra de estado, mismo script de píxeles)

| Pantalla | Antes | Después |
|---|---|---|
| Panel | 32dp | 25dp |
| Cuaderno | 70dp | 25dp |
| Simulador | 69dp | 24dp |
| Parcelas | 70dp | 27dp |
| Perfil | 30dp | 25dp |

Las diferencias de 1-3dp entre pantallas son el ascenso/descenso de cada letra (la "ó" de "Liquidación" sube, la "p" de "parcelas" baja), no la posición de la barra.

Reparto vertical del título (cuaderno, medido en la captura): 25dp de tinta encima del título y ~27-32dp hasta el contenido — equilibrado, frente a los 70dp/12dp de antes.

## Recorrido verificado en el Xiaomi Redmi Note 10 Pro

- Las 5 pestañas con la barra y el título a la misma altura (`despues-panel.png`, `despues-cuaderno.png`).
- Pantalla de segundo nivel con flecha de volver y título a la misma altura (`despues-notas-de-voz.png`).
- Botón "Dictar nota" **visible** sobre la barra de navegación en "Mi cuaderno" (antes invisible).
- Eliminar una labor: el aviso "Labor eliminada de tu cuaderno." con "Deshacer" aparece **sobre** el botón flotante (borde inferior a 207dp del borde de la pantalla, medido en captura y con `uiautomator`), no tapado por la barra inferior; "Deshacer" repone la labor.

## Verificación

- `cd android && ./gradlew testDebugUnitTest lintDebug` con el puerto 8000 libre: **285 tests, 0 fallos** y lint sin errores.
- `cd backend && ./.venv/bin/python -m pytest -q`: **166 passed**.
- `./init.sh` completo en verde (pytest, healthcheck 200, datos sembrados con token, APK de desarrollo compilado).
