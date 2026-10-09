# Domain Model — OlivApp

## Core Concepts
- **Agricultor (socio):** dueño de todos sus datos; identificado por email + contraseña. En el MVP todo usuario es socio de una cooperativa.
- **Cooperativa:** entidad oleícola a la que pertenece el socio y que moltura su aceituna. Fuente de los datos oficiales de campaña. En el MVP es única, no tiene usuarios ni administración en la app.
- **Campaña:** temporada de cosecha (año agrícola, p. ej. 2024/2025). Agrupa las labores de "Mi cuaderno" y los datos oficiales de la cooperativa para el socio. La campaña de una labor se deriva de su fecha: la campaña oleícola X/Y va del 1 de noviembre de X al 31 de octubre de Y (p. ej. 15/12/2025 → 2025/2026).
- **Datos de campaña (oficiales):** kilos entregados, rendimiento graso y aceite obtenido que la cooperativa registra para el socio en una campaña. Solo lectura en el MVP.
- **Parcela:** unidad de terreno con nombre; pertenece a un socio.
- **Labor:** registro cronológico de una tarea (tratamiento, riego o cosecha) en una parcela y fecha, dentro de una campaña. Puede originarse por voz o por formulario manual.
- **Nota de voz:** audio grabado + transcripción + borrador IA. Es el origen más común de una labor. Grabación de hasta 60 segundos en MPEG-4/AAC (`.m4a`), guardada en el dispositivo y registrada en una cola local con su estado hasta que se transcribe.
- **Borrador IA:** ficha propuesta (fecha, parcela, categoría y campos de la categoría) pendiente de confirmación.
- **Simulación:** cálculo bajo demanda de liquidación en limpio (kilos × rendimiento graso − ajuste industrial). No se persiste como entidad en el MVP ni se compara con el dato oficial.

Campos por categoría de labor (alineados con SIEX según ADR 0005):
- Tratamiento fitosanitario: producto, plaga a controlar, cantidad + unidad.
- Riego: cantidad (horas o volumen).
- Cosecha: modalidad (a vuelo / a suelo), kilos.
- Toda labor: observaciones (opcional).

## Relationships
- Una Cooperativa agrupa N socios (Agricultores) y N Campañas.
- Un Agricultor pertenece a una Cooperativa (única en el MVP) y tiene N Parcelas, N Labores y N datos de campaña.
- Una Campaña agrupa las Labores de "Mi cuaderno" y los datos oficiales de cada socio en esa temporada.
- Una Parcela pertenece a un Agricultor.
- Una Labor pertenece a exactamente una Parcela (y, a través de ella, a un Agricultor) y se sitúa en una Campaña.
- Una Nota de voz produce como mucho un Borrador IA, y este como mucho una Labor. Una Labor puede existir sin Nota de voz (formulario manual).
- Las Labores del socio y los Datos de campaña de la cooperativa son fuentes separadas: se muestran uno al lado del otro, no se fusionan.

## States and Lifecycles

**Nota de voz:** `pendiente → transcrita → confirmada | descartada`
- *pendiente:* grabada sin conexión. Transición: llega cobertura → transcribir automáticamente.
- *transcrita:* borrador IA visible esperando confirmación. Transiciones: confirmar → labor creada; rechazar → descartada.
- *confirmada:* labor en el cuaderno.
- *descartada:* el socio rechaza el borrador.
- Si la transcripción falla, la nota permanece pendiente con error visible y se reintenta.

**Labor:** `creada → editada → eliminada`. El cuaderno muestra solo labores vigentes; eliminar es borrado directo (sin papelera en el MVP).

**Parcela:** sin estados en el MVP (no se elimina ni archiva).

**Campaña:** `actual | cerrada`. La actual admite nuevas labores; las cerradas son anteriores y de solo consulta. Los datos oficiales de la cooperativa no cambian desde la app.

## Important Scenarios
1. **Consultar campañas:** el socio entra, ve la campaña actual y abre una campaña anterior con sus datos oficiales de cooperativa y sus labores.
2. **Dictado con cobertura:** grabar → transcribir → borrador → confirmar → labor en el cuaderno (en la campaña correspondiente).
3. **Dictado sin cobertura:** la nota queda pendiente con aviso visible; al reconectar se transcribe y se pide confirmación.
4. **Corrección manual:** el socio edita cualquier campo de una labor desde el formulario auxiliar.
5. **Simulación:** introduce kilos y rendimiento graso y ve el aceite neto con el ajuste industrial aplicado.
6. **Relleno conversacional del borrador:** el dictado no trae todos los campos, el asistente pregunta por los que faltan y el socio responde por voz hasta completar la ficha; entonces confirma.

## Edge Cases
- La IA no identifica la parcela: el borrador deja la parcela vacía y pide elegirla de la lista antes de confirmar.
- La IA no reconoce la categoría: mensaje claro de rechazo (solo hay 3 categorías en el MVP).
- Fechas relativas ("ayer", "hoy"): resolver a fecha concreta en el borrador antes de confirmar.
- Fecha dictada fuera de la campaña actual: el borrador propone la campaña que corresponde a la fecha (actual o anterior) y el socio confirma.
- Dos dictados del mismo día y parcela: dos labores separadas (no se fusionan).
- Audio demasiado corto o ilegible: pedir repetir el dictado.
- Audio demasiado largo: la grabación se detiene sola a los 60 segundos y la nota queda guardada.
- Varios pendientes en cola: se transcriben en orden de grabación.
- Eliminar una parcela con labores: fuera del MVP; las parcelas no se eliminan.
- Del socio solo existen datos oficiales (sin labores propias): la campaña muestra los datos de cooperativa con el cuaderno vacío.
- Baja de cuenta: fuera del MVP (limitación documentada en riesgos).
