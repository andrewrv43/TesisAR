package ups.tesis.detectoraltavelocidad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButtonToggleGroup

class InfoActivity : AppCompatActivity() {

    private var roadSelection: Int = 3  // Valor predeterminado 'Alto'
    private var landmarkSelection: Int = 3  // Valor predeterminado 'Alto'
    private var labelSelection: Int = 3  // Valor predeterminado 'Alto'

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

    /**
     * Configurar y actualizar los listeners para los grupos de botones
     */
    private fun setupToggleGroupListeners() {
        // Obtener los valores de selección guardados
        val (savedRoadSelection, savedLandmarkSelection, savedLabelSelection) = loadConfig(this)

        // Grupo de Botones "Caminos"
        val roadToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.roadToggleGroup)
        when (savedRoadSelection) {
            1 -> roadToggleGroup.check(R.id.roadBtn1)
            2 -> roadToggleGroup.check(R.id.roadBtn2)
            3 -> roadToggleGroup.check(R.id.roadBtn3)
        }
        this.roadSelection = savedRoadSelection
        roadToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                this.roadSelection = when (checkedId) {
                    R.id.roadBtn1 -> 1  // Bajo
                    R.id.roadBtn2 -> 2  // Medio
                    R.id.roadBtn3 -> 3  // Alto
                    else -> 3
                }
                saveConfig() // Actualizar despues de cambiar
            }
        }

        // Grupo de Botones "Puntos de Referencia"
        val landmarksToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.landmarksToggleGroup)
        when (savedLandmarkSelection) {
            1 -> landmarksToggleGroup.check(R.id.landmarksBtn1)
            2 -> landmarksToggleGroup.check(R.id.landmarksBtn2)
            3 -> landmarksToggleGroup.check(R.id.landmarksBtn3)
        }
        this.landmarkSelection = savedLandmarkSelection
        landmarksToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                this.landmarkSelection = when (checkedId) {
                    R.id.landmarksBtn1 -> 1  // Bajo
                    R.id.landmarksBtn2 -> 2  // Medio
                    R.id.landmarksBtn3 -> 3  // Alto
                    else -> 3
                }
                saveConfig() // Actualizar despues de cambiar
            }
        }

        // Grupo de Botones "Etiquetas"
        val labelsToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.labelsToggleGroup)
        when (savedLabelSelection) {
            1 -> labelsToggleGroup.check(R.id.labelsBtn1)
            2 -> labelsToggleGroup.check(R.id.labelsBtn2)
            3 -> labelsToggleGroup.check(R.id.labelsBtn3)
        }
        this.labelSelection = savedLabelSelection
        labelsToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                labelSelection = when (checkedId) {
                    R.id.labelsBtn1 -> 1  // Bajo
                    R.id.labelsBtn2 -> 2  // Medio
                    R.id.labelsBtn3 -> 3  // Alto
                    else -> 3
                }
                saveConfig() // Actualizar despues de cambiar
            }
        }
    }

    /**
     *  Manejar el clic en el botón de retroceso del Toolbar
     */
    override fun onSupportNavigateUp(): Boolean {
        finish() // Cierra la actividad y regresa a MapsActivity
        return true
    }

    /**
     * Guardar la configuración seleccionada en
     * SharedPreferences
     */
    private fun saveConfig() {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putInt("road_selection", roadSelection)
            putInt("landmark_selection", landmarkSelection)
            putInt("label_selection", labelSelection)
            apply()
        }

        // Enviar un broadcast para notificar a MapsActivity
        val intent = Intent("com.example.UPDATE_MAP_STYLE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    /**
     * Cargar la configuración seleccionada de
     * SharedPreferences
     * @return Triple<road, landmark, label>
     */
    private fun loadConfig(context: Context): Triple<Int, Int, Int> {
        val sharedPref = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        return Triple(sharedPref.getInt("road_selection", 3),
                    sharedPref.getInt("landmark_selection", 3),
                    sharedPref.getInt("label_selection", 3))
    }
}