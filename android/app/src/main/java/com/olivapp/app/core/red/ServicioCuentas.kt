package com.olivapp.app.core.red

import android.os.Handler
import android.os.Looper
import com.olivapp.app.AlmacenNotasVoz
import com.olivapp.app.DatosCampana
import com.olivapp.app.DatosCampanaDeLabores
import com.olivapp.app.DatosCampanaResumen
import com.olivapp.app.DatosLabor
import com.olivapp.app.DatosPanel
import com.olivapp.app.DatosParcela
import com.olivapp.app.core.formatos.formatearLabor
import com.olivapp.app.core.formatos.formatearNumero
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

/** Llamadas al backend para cuentas y sesión. Responde siempre en el hilo principal. */
object ServicioCuentas {
    private val principal = Handler(Looper.getMainLooper())

    fun crearCuenta(
        email: String,
        contrasena: String,
        codigoSocio: String,
        alTerminar: (Boolean, String) -> Unit
    ) {
        val cuerpo = JSONObject()
            .put("email", email)
            .put("contrasena", contrasena)
            .put("codigo_socio", codigoSocio)
            .toString()
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/registro")
            .post(cuerpo.toRequestBody(ClienteRed.json))
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Cuenta creada. Ya puedes iniciar sesión."
                    } else {
                        ClienteRed.detalleDeError(it).ifEmpty { "No se pudo crear la cuenta. Revisa los datos." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Inicia sesión; devuelve el token (o null) y un mensaje claro. */
    fun iniciarSesion(email: String, contrasena: String, alTerminar: (String?, String) -> Unit) {
        val cuerpo = JSONObject()
            .put("email", email)
            .put("contrasena", contrasena)
            .toString()
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/login")
            .post(cuerpo.toRequestBody(ClienteRed.json))
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val token = JSONObject(it.body?.string() ?: "").optString("token")
                        principal.post { alTerminar(token, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo iniciar sesión. Revisa los datos." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta los datos del panel del socio (llamada autenticada). */
    fun cargarMiPanel(token: String, alTerminar: (DatosPanel?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mi-panel")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "")
                        val campana = cuerpo.optJSONObject("campana")?.optString("nombre") ?: ""
                        val oficiales = cuerpo.optJSONObject("datos_oficiales") ?: JSONObject()
                        val datos = DatosPanel(
                            campana = campana,
                            kilosEntregados = formatearNumero(
                                oficiales.optDouble("kilos_entregados"), 0
                            ) + " kg",
                            rendimientoGraso = formatearNumero(
                                oficiales.optDouble("rendimiento_graso"), 2
                            ) + " %",
                            aceiteObtenido = formatearNumero(
                                oficiales.optDouble("aceite_obtenido"), 0
                            ) + " kg"
                        )
                        principal.post { alTerminar(datos, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus datos." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Guarda una labor del socio (llamada autenticada). */
    fun guardarLabor(token: String, cuerpo: JSONObject, alTerminar: (String?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-labores")
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toString().toRequestBody(ClienteRed.json))
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val respuesta = JSONObject(it.body?.string() ?: "")
                        val campana = respuesta.optString("campana_id")
                        principal.post {
                            alTerminar(
                                campana,
                                "Labor guardada en la campaña " +
                                    campana.replace("-", "/") + "."
                            )
                        }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo guardar la labor." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /**
     * Envía una nota de voz al backend para transcribirla (llamada autenticada).
     *
     * El resultado lleva la transcripción y su borrador IA (feature
     * borrador-ia-desde-transcripcion; el borrador va vacío si la respuesta no
     * lo trae y con el marcador de rechazo si la IA no reconoció la categoría).
     * El aviso distingue tres fallos (features cola-offline-de-notas y
     * reintento-de-transcripcion-fallida): sin conexión (la nota vuelve a la
     * cola local), audio inválido (el backend responde 400: ilegible o
     * demasiado corto, hay que repetir el dictado) y fallo del servicio de
     * transcripción (el resto: error visible reintentable).
     */
    fun transcribirNota(
        token: String,
        fichero: java.io.File,
        alTerminar: (String?, String, String, Boolean, Boolean) -> Unit
    ) {
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                fichero.name,
                fichero.asRequestBody("audio/mp4".toMediaType())
            )
            .build()
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-notas-voz/transcripcion")
            .header("Authorization", "Bearer $token")
            .post(cuerpo)
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(
                        null,
                        "",
                        "No hay conexión con el servidor. Inténtalo de nuevo.",
                        true,
                        false
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val respuesta = JSONObject(it.body?.string() ?: "")
                        val texto = respuesta.optString("texto")
                        // null en la respuesta = borrador rechazado por la IA.
                        val borrador = when {
                            !respuesta.has("borrador") -> ""
                            respuesta.isNull("borrador") ->
                                AlmacenNotasVoz.BORRADOR_RECHAZADO
                            else -> respuesta.optJSONObject("borrador")?.toString()
                                ?: AlmacenNotasVoz.BORRADOR_RECHAZADO
                        }
                        principal.post { alTerminar(texto, borrador, "", false, false) }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo transcribir la nota." }
                        principal.post {
                            alTerminar(null, "", detalle, false, it.code == 400)
                        }
                    }
                }
            }
        })
    }

    /**
     * Completa un borrador IA con la respuesta por voz del socio (feature
     * completar-borrador-por-preguntas).
     *
     * El resultado lleva el texto de la respuesta y el borrador actualizado,
     * con la siguiente pregunta del asistente (vacía cuando la ficha ya está
     * completa). Los fallos distinguen los mismos casos que en la
     * transcripción: sin conexión (la respuesta grabada se conserva y se
     * reintenta), audio no usable (hay que volver a responder) y fallo del
     * servicio (reintentable).
     */
    fun completarBorrador(
        token: String,
        fichero: java.io.File,
        borrador: String,
        alTerminar: (String?, String, String, Boolean, Boolean) -> Unit
    ) {
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                fichero.name,
                fichero.asRequestBody("audio/mp4".toMediaType())
            )
            .addFormDataPart("borrador", borrador)
            .build()
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/borradores/completar")
            .header("Authorization", "Bearer $token")
            .post(cuerpo)
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(
                        null,
                        "",
                        "No hay conexión con el servidor. Inténtalo de nuevo.",
                        true,
                        false
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val respuesta = JSONObject(it.body?.string() ?: "")
                        val texto = respuesta.optString("texto")
                        val borradorNuevo = respuesta.optJSONObject("borrador")?.toString()
                            ?: ""
                        principal.post { alTerminar(texto, borradorNuevo, "", false, false) }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo completar el borrador." }
                        principal.post {
                            alTerminar(null, "", detalle, false, it.code == 400)
                        }
                    }
                }
            }
        })
    }

    /** Consulta las parcelas del socio (llamada autenticada). */
    fun cargarParcelas(token: String, alTerminar: (List<DatosParcela>?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-parcelas")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val arreglo = JSONArray(it.body?.string() ?: "[]")
                        val lista = mutableListOf<DatosParcela>()
                        for (i in 0 until arreglo.length()) {
                            val p = arreglo.optJSONObject(i) ?: continue
                            lista.add(DatosParcela(p.optString("id"), p.optString("nombre")))
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus parcelas." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Edita cualquier campo de una labor existente (llamada autenticada). */
    fun actualizarLabor(
        token: String,
        id: String,
        cuerpo: JSONObject,
        alTerminar: (Boolean, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-labores/" + id)
            .header("Authorization", "Bearer $token")
            .put(cuerpo.toString().toRequestBody(ClienteRed.json))
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Cambios guardados en tu cuaderno."
                    } else {
                        ClienteRed.detalleDeError(it).ifEmpty { "No se pudieron guardar los cambios." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Elimina una labor existente: borrado directo, sin papelera (llamada autenticada). */
    fun eliminarLabor(token: String, id: String, alTerminar: (Boolean, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-labores/" + id)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Labor eliminada de tu cuaderno."
                    } else {
                        ClienteRed.detalleDeError(it).ifEmpty { "No se pudo eliminar la labor." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Carga "Mi cuaderno": las labores del socio agrupadas por campaña. */
    fun cargarMisLabores(token: String, alTerminar: (List<DatosCampanaDeLabores>?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-labores")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "{}")
                        val arreglo = cuerpo.optJSONArray("campanas") ?: JSONArray()
                        val lista = mutableListOf<DatosCampanaDeLabores>()
                        for (i in 0 until arreglo.length()) {
                            val c = arreglo.optJSONObject(i) ?: continue
                            val filas = c.optJSONArray("labores") ?: JSONArray()
                            val labores = mutableListOf<DatosLabor>()
                            for (j in 0 until filas.length()) {
                                val l = filas.optJSONObject(j) ?: continue
                                labores.add(formatearLabor(l))
                            }
                            lista.add(
                                DatosCampanaDeLabores(
                                    nombre = c.optString("nombre"),
                                    esActual = c.optBoolean("es_actual"),
                                    labores = labores
                                )
                            )
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo cargar tu cuaderno." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Crea una parcela del socio (llamada autenticada). */
    fun crearParcela(token: String, nombre: String, alTerminar: (Boolean, String) -> Unit) {
        val cuerpo = JSONObject().put("nombre", nombre).toString()
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mis-parcelas")
            .header("Authorization", "Bearer $token")
            .post(cuerpo.toRequestBody(ClienteRed.json))
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(false, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val exitosa = it.isSuccessful
                    val detalle = if (exitosa) {
                        "Parcela creada."
                    } else {
                        ClienteRed.detalleDeError(it).ifEmpty { "No se pudo crear la parcela." }
                    }
                    principal.post { alTerminar(exitosa, detalle) }
                }
            }
        })
    }

    /** Consulta el listado de campañas del socio (llamada autenticada). */
    fun cargarCampanas(
        token: String,
        alTerminar: (List<DatosCampanaResumen>?, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mi-campanas")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val arreglo = JSONArray(it.body?.string() ?: "[]")
                        val lista = mutableListOf<DatosCampanaResumen>()
                        for (i in 0 until arreglo.length()) {
                            val c = arreglo.optJSONObject(i) ?: continue
                            lista.add(
                                DatosCampanaResumen(
                                    id = c.optString("id"),
                                    nombre = c.optString("nombre"),
                                    estado = c.optString("estado")
                                )
                            )
                        }
                        principal.post { alTerminar(lista, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudieron cargar tus campañas." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta una campaña del socio (llamada autenticada). */
    fun cargarCampana(
        token: String,
        campanaId: String,
        alTerminar: (DatosCampana?, String) -> Unit
    ) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mi-campanas/$campanaId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val cuerpo = JSONObject(it.body?.string() ?: "")
                        val campana = cuerpo.optJSONObject("campana") ?: JSONObject()
                        val oficiales = cuerpo.optJSONObject("datos_oficiales") ?: JSONObject()
                        val laboresJson = cuerpo.optJSONArray("labores") ?: JSONArray()
                        val labores = mutableListOf<DatosLabor>()
                        for (i in 0 until laboresJson.length()) {
                            val l = laboresJson.optJSONObject(i) ?: continue
                            labores.add(formatearLabor(l))
                        }
                        val datos = DatosCampana(
                            nombre = campana.optString("nombre"),
                            estado = campana.optString("estado"),
                            kilosEntregados = formatearNumero(
                                oficiales.optDouble("kilos_entregados"), 0
                            ) + " kg",
                            rendimientoGraso = formatearNumero(
                                oficiales.optDouble("rendimiento_graso"), 2
                            ) + " %",
                            aceiteObtenido = formatearNumero(
                                oficiales.optDouble("aceite_obtenido"), 0
                            ) + " kg",
                            labores = labores
                        )
                        principal.post { alTerminar(datos, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "No se pudo cargar la campaña." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }

    /** Consulta la cuenta del token en un endpoint protegido (llamada autenticada). */
    fun cargarMiCuenta(token: String, alTerminar: (String?, String) -> Unit) {
        val peticion = Request.Builder()
            .url("${ClienteRed.URL_BASE}/api/v1/mi-cuenta")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        ClienteRed.cliente.newCall(peticion).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                principal.post {
                    alTerminar(null, "No hay conexión con el servidor. Inténtalo de nuevo.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val email = JSONObject(it.body?.string() ?: "").optString("email")
                        principal.post { alTerminar(email, "") }
                    } else {
                        val detalle = ClienteRed.detalleDeError(it)
                            .ifEmpty { "Sesión caducada o no válida. Vuelve a iniciar sesión." }
                        principal.post { alTerminar(null, detalle) }
                    }
                }
            }
        })
    }
}
