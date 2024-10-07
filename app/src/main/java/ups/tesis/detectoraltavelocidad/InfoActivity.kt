package ups.tesis.detectoraltavelocidad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButtonToggleGroup

class InfoActivity : AppCompatActivity() {

    private var roadSelection = 3  // Valor predeterminado 'Alto'
    private var landmarkSelection = 3  // Valor predeterminado 'Alto'
    private var labelSelection = 3  // Valor predeterminado 'Alto'

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        // Configurar y actualizar los listeners para los grupos de botones
        setupToggleGroupListeners()

        // Configurar el Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Habilitar el botón de retroceso en el Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupToggleGroupListeners() {
        // Grupo de Botones "Caminos"
        val roadToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.roadToggleGroup)
        roadToggleGroup.check(R.id.roadBtn3)
        roadToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                roadSelection = when (checkedId) {
                    R.id.roadBtn1 -> 1  // Bajo
                    R.id.roadBtn2 -> 2  // Medio
                    R.id.roadBtn3 -> 3  // Alto
                    else -> 3
                }
                updateMapStyle() // Actualizar despues de cambiar
            }
        }

        // Grupo de Botones "Puntos de Referencia"
        val landmarksToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.landmarksToggleGroup)
        landmarksToggleGroup.check(R.id.landmarksBtn3)
        landmarksToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                landmarkSelection = when (checkedId) {
                    R.id.landmarksBtn1 -> 1  // Bajo
                    R.id.landmarksBtn2 -> 2  // Medio
                    R.id.landmarksBtn3 -> 3  // Alto
                    else -> 3
                }
                updateMapStyle() // Actualizar despues de cambiar
            }
        }

        // Grupo de Botones "Etiquetas"
        val labelsToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.labelsToggleGroup)
        labelsToggleGroup.check(R.id.labelsBtn3)
        labelsToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                labelSelection = when (checkedId) {
                    R.id.labelsBtn1 -> 1  // Bajo
                    R.id.labelsBtn2 -> 2  // Medio
                    R.id.labelsBtn3 -> 3  // Alto
                    else -> 3
                }
                updateMapStyle() // Actualizar despues de cambiar
            }
        }
    }

    private fun updateMapStyle() {
        // Construir el nombre del archivo basado en las selecciones
        val styleFilename = "map_style_standard_${roadSelection}${landmarkSelection}${labelSelection}.json"

        // Guardar el nombre del estilo en SharedPreferences
        val sharedPreferences = getSharedPreferences("MapStyles", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("STYLE_FILENAME", styleFilename)
        editor.apply()

        // Enviar un broadcast para notificar a MapsActivity
        val intent = Intent("com.example.UPDATE_MAP_STYLE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        //sendBroadcast(intent)
    }

    // Manejar el clic en el botón de retroceso del Toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish() // Cierra la actividad y regresa a MapsActivity
        return true
    }
}