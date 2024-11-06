package ups.tesis.detectoraltavelocidad.conexionec2

import android.content.Context
import android.content.SharedPreferences

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
}