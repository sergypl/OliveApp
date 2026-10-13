# Set de frases de prueba — flujo voz→labor

Dictados representativos del vocabulario local para verificar el flujo voz→labor
(transcripción → borrador IA → labor). Con el tiempo deben ser 10-20 frases
cubriendo el vocabulario real de la campaña; este es el set de trabajo.

Cómo se usan: se dictan (o se sintetizan con voz en español) sobre la app y se
comprueba el borrador IA que propone. Las frases cubren las 3 categorías del MVP
y los casos borde del dominio (`docs/domain-model.md`).

| # | Frase dictada | Qué se espera |
|---|---|---|
| 1 | "He hecho un tratamiento fitosanitario en la parcela Cañada Honda contra la mosca del olivo con dos litros de cobre, el diez de octubre de dos mil veintiséis." | Tratamiento: fecha 2026-10-10, parcela Cañada Honda, producto cobre, plaga mosca del olivo, cantidad 2 L |
| 2 | "Ayer eché un tratamiento en El Encinar para las malas hierbas, un kilo de producto." | Tratamiento con fecha relativa (la resuelve `fecha-relativa-en-borrador`) y producto genérico |
| 3 | "He regado la parcela Los Llanos durante tres horas, el once de octubre de dos mil veintiséis." | Riego: cantidad 3 horas |
| 4 | "Regué La Umbría con cuatrocientos litros de agua, el doce de octubre de dos mil veintiséis." | Riego: cantidad 400 L |
| 5 | "He cosechado El Encinar a vuelo, ochocientos kilos de aceituna, el trece de octubre de dos mil veintiséis." | Cosecha: modalidad a vuelo, kilos 800 |
| 6 | "Recogida a suelo en Los Llanos, quinientos kilos, el uno de junio de dos mil veintiséis." | Cosecha: modalidad a suelo, kilos 500 |
| 7 | "He regado Cañada Honda dos horas. Además he visto cochinilla en algunos olivos." | Riego con observaciones |
| 8 | "Tratamiento contra el repilo en la parcela Canyada Honda, un kilo de cobre, el veinte de marzo de dos mil veintiséis." | Desambiguación de typo de parcela ("Canyada Honda" → Cañada Honda) |
| 9 | "Hoy he estado paseando por el campo y he visto pajaritos." | Rechazo claro: no hay categoría reconocible |
| 10 | "He regado la parcela de mi cuñado tres horas." | Borrador sin parcela (la elige el socio en `borrador-sin-parcela`) |
| 11 | "Ayer regué El Encinar dos horas." | Fecha relativa pendiente de resolver |
| 12 | "Tratamiento fitosanitario en La Umbría contra prays, trescientos gramos de producto, el veinte de marzo de dos mil veintiséis." | Tratamiento: plaga prays, cantidad 300 g |
| 13 | "He hecho tratamiento en El Encinar y luego he regado Los Llanos." | Dos labores en un dictado: solo cabe un borrador por nota (limitación conocida) |
| 14 | "He hecho un tratamiento en la parcela Cañada Honda contra la mosca del olivo, el diez de octubre de dos mil veintiséis." | Tratamiento sin producto ni cantidad: el asistente pregunta por lo que falta ("¿Qué producto has echado?") y las respuestas por voz completan la ficha |
| 15 | "He regado La Umbría, el doce de octubre de dos mil veintiséis." | Riego sin cantidad: el asistente pregunta "¿Cuánto has regado?" y la respuesta ("Tres horas.") completa la ficha |
| 16 | "He cosechado El Encinar, el trece de octubre de dos mil veintiséis." | Cosecha sin modalidad ni kilos: el asistente pregunta por la modalidad y después por los kilos, una pregunta por respuesta |

Respuestas de voz para completar el borrador (features de preguntas):
"Dos litros de cobre." (frase 14), "Tres horas." (frase 15) y "A vuelo, ochocientos kilos." (frase 16).

Frases verificadas en vivo (sesión 027): la 1 (borrador completo correcto) y la 9
(rechazo por categoría no reconocida).

Frases 14-16 verificadas en vivo (sesión 028, `completar-borrador-por-preguntas`):
la 14 dictada en el móvil físico dejó el borrador sin producto ni cantidad y el
asistente preguntó "¿Qué producto has echado?"; la respuesta por voz "Dos litros
de cobre." completó la ficha (Producto cobre, Cantidad 2 L) y quedó lista para
confirmar. También comprobado en vivo por API (gateway real): una respuesta que
no aporta el dato ("Hoy he visto pajaritos.") deja los campos intactos y el
asistente vuelve a preguntar por el mismo campo.
