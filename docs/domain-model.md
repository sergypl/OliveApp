# Domain Model — OlivApp

## Core Concepts
- **Agricultor (usuario):** dueño de todos los datos; identificado por email + contraseña.
- **Parcela:** unidad de terreno con nombre; pertenece a un agricultor.
- **Labor:** registro cronológico de una tarea (tratamiento, riego o cosecha) en una parcela y fecha. Puede originarse por voz o por formulario manual.
- **Nota de voz:** audio grabado + transcripción + borrador IA. Es el origen más común de una labor.
- **Borrador IA:** ficha propuesta (fecha, parcela, categoría y campos de la categoría) pendiente de confirmación.
- **Simulación:** cálculo bajo demanda de liquidación en limpio (kilos × rendimiento graso − ajuste industrial). No se persiste como entidad en el MVP.

Campos por categoría de labor:
- Tratamiento fitosanitario: producto, cantidad + unidad.
- Riego: cantidad (horas o volumen).
- Cosecha: modalidad (a vuelo / a suelo), kilos.

## Relationships
- Un Agricultor tiene N Parcelas y N Labores. Una Parcela pertenece a un Agricultor.
- Una Labor pertenece a exactamente una Parcela (y, a través de ella, a un Agricultor).
- Una Nota de voz produce como mucho un Borrador IA, y este como mucho una Labor. Una Labor puede existir sin Nota de voz (formulario manual).

## States and Lifecycles

**Nota de voz:** `pendiente → transcrita → confirmada | descartada`
- *pendiente:* grabada sin conexión. Transición: llega cobertura → transcribir automáticamente.
- *transcrita:* borrador IA visible esperando confirmación. Transiciones: confirmar → labor creada; rechazar → descartada.
- *confirmada:* labor en el cuaderno.
- *descartada:* el agricultor rechaza el borrador.
- Si la transcripción falla, la nota permanece pendiente con error visible y se reintenta.

**Labor:** `creada → editada → eliminada`. El cuaderno muestra solo labores vigentes; eliminar es borrado directo (sin papelera en el MVP).

**Parcela:** sin estados en el MVP (no se elimina ni archiva).

## Important Scenarios
1. **Dictado con cobertura:** grabar → transcribir → borrador → confirmar → labor en el cuaderno.
2. **Dictado sin cobertura:** la nota queda pendiente con aviso visible; al reconectar se transcribe y se pide confirmación.
3. **Corrección manual:** el agricultor edita cualquier campo de una labor desde el formulario auxiliar.
4. **Simulación:** introduce kilos y rendimiento graso y ve el aceite neto con el ajuste industrial aplicado.

## Edge Cases
- La IA no identifica la parcela: el borrador deja la parcela vacía y pide elegirla de la lista antes de confirmar.
- La IA no reconoce la categoría: mensaje claro de rechazo (solo hay 3 categorías en el MVP).
- Fechas relativas ("ayer", "hoy"): resolver a fecha concreta en el borrador antes de confirmar.
- Dos dictados del mismo día y parcela: dos labores separadas (no se fusionan).
- Audio demasiado corto o ilegible: pedir repetir el dictado.
- Varios pendientes en cola: se transcriben en orden de grabación.
- Eliminar una parcela con labores: fuera del MVP; las parcelas no se eliminan.
- Baja de cuenta: fuera del MVP (limitación documentada en riesgos).
