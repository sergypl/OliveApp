# Filtros de "Mi cuaderno" a todo el ancho

Encargo del usuario (2026-10-15): *"en la pantalla de cuaderno los filtros cuando haces scroll horizontal deberían de hacerlo al full screen y no esconderse detrás de los márgenes porque se ve muy feo el corte"*.

## Diagnóstico

Los chips de filtro ("Filtrar por categoría" y "Filtrar por parcela") se deslizaban a los lados **dentro** del margen de contenido: la fila vivía dentro de la caja ya rellenada de `PantallaConBarraSuperiorEnLista` (24dp a cada lado), de modo que al deslizarla los chips se cortaban en seco **en el margen**, a 24dp del borde de la pantalla, con el texto partido por la mitad (ver `antes-chips-cortados-en-el-margen.png`: "Tratamiento fitosanitari…" y "La Nori…" partidos en x=850px = 24dp del borde, en una pantalla de 1080px = 392,7dp).

Medido con `uiautomator` después del arreglo: la fila de chips ocupa `[0,474][1080,606]` — los **1080px completos** de la pantalla — mientras que el resto del contenido sigue en `[66,1014]` (los 24dp de margen).

## Qué cambia

- **Las filas que se deslizan a los lados van a todo el ancho de la pantalla**: los chips pasan **por debajo de los márgenes** (y del borde de la pantalla) sin cortarse, como un carrusel.
- El margen de contenido se aplica **dentro del área deslizante**, de modo que en reposo el primer chip y el último quedan alineados con el margen de 24dp como el resto del contenido (la etiqueta "Filtrar por categoría", las tarjetas, las cabeceras de campaña).
- Mecanismo en `core/ui`:
  - `Modifier.margenDeContenido()`: el margen horizontal de 24dp que aplica cada bloque cuando la pantalla se dibuja a todo el ancho.
  - `PantallaConBarraSuperiorEnLista(margenLateral = false)`: la pantalla va de borde a borde y se da sus propios márgenes (así una fila puede deslizarse por debajo de ellos).
  - `FilaDeChips` (cuaderno): `Row` deslizante a todo el ancho con el margen dentro del área deslizante.
- La cabecera fija de mes mantiene su fondo opaco **a todo el ancho** para que los chips (y las tarjetas) pasen por debajo sin transparentarse.
- Regla general recogida en `DESIGN.md` (Layout): los desplazamientos horizontales van a todo el ancho, con el primer y el último elemento alineados al margen de contenido.

## Verificado en el Xiaomi Redmi Note 10 Pro (1080x2400)

- En reposo: primer chip a 24dp del borde (x=66px), igual que la etiqueta y las tarjetas; los chips que no caben se salen por el borde de la pantalla (`despues-chips-al-borde.png`), sin corte en el margen.
- Deslizando: los chips pasan por debajo del borde de la pantalla — el chip se corta en x=0 (el propio borde), no en el margen (`despues-deslizando-por-debajo-del-borde.png`).
- Sin cambios en el resto del ritmo vertical: 24dp de margen, 8dp entre barra y contenido, cabeceras de campaña y de mes, tarjetas y hueco inferior como antes.

## Verificación

- `cd android && ./gradlew testDebugUnitTest lintDebug` con el puerto 8000 libre: **285 tests, 0 fallos** y lint sin errores.
- `cd backend && ./.venv/bin/python -m pytest -q`: **166 passed**.
- `./init.sh` completo en verde (pytest, healthcheck 200, datos sembrados, APK instalado y abierto).
