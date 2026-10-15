# perfil-del-socio — NOTAS

## Estado
Implementada y verificada con pytest, tests unitarios, `assembleDebug` y `lint` (sin dispositivo: las
pistas no usan adb). **Pendiente de la sesión de integración**: comprobación visual en el Xiaomi
(pestaña Perfil con fuente 1,3× y horizontal, estado sin conexión con el backend parado, cerrar sesión)
y paso a `passing`. Depende de `navegacion-barra-inferior` (misma rama).

## Qué se hizo
- **Backend**: `backend/app/perfil.py` (función `perfil_de_la_cuenta` + `crear_router`) y
  `GET /api/v1/mi-perfil`; en `main.py` solo el `import` y `app.include_router(...)`. Devuelve
  `{codigo_socio, email, cooperativa, numero_parcelas, campana_actual}`; todo se resuelve desde la
  cuenta del token (nunca desde un id de la petición). Cuenta inexistente → 401.
- **Android** (paquete nuevo `perfil`, capas presentation/domain/data):
  - `domain`: `DatosPerfil`, `PerfilRepositorio`, `inicialesDe(email)` (iniciales del avatar).
  - `data`: `PerfilRepositorioImpl` (delega en `ServicioCuentas.cargarMiPerfil`) y `ModuloPerfil` (Hilt).
  - `presentation`: `PerfilViewModel` (cargar/reintentar, cerrar sesión) y `PantallaPerfil` (avatar de
    iniciales, email, tarjeta con código de socio, cooperativa, parcelas y campaña actual, aviso de
    datos de demostración (ADR 0006), versión `BuildConfig.VERSION_NAME`, «Cerrar sesión»).
    Estados: carga (`EsqueletoDeCarga`), error y «Sin conexión» (icono + texto + «Reintentar»).
  - `build.gradle.kts`: `buildFeatures.buildConfig = true` (AGP lo desactiva por defecto) para leer la
    versión. No toca permisos.
- **«Cerrar sesión» movido del Panel al Perfil** con el mismo comportamiento (borra el token y vuelve
  a la bienvenida; no tenía diálogo de confirmación y no se ha añadido). `PanelViewModel` pierde
  `cerrarSesion`/`sesionCerradaAtendida` y `PantallaPanel` pierde `onSesionCerrada`.
- **Docs**: `docs/domain-model.md` (Perfil como vista) y `docs/technical-discovery.md` (endpoint).

## Verificación ejecutada
- `cd backend && ./.venv/bin/python -m pytest -q` → 129 passed (5 nuevos en `tests/test_perfil.py`:
  con token, número de parcelas, sin token 401, token no válido 401, aislamiento entre socios).
- `cd android && ./gradlew assembleDebug testDebugUnitTest lint` → BUILD SUCCESSFUL; 149 tests
  unitarios, 0 fallos (nuevos: `PerfilViewModelTest` 7, `InicialesTest` 4, más `PestanasTest` 12).

## Tests cambiados y por qué
- `PanelViewModelTest`: se elimina `cerrar sesion borra el token y lo avisa una sola vez`, porque la
  feature mueve «Cerrar sesión» al Perfil. El mismo caso (con las mismas aserciones) vive ahora en
  `PerfilViewModelTest.cerrar sesion borra el token y lo avisa una sola vez`.

## Decisiones tomadas sin consultar
- El avatar usa las iniciales del email (la cuenta no tiene nombre): `juan.perez@…` → «JP».
- El aviso de demostración es un texto fijo en la app (la `declaracion` de `/api/v1/cooperativa` es
  larga y técnica, con referencias a ADR); el texto sigue ADR 0006.
- «Sin conexión» se detecta por el prefijo del mensaje que ya usa `ServicioCuentas` («No hay
  conexión…»), igual que el resto de pantallas distinguen mensajes.
- El email se muestra bajo el avatar (no como fila de la tarjeta) para que un email largo no rompa la
  fila etiqueta/valor.
- Sin campaña actual se muestra «Sin campaña» (el backend devuelve `null`).

## Pendiente / riesgos
- Verificación visual en dispositivo (ver Estado).
- La pista `panel` ya no encontrará «Cerrar sesión» en `PantallaPanel`: no debe reintroducirlo.

## Texto propuesto para CONTEXT.md, docs y feature_list.json
- CONTEXT.md, término **Perfil del socio**: «Pestaña de solo lectura con el código de socio, el email,
  la cooperativa, el número de parcelas y la campaña actual del socio, el aviso de datos de
  demostración, la versión de la app y "Cerrar sesión".»
- docs/risks-and-open-questions.md (límites asumidos): «Perfil de solo lectura: sin edición de datos,
  cambio de contraseña ni foto en el MVP. El avatar usa las iniciales del email porque la cuenta no
  guarda nombre; si la cooperativa aporta el nombre del socio, bastaría ampliar `/api/v1/mi-perfil`.»
- feature_list.json `perfil-del-socio`: tras la comprobación en dispositivo, `passing` con evidencia
  «pytest 129/129, tests unitarios 149/149, assembleDebug y lint en verde; <capturas en Xiaomi>».
