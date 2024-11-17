package ups.tesis.detectoraltavelocidad

import LocalDataAdapter
import RegistroAdapter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import org.json.JSONObject
import ups.tesis.detectoraltavelocidad.conexionec2.models.showRegs
import ups.tesis.detectoraltavelocidad.conexionec2.Referencias
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitService


class ProfileActivity:AppCompatActivity() {
    private lateinit var ref: Referencias
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewLocal: RecyclerView
    private lateinit var localView: RecyclerView
    private lateinit var registroAdapter: RegistroAdapter
    private lateinit var registroAdapterLocal: LocalDataAdapter
    private val registros = mutableListOf<showRegs>()
    private val registrosLocal = mutableListOf<showRegs>()
    private lateinit var username: MaterialTextView
    private lateinit var id: MaterialTextView
    private lateinit var retrofitService: RetrofitService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        ref = Referencias(this)
        recyclerView = findViewById(R.id.onlineData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        registroAdapter = RegistroAdapter(registros)
        recyclerView.adapter = registroAdapter

        localView = findViewById(R.id.localData)
        localView.layoutManager = LinearLayoutManager(this)
        registroAdapterLocal = LocalDataAdapter(registrosLocal)
        localView.adapter = registroAdapterLocal

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        username = findViewById(R.id.user_name)
        username.text = "Nombre de Usuario: ${ref.getFromPreferences("username")}"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        retrofitService = ref.initializeRetrofitService(ref.getFromPreferences("auth_token"))

        if (ref.hayConexionAInternet(this)){
            try {
                lifecycleScope.launch {
                    cargarRegistrosOnline()
                }
            }catch (ex:Exception){
                Log.e("On Create","Error al cargar los registros ${ex.message}")
            }
        }
        else{
            cargarRegistrosLocal()
        }




    }
    override fun onSupportNavigateUp(): Boolean {
        finish() // Cierra la actividad y regresa a MapsActivity
        return true
    }
    private suspend fun cargarRegistrosOnline() {
        val listregistros = ref.get_speed_data_per_user(retrofitService,20)
        if (listregistros != null){
            for (registro in listregistros){
                registros.add(registro)
            }
            registroAdapter.notifyDataSetChanged()
        }else{
            Log.e("On Create","Error al cargar los registros")
        }
    }
    private fun cargarRegistrosLocal() {
        val listregistros = ref.obtainLocalRegs()
        if (listregistros.isNotEmpty()) {
            for (reg in listregistros) {
                var dir = "Aun ubicandote"

                try {
                    val direccion = reg.direccion

                    direccion?.let {
                        val propertiesString = it.optString("properties")

                        if (!propertiesString.isNullOrEmpty()) {
                            // Limpiar el texto para convertirlo a JSON válido
                            val cleanedProperties = propertiesString
                                .replace("=", ":")
                                .replace("nameValuePairs=", "")
                                .replace("@id", "\"id\"")
                                .replace("access", "\"access\"")
                                .replace("alt_name", "\"alt_name\"")
                                .replace("highway", "\"highway\"")
                                .replace("maxspeed", "\"maxspeed\"")
                                .replace("name", "\"name\"")
                                .replace("oneway", "\"oneway\"")
                                .replace("source", "\"source\"")

                            // Envolver en un objeto JSON válido
                            val jsonText = "{\"nameValuePairs\":$cleanedProperties}"

                            // Convertir a JSONObject
                            val propertiesJson = JSONObject(jsonText)
                            val nameValuePairs = propertiesJson.optJSONObject("nameValuePairs")

                            // Obtener el valor de "name"
                            dir = nameValuePairs?.optString("name") ?: "Nombre no disponible"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("cargarRegistrosLocal","${e.message}")
                    dir = "Error al obtener nombre"
                }

                registrosLocal.add(
                    showRegs(
                        dir,
                        reg.fecha,
                        reg.speed.toString(),
                        reg.streetMaxSpeed.toString(),
                        "Pendiente de carga"
                    )
                )
            }
            registroAdapterLocal.notifyDataSetChanged()
        }
    }

}