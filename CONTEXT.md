# Contexto

Glosario compartido de OlivApp. Solo vocabulario del dominio: sin planes, decisiones ni detalles de implementación. Idioma: español.

## Glosario

### OlivApp
Cuaderno agrícola e inteligencia oleícola. MVP del proyecto: aplicación web responsive con tres módulos — registro de labores por voz, cuaderno de campo y simulador de liquidación en limpio.

### Cuaderno de campo
Historial cronológico de labores de un agricultor. En OlivApp es la vista que lista las labores registradas; su estructura de campos imita la del SIEX.

### SIEX
Plataforma digital oficial de la Junta de Andalucía para el cuaderno de campo del olivar. OlivApp imita su estructura de campos (categorías y datos por labor) para facilitar el volcado posterior; no lo integra ni exporta en el MVP.

### Agricultor
Usuario registrado de OlivApp. Es dueño de sus parcelas, labores y notas de voz; solo él puede verlas y editarlas.

### Parcela
Unidad de terreno de olivar con nombre propio. La crea manualmente el agricultor en el MVP. Cada labor se asigna a exactamente una parcela.

### Labor
Tarea realizada en una parcela en una fecha. Categorías del MVP: tratamiento fitosanitario, riego y cosecha.

### Tratamiento fitosanitario
Labor de aplicación de un producto fitosanitario: fecha, parcela, producto y cantidad (con unidad).

### Riego
Labor de riego: fecha, parcela y cantidad (horas o volumen, según dicte el agricultor).

### Cosecha
Labor de recolección: fecha, parcela, modalidad (a vuelo o a suelo) y kilos de aceituna.

### Cosecha a vuelo
Aceituna recolectada directamente del árbol.

### Cosecha a suelo
Aceituna caída al suelo y recogida de este.

### Nota de voz
Grabación corta dictada por el agricultor para registrar una labor. Estados: pendiente (grabada sin conexión), transcrita (borrador IA generado), confirmada (labor creada en el cuaderno), descartada.

### Borrador IA
Ficha propuesta por el módulo de IA a partir de la transcripción: fecha, parcela, categoría de tarea y campos de la categoría. Requiere confirmación del agricultor antes de convertirse en labor.

### Rendimiento graso
Porcentaje de aceite obtenible por peso de aceituna (típicamente entre el 18 % y el 25 %). Entrada principal del simulador junto a los kilos de cosecha.

### Ajuste industrial
Porcentaje configurable que se resta del aceite estimado para simular mermas del proceso de la almazara. Es la única deducción del MVP.

### Liquidación en limpio
Resultado del simulador: aceite neto estimado tras aplicar el ajuste industrial. Es una estimación orientativa, no el documento de liquidación real de una cooperativa.

### Almazara
Fábrica donde se muele la aceituna para obtener aceite. Contexto del simulador.

## Términos rechazados / ambiguos

### App móvil
Usar "aplicación web responsive". El MVP se accede desde el navegador del móvil (ver ADR 0001).

### Compatible con SIEX
Usar "estructura de campos SIEX": mismas categorías y datos por labor, sin exportación ni integración en el MVP.

### Liquidación (a secas)
Usar "liquidación en limpio". En OlivApp siempre es una estimación simplificada, no la liquidación oficial de la cooperativa.

### Comando de voz
Usar "nota de voz". El agricultor dicta; no ejecuta comandos.
