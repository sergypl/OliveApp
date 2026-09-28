# ADR 0002: OlivApp MVP como app Android nativa en lugar de web responsive

## Status
Accepted (2026-09-28). Supersede al [ADR 0001](0001-plataforma-web-responsive.md).

## Context
El ADR 0001 decidió un MVP de web responsive para reducir fricción. Tras replantear el producto, el usuario fija una nueva visión: app **Android nativa**, con un panel de campañas donde el socio consulta los datos fieles de su cooperativa (kilos, rendimiento, aceite), un cuaderno rellenable por voz con IA, y una calculadora de liquidación en limpio. El replanteo refuerza el valor de una experiencia móvil nativa (rendimiento, micrófono y almacenamiento local en el campo) y se asume el coste extra de desarrollo dentro del alcance académico.

## Decision
Construir el MVP como **app Android nativa (Kotlin + Jetpack Compose, Material 3)**, respaldada por un **backend ligero obligatorio** que protege la clave de la API de IA, autentica al socio (JWT) y expone la API de datos. Los datos de campaña de la cooperativa entran sembrados y son de solo lectura; la app no tiene administración de cooperativa.

## Alternatives Considered
- **Web responsive (ADR 0001):** la opción anterior; más simple y sin instalación, pero descartada por el usuario al optar por nativa.
- **App híbrida (React Native / Expo):** una base para Android e iOS; punto intermedio, descartada por preferir Kotlin/Compose nativo.
- **PWA instalable:** variante de la web; descartada junto con la web.

## Consequences
- Se preserva la feature estrella (dictado → borrador IA → confirmación) usando APIs nativas de audio de Android.
- Se gana experiencia nativa (rendimiento, almacenamiento local con Room, control del micrófono) a costa de build, firma, Gradle y demo en dispositivo real.
- El backend sigue siendo imprescindible: la clave de IA nunca viaja en el APK y el backend expone auth + datos.
- La cola offline vive en el dispositivo (Room); la sincronización al reconectar requiere idempotencia al confirmar.
- iOS y cualquier versión web quedan fuera del alcance; si el producto creciera, habría que replantear la plataforma.
