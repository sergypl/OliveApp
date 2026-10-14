# ADR 0009: Tipografía Montserrat + Inter empaquetada como recursos

- Estado: aceptada (2026-10-14, aprobada por el usuario en la Fase 0 de `rediseno-visual-android`).
- Contexto: los mockups de `docs/design/stitch/` (sistema "Agro-Modern Olive Systems") usan Montserrat para títulos y cifras e Inter para cuerpo. La app usaba la tipografía del sistema. Regla del proyecto: ninguna dependencia nueva sin ADR y sin OK del usuario.

## Decisión

- Empaquetar **TTF estáticos** de ambas familias como recursos en `android/app/src/main/res/font/`:
  - Montserrat SemiBold (600) y Bold (700) — títulos y cifras destacadas.
  - Inter Regular (400) y SemiBold (600) — cuerpo, etiquetas y botones.
- **Sin dependencias Gradle nuevas ni descargas en runtime** (nada de Google Fonts descargables): las fuentes viajan en el APK y la app es idéntica sin conexión.
- Procedencia (upstreams oficiales, licencia SIL Open Font License 1.1, redistribución permitida):
  - Montserrat: `https://github.com/JulietaUla/Montserrat` (`fonts/ttf/`), copyright The Montserrat Project Authors.
  - Inter: `https://github.com/rsms/inter` (release v4.1, `extras/ttf/`), copyright The Inter Project Authors.
  - Texto de la licencia: `https://openfontlicense.org` (los ficheros `.ttf` conservan su `name` table con copyright y licencia; aviso incluido en este ADR por cumplimiento de la OFL).
- Los tamaños de Stitch se **escalan a la regla sénior** de `DESIGN.md`: cuerpo ≥ 18sp y nada por debajo de 16sp (los cuerpos 12-16px de Stitch se descartan por legibilidad).

## Consecuencias

- ~1 MB de assets de fuente en el APK; compatibles desde minSdk 26 (TTF estáticos: cero riesgo de ejes variables en gama modesta).
- `core/ui` define las `FontFamily` en un único sitio (sistema de diseño de la Fase 1); las pantallas no referencian los ficheros directamente.
- Si se añade un peso nuevo, se suma su TTF (misma licencia) y se registra aquí.
