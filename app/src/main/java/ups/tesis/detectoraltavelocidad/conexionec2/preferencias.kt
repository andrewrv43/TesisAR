package ups.tesis.detectoraltavelocidad.conexionec2

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import retrofit2.Response
import ups.tesis.detectoraltavelocidad.R
import ups.tesis.detectoraltavelocidad.conexionec2.models.envRegistro
import ups.tesis.detectoraltavelocidad.conexionec2.models.getTok
import ups.tesis.detectoraltavelocidad.conexionec2.models.obtRegsId
import ups.tesis.detectoraltavelocidad.conexionec2.models.tokenRequest
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class Referencias(val context: Context){
    /**
     * Obtiene las preferencias compartidas.
     */
    fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    /**
     * Obtiene un valor de las preferencias compartidas por nombre.
     */
    fun getFromPreferences(name: String): String {
        val sharedPreferences = getSharedPreferences()
        val res = sharedPreferences.getString(name, null)
        return res ?: ""
    }

    /**
     * Guarda un valor en las preferencias compartidas con el nombre especificado.
     */
    fun saveToPreferences(value: String, name: String) {
        val sharedPreferences = getSharedPreferences()
        val editor = sharedPreferences.edit()
        editor.putString(name, value)
        editor.apply()
    }

    /**
     * Elimina una preferencia específica según su nombre.
     */
    fun removeFromPreferences(name: String) {
        val sharedPreferences = getSharedPreferences()
        val editor = sharedPreferences.edit()
        editor.remove(name)
        editor.apply()
    }

    fun initializeRetrofitService(token: String=""):RetrofitService {
        return RetrofitServiceFactory.makeRetrofitService(token)
    }

    fun alertBox(titulo: String, texto: Int, btnTxt: String, onPositiveClick: (() -> Unit)? = null) {
        val artDialogBuilder = AlertDialog.Builder(context)
        artDialogBuilder.setTitle(titulo)
        artDialogBuilder.setMessage(texto)
        artDialogBuilder.setCancelable(false)
        artDialogBuilder.setPositiveButton(btnTxt) { dialog, _ ->
            if (onPositiveClick != null) {
                onPositiveClick()
            } else {
                dialog.dismiss()
            }
        }
        artDialogBuilder.setNegativeButton("CANCELAR") { dialog, _ ->
            dialog.dismiss()
        }
        artDialogBuilder.create().show()
    }


    suspend fun getLifeTimeToken(retrofitService:RetrofitService): Int {
        return try {
            val response = retrofitService.getTimeLeft()
            return if (response.isSuccessful) {
                val responseBody = response.body()
                responseBody?.let {
                    if (it.time_left.toInt() == 0){
                        val (usrInfo,_,_ ) = makeGetTokenRequest(tokenRequest(username = getFromPreferences("username"), password = getFromPreferences("password")), retrofitService, mutableMapOf())
                        saveToPreferences(usrInfo["token"].toString(), "auth_token")
                        getLifeTimeToken(retrofitService)
                    }else{
                        it.time_left.toInt()
                    }
                } ?: 0
            } else {
                0
            }

        } catch (e: ConnectException) {
            alertBox(titulo = "ALERTA", texto = R.string.txtNoInternetConnection, btnTxt = "Continuar")
            Log.e("getLifeTimeToken", "Error de conexión: ${e.message}")
            0
        } catch (e: SocketTimeoutException) {
            Log.e("getLifeTimeToken", "Tiempo de espera agotado: ${e.message}")
            0
        } catch (e: IOException) {
            Log.e("getLifeTimeToken", "Error de entrada/salida: ${e.message}")
            0
        } catch (e: Exception) {
            Log.e("getLifeTimeToken", "Error inesperado: ${e.message}")
            0
        }
    }

    suspend fun makeGetTokenRequest(
        request: tokenRequest,
        retrofitService: RetrofitService,
        usrInfo: MutableMap<String, Any>
    ): Triple<MutableMap<String, Any>, Boolean, RetrofitService> {
        return try {
            val response: Response<getTok> = retrofitService.getTok(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                responseBody?.let {
                    usrInfo["token"] = it.token
                    saveToPreferences(it.token, "auth_token")
                    saveToPreferences(request.username, "username")
                    saveToPreferences(request.password, "password")

                    val updatedRetrofitService = initializeRetrofitService(it.token)
                    Triple(usrInfo, true, updatedRetrofitService)
                } ?: run {
                    Log.e("makeGetTokenRequest", "El cuerpo de la respuesta es nulo")
                    Triple(usrInfo, false, retrofitService)
                }
            } else {
                Triple(usrInfo, false, retrofitService)
            }
        } catch (e: ConnectException) {
            alertBox(titulo = "ALERTA", texto = R.string.txtNoInternetConnection, btnTxt = "Continuar")
            Log.e("makeGetTokenRequest", "Error de conexión: ${e.message}")
            Triple(usrInfo, false, retrofitService)
        } catch (e: SocketTimeoutException) {
            Log.e("makeGetTokenRequest", "Tiempo de espera agotado: ${e.message}")
            Triple(usrInfo, false, retrofitService)
        } catch (e: IOException) {
            Log.e("makeGetTokenRequest", "Error de entrada/salida: ${e.message}")
            Triple(usrInfo, false, retrofitService)
        } catch (e: Exception) {
            Log.e("makeGetTokenRequest", "Error inesperado: ${e.message}")
            Triple(usrInfo, false, retrofitService)
        }
    }



    suspend fun get_speed_data_per_user(
        retrofitService: RetrofitService,
        limit: Int
    ): obtRegsId? {
        return try {
            val response = retrofitService.getSpdRecordUser(limit)
            if (response.isSuccessful) {
                val responseBody = response.body()
                responseBody?.let {
                    Log.e("get_speed_data_per_user", "Registros obtenidos: ${it.records.size}, Total: ${it.total_length}")

                    obtRegsId(
                        records = it.records,
                        total_length = it.total_length
                    )
                }
            } else {
                Log.e("get_speed_data_per_user", "Error en la respuesta: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: ConnectException) {
            Log.e("get_speed_data_per_user", "Error de conexión: ${e.message} al obtener registros")
            null
        } catch (e: SocketTimeoutException) {
            Log.e("get_speed_data_per_user", "Tiempo de espera agotado: ${e.message} al obtener registros")
            null
        } catch (e: IOException) {
            Log.e("get_speed_data_per_user", "Error de entrada/salida: ${e.message} al obtener registros")
            return get_speed_data_per_user(retrofitService,limit)
        } catch (e: Exception) {
            Log.e("get_speed_data_per_user", "Error inesperado: ${e.message} al obtener registros")
            null
        }
    }
    fun hayConexionAInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    suspend fun actualizacion(retrofitService: RetrofitService) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Verificando si existe una nueva versión...", Toast.LENGTH_LONG).show()
            }
            val clientVersion = "0.4"  // Versión de la aplicación
            val response = retrofitService.downloadApk(clientVersion)
            when (response.code()) {
                200 -> {
                    val body = response.body()
                    if (body != null) {
                        withContext(Dispatchers.Main) {
                            alertBox("Actualización Disponible", R.string.Actualizacion, "ACTUALIZAR") {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val apkFile = File(context.getExternalFilesDir(null), "app_v${clientVersion}.apk")

                                        val inputStream: InputStream = body.byteStream()
                                        val outputStream = FileOutputStream(apkFile)

                                        inputStream.use { input ->
                                            outputStream.use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            checkInstallPermissionAndInstall(apkFile)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Log.e("Actualizacion", "Error al descargar la actualización", e)
                                    }
                                }
                            }
                        }
                    }
                }
                204 -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "La aplicación está actualizada.", Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    Log.e("Actualizacion", "Error en la respuesta: ${response.code()} - ${response.message()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Actualizacion", "Error al verificar actualizaciones", e)
        }
    }

    fun installApk(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }

    fun checkInstallPermissionAndInstall(apkFile: File) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (canInstall) {
                    // Tiene permiso para instalar
                    installApk(apkFile)
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                    Toast.makeText(context, "Por favor, permite la instalación de aplicaciones desconocidas", Toast.LENGTH_LONG).show()
                }
            } else {
                installApk(apkFile)
            }
    }

}

class CargaDatos(){
    suspend fun saveInfoToSv(context: DataStore<Preferences>,retrofitService: RetrofitService, newRegister: envRegistro, local: Boolean = false): Int {
        try {
            if(newRegister.streetMaxSpeed.toDouble()==0.00){
                Log.e("saveInfoToSv", "Dato no valido velocidad maxima = 0")
                return 0
            }
            val response = retrofitService.newRecord(newRegister)
            if (response.isSuccessful) {
                Log.i("saveInfoToSv", "Registro exitoso")
                return 1
            } else {
                Log.i("saveInfoToSv", "Registro fallido")
                return 1
            }
        } catch (e: ConnectException) {
            if (local) {
                Log.e("saveInfoToSv", "Datos locales no cargados debido a un error de conexión: ${e.message}")
            } else {
                addNewLocalRegister(context,newRegister)
                Log.e("saveInfoToSv", "No existe conexión, los datos serán guardados de manera local: ${e.message}")
            }
            return 0
        } catch (e: SocketTimeoutException) {
            if (local) {
                Log.e("saveInfoToSv", "Datos locales no cargados debido a un tiempo de espera agotado: ${e.message}")
            } else {
                addNewLocalRegister(context,newRegister)
                Log.e("saveInfoToSv", "No existe conexión, los datos serán guardados de manera local: ${e.message}")
            }
            return 0
        } catch (e: IOException) {
            if (local) {
                Log.e("saveInfoToSv", "Datos locales no cargados debido a un error de entrada/salida: ${e.message}")
            }
            return 0
        } catch (e: Exception) {
            if (local) {
                Log.e("saveInfoToSv", "Datos locales no cargados debido a un error inesperado: ${e.message}")
            } else {
                addNewLocalRegister(context,newRegister)
                Log.e("saveInfoToSv", "Exepcion Existente: ${e}")
            }
            return 0
        }
    }

    suspend fun addNewLocalRegister(dataStore: DataStore<Preferences>, newRegister: envRegistro) {
        val gson = Gson()

        dataStore.edit { preferences ->
            // Leer el JSON existente
            val jsonArrayString = preferences[stringPreferencesKey("envRegistro")]
            val registrosList: MutableList<envRegistro> = if (jsonArrayString != null) {
                val type = object : TypeToken<MutableList<envRegistro>>() {}.type
                gson.fromJson(jsonArrayString, type)
            } else {
                mutableListOf()
            }

            // Agregar el nuevo registro
            registrosList.add(newRegister)

            // Convertir de nuevo a JSON y guardarlo
            val newJsonArrayString = gson.toJson(registrosList)
            preferences[stringPreferencesKey("envRegistro")] = newJsonArrayString
        }
    }
    val envRegistroKey = stringPreferencesKey("envRegistro")

    // Obtener registros locales desde DataStore
    suspend fun obtainLocalRegs(dataStore: DataStore<Preferences>): List<envRegistro> {
        val gson = Gson()

        val jsonArrayString = dataStore.data.map { preferences ->
            preferences[envRegistroKey] ?: "[]" // Devuelve un array vacío si no hay datos
        }.first() // Toma el primer valor emitido por el flujo

        return if (jsonArrayString.isNotEmpty()) {
            val type = object : TypeToken<List<envRegistro>>() {}.type
            gson.fromJson(jsonArrayString, type)
        } else {
            emptyList()
        }
    }

    // Guardar registros locales en DataStore
    suspend fun saveEnvRegistros(dataStore: DataStore<Preferences>, registros: List<envRegistro>) {
        val gson = Gson()
        val jsonArrayString = gson.toJson(registros)

        dataStore.edit { preferences ->
            preferences[envRegistroKey] = jsonArrayString
        }
    }

    // Guardar múltiples registros en el servidor
    suspend fun saveInfoBatchToSv(
        retrofitService: RetrofitService,
        registerList: List<envRegistro>
    ): Int {
        return try {
            var total = 0
            val response = retrofitService.saveBatch(registerList)
            if (response.isSuccessful) {
                val responseBody = response.body()
                responseBody?.let {
                    if (it.count.toInt() != 0) {
                        total = it.count.toInt()
                        Log.e("saveInfoBatchToSv", "$responseBody ")
                    }
                }
                total
            } else {
                0
            }
        } catch (e: ConnectException) {
            Log.e("saveInfoBatchToSv", "Error de conexión: ${e.message} Al cargar todos los datos locales")
            0
        } catch (e: SocketTimeoutException) {
            Log.e("saveInfoBatchToSv", "Tiempo de espera agotado: ${e.message} Al cargar todos los datos locales")
            0
        } catch (e: IOException) {
            Log.e("saveInfoBatchToSv", "Error de entrada/salida: ${e.message} Al cargar todos los datos locales")
            0
        } catch (e: Exception) {
            Log.e("saveInfoBatchToSv", "Error inesperado: ${e.message} Al cargar todos los datos locales")
            0
        }
    }

    // Procesar registros locales y enviarlos al servidor
    suspend fun loadLocalRegsSv(dataStore: DataStore<Preferences>, retrofitService: RetrofitService) {
        val localRegs = obtainLocalRegs(dataStore).toMutableList()

        if (localRegs.isNotEmpty()) {
            val result = saveInfoBatchToSv(
                retrofitService = retrofitService,
                registerList = localRegs
            )
            if (result != 0) {
                Log.e(
                    "loadLocalRegsSv",
                    "Se enviaron todos los datos locales correctamente y se eliminarán. Se enviaron un total de $result registros."
                )
                // Si la operación fue exitosa, limpiar los registros locales
                saveEnvRegistros(dataStore, emptyList())
            } else {
                Log.e("loadLocalRegsSv", "Error inesperado: No se enviaron todos los datos locales.")
            }
        }
    }


}