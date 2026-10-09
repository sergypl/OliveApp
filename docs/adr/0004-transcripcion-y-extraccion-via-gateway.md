# ADR 0004: Transcripción y extracción por voz vía aliases del gateway de inferencia

## Status
Accepted (2026-09-28). Desbloquea el punto "endpoint de audio y modelo de texto del gateway" de `docs/risks-and-open-questions.md`. **Validado empíricamente (2026-10-09)** con una nota de voz real en español en el formato nativo de Android; ver *Validación empírica*.

## Context
La feature estrella de OlivApp es dictar una labor por voz: audio del socio → transcripción → extracción de una ficha estructurada `{fecha, parcela, categoría, campos de la categoría}` → borrador IA → confirmación. El gateway `inference.devexpert.io` expone una API compatible con OpenAI con aliases estables (el modelo real detrás puede cambiar sin cambiar el alias). Restricciones: la clave `DEVEXPERT_API_KEY` solo en el backend; coste acotado (dictados cortos, 3 categorías); precisión con acentos y vocabulario local es un riesgo declarado; el criterio acordado con el usuario es **empezar con los modelos más baratos y subir solo donde fallen**.

## Decision
1. **Transcripción:** alias **`transcribe`** (`/audio/transcriptions`, GPT-4o mini Transcribe) con el audio de la nota de voz tal y como lo graba Android. Formato candidato `m4a`, sin conversiones salvo que la validación demuestre que el gateway las exige (formato dejado a decidir empíricamente).
2. **Extracción:** alias **`chat`** (`/chat/completions`, DeepSeek V4.1 Flash) con salida JSON forzada y esquema `{fecha, parcela, categoría, campos de la categoría}`, apoyándose en la lista de parcelas del socio para desambiguar. Si el set de 10-20 frases de prueba muestra errores de extracción relevantes, se sube a **`chat-pro`** (MiMo V2.6 Pro) **solo para extracción**; la transcripción no se toca salvo que falle por su cuenta.
3. El código usa **aliases**, nunca nombres de modelo reales.
4. **Validación empírica obligatoria** antes de implementar la feature de voz: nota de voz real en español grabada en el móvil → `transcribe` → `chat`, midiendo formato aceptado, latencia percibida y coste por dictado. Punto abierto a verificar ahí: si el modo JSON forzado (`response_format`) está soportado vía el gateway; si no, alternativa de prompt "responde solo con JSON válido" con parseo y reintento.

## Validación empírica (2026-10-09)
Nota de voz real en español ("He hecho un tratamiento en la parcela La Noria contra la mosca del olivo con dos litros de producto, el veinte de enero.") en el formato con el que graba Android (MPEG-4/AAC, 16 kHz mono) contra `inference.devexpert.io`:

| Paso | Alias | Resultado | Latencia | Coste |
|---|---|---|---|---|
| Transcripción | `transcribe` | Español correcto, con typos menores ("Eche", "Lanoria") | 1,7 s | 55 tokens de audio + 30 de salida |
| Extracción | `chat` | JSON válido con `response_format: json_object`: fecha 2026-01-20, parcela "La Noria" (desambigua "Lanoria" con la lista del socio), categoría tratamiento, plaga mosca del olivo, cantidad 2 | 5,7 s | 1.277 tokens |

Conclusiones:
- El formato nativo de Android (m4a/AAC/16 kHz) se acepta tal cual: no hacen falta conversiones.
- El modo JSON forzado (`response_format`) sí está soportado vía el gateway: no hace falta el plan B de prompt.
- La latencia percibida es aceptable en el MVP (transcripción ~2 s; borrador ~6 s).
- Puntos a vigilar en las features de extracción: normalizar unidades a las listas cerradas ("litros" → "L"), relleno conversacional cuando falte un campo ("producto: No especificado") y corrección de typos de la transcripción con la lista de parcelas.

## Alternatives Considered
- **`chat-pro` desde el principio:** máxima calidad de extracción a cambio de más coste por dictado; descartado por el criterio "empezar barato y subir si falla".
- **Modelo grande de transcripción:** descartado; `transcribe` cubre español y es el único endpoint de audio del gateway.
- **Whisper local en backend o en el dispositivo:** sin depender del gateway, pero contradice la tesis del proyecto (IA vía gateway) y añade peso a dispositivos/portátil modestos. Descartado.
- **Endpoint `/decisions` (alias `jev`):** clasifica y puntúa criterios, no produce la ficha estructurada de la labor. No encaja en el pipeline.

## Consequences
- Pipeline modular: transcripción y extracción se escalan de modelo por separado sin tocar la app.
- Usar aliases hace el pipeline inmune a cambios del modelo real configurado por el servicio.
- El coste por dictado queda acotado (modelo pequeño de audio + modelo pequeño de texto) y se mide en la validación empírica.
- Si la precisión con acento/vocabulario local falla, el primer escalón de subida ya está decidido (`chat-pro` para extracción), sin reabrir la decisión.
- El riesgo de dependencia del gateway persiste (caída o cambio de API): aceptado para el MVP; el backend centraliza las llamadas y puede registrar errores (ya requerido en observabilidad).
