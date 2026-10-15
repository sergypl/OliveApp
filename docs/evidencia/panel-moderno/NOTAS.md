# panel-moderno — notas de evidencia

## Estado
Terminada y verificada en el Xiaomi (M2101K6G) el 2026-10-15, en `feature/modernizacion-android`.

## Qué se hizo
- **Cabecera** (`PantallaPanel.kt`): saludo según la hora ("Buenos días / Buenas tardes / Buenas noches, Socio 1"), cooperativa debajo y avatar de iniciales a la derecha (56 dp, rol botón, "Abrir tu perfil") que abre la pestaña Perfil. Datos de `/api/v1/mi-perfil`; si falla el perfil la cabecera se degrada (saludo sin nombre y avatar con icono) y el panel carga igual.
- **Tarjeta destacada** de la campaña actual: título de campaña, etiqueta "Oficial · Cooperativa", aceite obtenido como cifra grande (verde oliva), kilos entregados y rendimiento graso como filas secundarias y la frase de que las labores van aparte, en el cuaderno de campo. Sustituye a los tres bloques anteriores.
- **Comparativa** (`campana/domain/Comparativa.kt`): `TarjetaDatoVariacion` ("+48 %" con icono de subida/bajada y la referencia "Frente a la campaña 24/25 (1.198 kg)") y un gráfico de barras horizontales del aceite de las campañas disponibles. Cada barra lleva campaña y valor escritos; la actual se marca con "(actual)" y barra rellena, las anteriores con contorno: no depende solo del color. Sin campaña anterior (o sin cifras) no se muestra nada.
- **Campañas anteriores** como filas de lista (nombre, aceite obtenido y flecha, 72 dp mínimo) que abren el detalle como antes.
- **Dictar nota**: `BotonMicrofonoFlotante` (el mismo del Cuaderno) sobre la barra inferior; abre Notas de voz.
- **Deslizar para refrescar** (`ContenedorRefrescar`), **esqueleto** al cargar y **error con Reintentar** (`EstadoDeError`): un fallo de red sin datos ya no se ve como "no hay datos". Con datos en pantalla, un fallo al refrescar los conserva y avisa debajo.
- **Backend (único cambio imprescindible)**: `GET /api/v1/mi-campanas` ahora incluye `aceite_obtenido` (kg) en cada campaña; el listado no traía ninguna cifra y la alternativa era pedir el detalle de cada campaña (N+1). Campo añadido, sin cambios de contrato en el resto; test actualizado.
- `PanelViewModel` pasa a su propio fichero y recibe también `PerfilRepositorio`.

## Verificación ejecutada
- `./init.sh` completo: pytest 156/156, healthcheck 200, datos sembrados con token OK, APK compilado, instalado y abierto en el Xiaomi.
- `cd android && ./gradlew testDebugUnitTest lint` (puerto 8000 libre): 257 tests, 0 fallos, 0 omitidos (+26 nuevos: `ComparativaTest`, `SaludoTest` y los del ViewModel); lint en verde.
- En el móvil (backend de desarrollo + `adb reverse`): cabecera, tarjeta, comparativa +48 % (1.774 kg frente a 1.198 kg), gráfico con 4 campañas y lista de 3 anteriores; avatar → Perfil; Dictar nota → Notas de voz; fila de campaña → detalle de la campaña cerrada; atrás vuelve al panel; deslizar muestra el indicador y recarga; sin backend: refresco con datos muestra "No hay conexión…" debajo de la tarjeta y arranque en frío muestra "Sin conexión" + Reintentar; con un servidor que no responde se ve el esqueleto. Capturas con fuente normal (01-07), fuente 150 % (08-11) y horizontal (12-14).

## Tests cambiados y por qué
- `PanelViewModelTest`: el constructor recibe ahora `PerfilRepositorio` (cabecera). `si fallan las campanas…` ya no mira `mensaje` sino `errorDeCampanas`: los fallos de campañas y de datos oficiales tienen campos separados (antes el último en llegar pisaba al otro). El resto de aserciones originales no cambia.
- `FakeCampanasRepositorio` gana modo diferido y contador de llamadas (solo soporte de test).
- `test_campanas_anteriores.py`: se espera el campo nuevo `aceite_obtenido`.

## Decisiones tomadas sin consultar
- Aceite por campaña en `mi-campanas` (arriba) en vez de N+1 peticiones al detalle.
- La campaña anterior es la cerrada más reciente con cifra de aceite. Variación en % **entero** (medios alejándose del cero) y sin variación si alguna cifra es 0 (se reutiliza `variacionPorcentual` de core/formatos); con "0 %" se dice "Igual que…" sin icono de subida/bajada.
- Saludo: buenos días 6:00-13:59, buenas tardes 14:00-20:59, buenas noches el resto.
- Gráfico de barras horizontales propio en Compose (sin librería): con fuente al 150 % las verticales no caben.
- Espacios duros en "Socio 1" y en "1.198 kg" para que no se partan en dos líneas.

## Pendiente / riesgos
- En horizontal el botón flotante tapa parte de los valores de la derecha mientras se desplaza (igual que en el Cuaderno); se ve todo al desplazar. La zona visible en horizontal es pequeña por el hueco de la barra inferior (ya anotado en la oleada 0). La franja lavanda a la izquierda en horizontal es del fondo de la ventana y afecta a todas las pestañas (no es de esta feature).
- La comparativa depende de `aceite_obtenido` en `mi-campanas`: con un backend antiguo no se muestra (degrada bien).
- No probado: lectura con TalkBack.

## Texto propuesto
CONTEXT.md (Panel) actualizado en este mismo cambio; `feature_list.json` → `panel-moderno` en `passing`.
