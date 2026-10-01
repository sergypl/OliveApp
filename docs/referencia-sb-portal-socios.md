# Referencia de producto — SB Portal Socios

Análisis de la app real del sector usada como referencia (no como especificación). Fuente: SB Portal Socios (SB Software), app de consulta para socios de cooperativas y almazaras cliente de sus "Portales Web para Socios" (Google Play / App Store, v5.9.8), analizada con capturas reales de un socio de una cooperativa oleícola andaluza (octubre 2026).

> **Privacidad:** este documento y el repo no contienen datos personales ni identificadores de la cooperativa real (nombre, DNI, CIF, email, números de parcela). Solo magnitudes y conceptos, anonimizados.

## Qué es

App de **solo consulta** donde el socio ve los datos que su cooperativa ya registra: entregas de aceituna, análisis de laboratorio, facturas, avisos y trámites. No registra labores, no tiene cuaderno de campo, no tiene IA ni estimaciones: es el "panel de datos oficiales" del sector.

## Arquitectura de información (qué ve un socio real)

- **Acceso:** identificarse / demostración / info.
- **Vistas:** Almazara (Vales + Totales por producto), Facturas, Noticias, DAT, Impuestos.
- **Almazara → Vales:** lista de entregas (producto, nº de vale, fecha, rdto. laboratorio, kilos) + total de kilos de la campaña.
- **Almazara → Detalle del vale:** producto, kilos, **rdto. y aceite de laboratorio**, **rdto. y aceite industrial**, kg sin analizar, acidez %, humedad %, grasa sobre seco, tolva, término municipal, finca/polígono/parcela/recinto/paraje, tiempo de limpieza.
- **Perfil:** socio (cooperativa, rol), cerrar sesión.
- **Noticias:** avisos de la cooperativa (hojarascas, horarios, anticipos de campaña).

## Mapeo de conceptos: SB ↔ OlivApp

| Concepto real (SB) | Equivalente en OlivApp | Nota |
|---|---|---|
| Producto `ACEITUNA ARBOL` / `ACEITUNA` | Modalidad **a vuelo** / **a suelo** | El vocabulario del usuario coincide con el real |
| Vale (nº, fecha, kilos) | Entrega parcial dentro de una campaña | Sin entidad propia en el MVP: los datos de campaña son el agregado |
| Rdto. Laboratorio → Aceite Laboratorio | **Rendimiento graso** (dato oficial) | Es el número que el socio conoce del análisis |
| Rdto. Industrial → Aceite Industrial | **Aceite obtenido** (dato oficial) | Lo que realmente se recibe tras la merma |
| Merma lab → industrial (≈ 9,7 % relativo, medido en dos agregados reales) | **Ajuste industrial** del simulador | Por defecto 10 % (ver ADR 0007) |
| Acidez, humedad, grasa sobre seco, kg sin analizar | Fuera del núcleo (viven en observaciones si acaso) | Opcionales del CUE/almazara |
| Finca / polígono / parcela / recinto / paraje / término | **Parcela** (nombre propio) | El mundo real usa identificación SIGPAC; el MVP usa nombre, decisión documentada en ADR 0005 |
| Facturas, DAT, Impuestos | Fuera del MVP | Posibles extensiones si el producto crece |
| Noticias de la cooperativa | Fuera del MVP | Posible extensión; la cooperativa no administra nada en OlivApp |

## Datos reales observados (anonimizados)

Campaña 2025/2026 de un socio real, 5 vales de entrega:

| Fecha | Producto | Kilos | Rdto. lab. |
|---|---|---|---|
| 26/11/2025 | árbol (a vuelo) | 1.878 | 15,43 % |
| 27/11/2025 | árbol (a vuelo) | 4.352 | 19,20 % |
| 28/11/2025 | árbol (a vuelo) | 1.179 | 19,85 % |
| 29/11/2025 | árbol (a vuelo) | 1.600 | 15,79 % |
| 27/02/2026 | suelo | 1.949 | 18,08 % |
| **Total** | | **10.958** | **17,93 %** (medio ponderado) |

Validaciones extraídas:
- `aceite lab = kilos × rdto lab` y `aceite industrial = kilos × rdto industrial`, al detalle decimal (confirma la regla de coherencia de la ADR 0006).
- La merma lab → industrial es constante y medible: 9,71 % y 9,71 % en los dos agregados comprobados.
- Los vales a vuelo son de noviembre y el de suelo de febrero: coherente con el calendario real de recolección.

## Lecciones de UX

Copiar (arquitectura de información):
- El socio piensa en **entregas** (vales) y **totales por producto**, no en informes: el panel debe mostrar primero kilos y aceite, el detalle después.
- El "total de kilos de campaña" como cierre de la lista: es la cifra que el socio busca.
- Noticias de la cooperativa como canal de comunicación (fuera del MVP, pero mide qué espera el usuario).

No copiar (diseño):
- Diálogos modales apilados con botón "Continuar" para cerrar: un solo toque para salir, sin modales.
- Textos diminutos, mayúsculas continuas y jerarquía gris sobre gris: contraproducente para el perfil sénior (ver `DESIGN.md`).
- Campos crudos del sistema interno (tolva, puesto de compra, recinto 0, finca 0) sin traducir al lenguaje del socio: en OlivApp solo se muestra vocabulario de `CONTEXT.md`.
- Precisión absurda en decimales (17,893561): redondeo humano (17,9 %).
