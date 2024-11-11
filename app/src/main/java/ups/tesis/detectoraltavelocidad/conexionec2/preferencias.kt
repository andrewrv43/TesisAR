package ups.tesis.detectoraltavelocidad.conexionec2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import ups.tesis.detectoraltavelocidad.R
import ups.tesis.detectoraltavelocidad.conexionec2.models.envRegistro
import ups.tesis.detectoraltavelocidad.conexionec2.models.getTok
import ups.tesis.detectoraltavelocidad.conexionec2.models.tokenRequest
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class Referencias(val context: Context){
    fun getSharedPreferences(): SharedPreferences {
        return  context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }
    fun getFromPreferences(name:String): String {
        val sharedPreferences = getSharedPreferences()
        val res = sharedPreferences.getString(name, null)
        if(res!=null) {
            return res
        }
        return ""
    }
    fun saveToPreferences(value: String,name:String) {
        val sharedPreferences = getSharedPreferences()
        val editor = sharedPreferences.edit()
        editor.putString(name, value)
        editor.apply()
    }
    fun initializeRetrofitService(token: String=""):RetrofitService {
        return RetrofitServiceFactory.makeRetrofitService(token)
    }

    fun alertBox(titulo:String, texto: Int, btnTxt:String ){
        val artDialogBuilder= AlertDialog.Builder(context)
        artDialogBuilder.setTitle(titulo)
        artDialogBuilder.setMessage(texto)
        artDialogBuilder.setCancelable(false)
        artDialogBuilder.setPositiveButton(btnTxt){_,_->

        }
        artDialogBuilder.create().show()
    }

    suspend fun saveInfoToSv(retrofitService: RetrofitService, newRegister: envRegistro,local:Boolean=false):Int{
        try {
            val response = retrofitService.newRecord(newRegister)
            if (response.isSuccessful){
                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                return 1
            }else{
                Toast.makeText(context, "Registro Fallido", Toast.LENGTH_SHORT).show()
                return 1
            }
        } catch (e: ConnectException) {
            if(local){
                Toast.makeText(context, "Datos locales no cargados", Toast.LENGTH_SHORT).show()
            }else{
                addNewLocalRegister(newRegister)
                Toast.makeText(context, "No existe conexion los datos seran guardados de manera local", Toast.LENGTH_SHORT).show()
            }
            Log.e("saveInfoToSv", "Error de conexión: ${e.message}")
            return 0
        } catch (e: SocketTimeoutException) {
            if(local){
                Toast.makeText(context, "Datos locales no cargados", Toast.LENGTH_SHORT).show()
            }else{
                addNewLocalRegister(newRegister)
                Toast.makeText(context, "No existe conexion los datos seran guardados de manera local", Toast.LENGTH_SHORT).show()
            }
            Log.e("saveInfoToSv", "Tiempo de espera agotado: ${e.message}")
            return 0
        } catch (e: IOException) {
            if(local){
                Toast.makeText(context, "Datos locales no cargados", Toast.LENGTH_SHORT).show()
            }else{
                addNewLocalRegister(newRegister)
                Toast.makeText(context, "No existe conexion los datos seran guardados de manera local", Toast.LENGTH_SHORT).show()
            }
            Log.e("saveInfoToSv", "Error de entrada/salida: ${e.message}")
            return 0
        } catch (e: Exception) {
            if(local){
                Toast.makeText(context, "Datos locales no cargados", Toast.LENGTH_SHORT).show()

            }else{
                addNewLocalRegister(newRegister)
                Toast.makeText(context, "No existe conexion los datos seran guardados de manera local", Toast.LENGTH_SHORT).show()
            }
            Log.e("saveInfoToSv", "Error inesperado: ${e.message}")
            return 0
        }

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

    fun addNewLocalRegister(newRegister: envRegistro){
        val sharedPreferences = context.getSharedPreferences("localRegs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val jsonArrayString = sharedPreferences.getString("envRegistro", null)
        val registrosList: MutableList<envRegistro> = if (jsonArrayString != null) {
            val type = object : TypeToken<MutableList<envRegistro>>() {}.type
            gson.fromJson(jsonArrayString, type)
        } else {
            mutableListOf()
        }
        registrosList.add(newRegister)
        val newJsonArrayString = gson.toJson(registrosList)
        editor.putString("envRegistro", newJsonArrayString)
        editor.apply()
    }
    fun obtainLocalRegs():List<envRegistro>{
        val sharedPreferences = context.getSharedPreferences("localRegs", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonArrayString = sharedPreferences.getString("envRegistro", null)

        return if (jsonArrayString != null) {
            val type = object : TypeToken<List<envRegistro>>() {}.type
            gson.fromJson(jsonArrayString, type)
        } else {
            emptyList()
        }
    }
    suspend fun loadLocalRegsSv(){
        val localRegs = obtainLocalRegs().toMutableList()

        if (localRegs.isNotEmpty()) {
            val iterator = localRegs.iterator()
            while (iterator.hasNext()) {
                val reg = iterator.next()

                val result = saveInfoToSv(
                    retrofitService = initializeRetrofitService(getFromPreferences("auth_token")),
                    newRegister = reg,
                    local = true
                )
                if (result == 1) {
                    Log.e("loadLocalRegsSv", "Se removio un dato guardado")
                    iterator.remove()
                } else{
                    Log.e("loadLocalRegsSv", "Error inesperado: No se cargaron todos los datos locales")
                    break
                }

            }
            saveEnvRegistros(localRegs)
        }

    }
    fun saveEnvRegistros( registros: List<envRegistro>) {
        val sharedPreferences = context.getSharedPreferences("localRegs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val jsonArrayString = gson.toJson(registros)
        editor.putString("envRegistro", jsonArrayString)
        editor.apply()
    }

}