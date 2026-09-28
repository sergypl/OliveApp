# Contexto

Glosario compartido de OlivApp. Solo vocabulario del dominio: sin planes, decisiones ni detalles de implementación. Idioma: español.

## Glosario

### OlivApp
Aplicación Android para socios de cooperativa oleícola. Reúne el panel de campañas, el cuaderno de campo por voz y el simulador de liquidación en limpio. MVP del proyecto.

### Agricultor
Persona que cultiva olivar. Perfil del usuario de OlivApp, mayoritariamente sénior.

### Socio
Agricultor miembro de una cooperativa. En el MVP todo usuario de OlivApp es un socio y solo ve sus propios datos.

### Cooperativa
Entidad oleícola a la que pertenece el socio y que moltura su aceituna. Es la fuente fiel de los datos de campaña del socio. En el MVP sus datos se muestran en la app de solo lectura: no se administran desde la aplicación, no tiene usuarios propios.

### Campaña
Temporada de cosecha, identificada por su año agrícola (p. ej. 2024/2025). Agrupa las labores del socio en esa temporada y sus datos oficiales de cooperativa.

### Datos de la campaña
Datos oficiales que la cooperativa registra para el socio en una campaña: kilos entregados, rendimiento graso y aceite obtenido. En el MVP son de solo lectura.

### Kilos entregados
Aceituna que el socio lleva a la cooperativa durante una campaña.

### Aceite obtenido
Aceite resultante de molturar los kilos entregados en una campaña. Cifra oficial de la cooperativa.

### Panel
Pantalla de aterrizaje tras el inicio de sesión. Muestra la campaña actual, da acceso a las campañas anteriores y a los dos módulos: cuaderno de campo y simulador.

### Cuaderno de campo
Historial cronológico de labores de un socio, agrupado por campaña. En OlivApp es la vista "Mi cuaderno"; su estructura de campos imita la del SIEX.

### Mi cuaderno
Nombre de la sección donde el socio consulta y edita sus propias labores, con origen por voz o manual. Se contrapone a los datos oficiales de la cooperativa.

### SIEX
Plataforma digital oficial de la Junta de Andalucía para el cuaderno de campo del olivar. OlivApp imita su estructura de campos (categorías y datos por labor) para facilitar el volcado posterior; no lo integra ni exporta en el MVP.

### Parcela
Unidad de terreno de olivar con nombre propio. La crea manualmente el socio en el MVP. Cada labor se asigna a exactamente una parcela.

### Labor
Tarea realizada en una parcela en una fecha. Categorías del MVP: tratamiento fitosanitario, riego y cosecha.

### Tratamiento fitosanitario
Labor de aplicación de un producto fitosanitario: fecha, parcela, producto y cantidad (con unidad).

### Riego
Labor de riego: fecha, parcela y cantidad (horas o volumen, según dicte el socio).

### Cosecha
Labor de recolección: fecha, parcela, modalidad (a vuelo o a suelo) y kilos de aceituna. No se confunde con la campaña ni con los kilos entregados a la cooperativa.

### Cosecha a vuelo
Aceituna recolectada directamente del árbol.

### Cosecha a suelo
Aceituna caída al suelo y recogida de este.

### Nota de voz
Grabación corta dictada por el socio para registrar una labor. Estados: pendiente (grabada sin conexión), transcrita (borrador IA generado), confirmada (labor creada en el cuaderno), descartada.

### Borrador IA
Ficha propuesta por el módulo de IA a partir de la transcripción: fecha, parcela, categoría de tarea y campos de la categoría. Requiere confirmación del socio antes de convertirse en labor.

### Rendimiento graso
Porcentaje de aceite obtenible por peso de aceituna (típicamente entre el 18 % y el 25 %). Entrada principal del simulador y, además, dato oficial de campaña que registra la cooperativa.

### Ajuste industrial
Porcentaje configurable que se resta del aceite estimado para simular mermas del proceso de la almazara. Es la única deducción del MVP.

### Liquidación en limpio
Resultado del simulador: aceite neto estimado tras aplicar el ajuste industrial. Es una estimación orientativa, no el documento de liquidación real de una cooperativa.

### Simulación
Cálculo bajo demanda de una liquidación en limpio en el simulador. No se guarda como dato de campaña ni compite con las cifras oficiales de la cooperativa.

### Almazara
Fábrica donde se muele la aceituna para obtener aceite. Una cooperativa suele tener la suya; en OlivApp "almazara" nombra la fábrica, no la cooperativa.

## Términos rechazados / ambiguos

### App móvil
Ahora es literal: aplicación Android nativa (ver ADR 0002). No usar "web", "PWA" ni "navegador" para el MVP.

### Compatible con SIEX
Usar "estructura de campos SIEX": mismas categorías y datos por labor, sin exportación ni integración en el MVP.

### Liquidación (a secas)
Usar "liquidación en limpio". En OlivApp siempre es una estimación simplificada, no la liquidación oficial de la cooperativa.

### Campaña / cosecha
"Campaña" es la temporada (2024/2025); "cosecha" es la labor de recolección. No usar "campaña" para nombrar una labor ni "cosecha" para nombrar la temporada.

### Cooperativa / almazara
La cooperativa es la entidad y el origen de los datos; la almazara es la fábrica. No usarlos como sinónimos.

### Comando de voz
Usar "nota de voz". El socio dicta; no ejecuta comandos.
