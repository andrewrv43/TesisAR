package ups.tesis.detectoraltavelocidad

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import kotlin.math.sqrt
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMapClickListener, SensorEventListener
    /*,LocationListener*/ {

    private lateinit var map: GoogleMap
    companion object { const val REQUEST_CODE_LOCATION = 1000 }
    private var lastMarker: Marker? = null
    private var mapStyleFile: String = "map_style_standard_111.json"  // Estilo predeterminado
    private lateinit var mapStyleReceiver: BroadcastReceiver
    private lateinit var infoBtn: ImageView

    /* Variables para sensor de velocidad */
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var xValueText: TextView
    private lateinit var yValueText: TextView
    private lateinit var zValueText: TextView
    private lateinit var speedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        //val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        //val adapter = ViewPagerAdapter(this)
        //viewPager.adapter = adapter

        createMapFragment()
        createAcelerometerSensor()

        // Obtener referencia al ImageView
        infoBtn = findViewById(R.id.infoBtn)

        // Establecer el listener de click
        infoBtn.setOnClickListener {
            // Crear un Intent para lanzar actividad InfoActivity
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        // Registrar el BroadcastReceiver
        registerMapStyleReceiver()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMyLocationClickListener(this)
        map.setOnMapClickListener(this)

        // Aplicar el estilo del mapa
        applyMapStyle()

        enableLocation()
    }

    /**
     * Crear fragmento del mapa
     */
    private fun createMapFragment() {
        // Obtener el nombre del estilo desde SharedPreferences
        val sharedPreferences = getSharedPreferences("MapStyles", Context.MODE_PRIVATE)
        mapStyleFile = sharedPreferences.getString("STYLE_FILENAME", "map_style_standard_111.json")!!

        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mapStyleReceiver)
    }

    /**
     *  Verifica si existen permisos de localizacion aceptados
     */
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /**
     *  Activar los permisos de Localizacion
     */
    private fun enableLocation() {
        if(!::map.isInitialized) return
        if(isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    /**
     *  Pide los permisos de Localizacion al usuario
     */
    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Acepta los permisos en ajustes", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    this,
                    "Para aceptar los permisos debes hacerlo desde ajustes",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    /**
     *   Se muestra una notificacion en la localizacion del usuario
     */
    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this, "Localizacion: ${p0.latitude} , ${p0.longitude}", Toast.LENGTH_SHORT).show()
    }

    /**
     *   Se muestra una notificacion y una marca en la localizacion seleccionada
     */
    override fun onMapClick(p0: LatLng) {
        lastMarker?.remove()

        val marker = MarkerOptions()
            .position(p0)
            .title("Posicion seleccionada")
        lastMarker = map.addMarker(marker)

        Toast.makeText(this, "Localizacion: ${p0.latitude} , ${p0.longitude}", Toast.LENGTH_SHORT).show()
    }

    /**
     *  Calculo de la distancia entre dos puntos
     */
    /*override fun onLocationChanged(location: Location) {
        val speed = location.speed // Velocidad en m/s
        val speedKmh = speed * 3.6 // Convertir a km/h

        // Mostrar la velocidad en tu TextView
        speedText.text = "Velocidad: %.2f km/h".format(speedKmh)
    }*/

    /***********************************************************************************************
     *   Estilo del mapa
     **********************************************************************************************/
    /**
     *  Busca y aplica el estilo almacenado en "SharedPreferences"
     */

    private fun applyMapStyle() {
        // Obtener el nombre del estilo desde SharedPreferences
        val sharedPreferences = getSharedPreferences("MapStyles", Context.MODE_PRIVATE)
        mapStyleFile = sharedPreferences.getString("STYLE_FILENAME", "map_style_standard_111.json")!!

        try {
            // Obtener el ID del recurso del archivo de estilo
            val styleResId = resources.getIdentifier(
                mapStyleFile.substringBeforeLast(".json"),
                "raw",
                packageName
            )

            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, styleResId
                )
            )

            if (!success) {
                Log.e("MapsActivity", "Error al cargar el estilo.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "No se encontro el estilo.", e)
        }
    }

    /**
     *  Registra el BroadcastReceiver para recibir actualizaciones de estilo
     */

    private fun registerMapStyleReceiver() {
        mapStyleReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Actualizar el estilo del mapa
                applyMapStyle()
            }
        }
        val filter = IntentFilter("com.example.UPDATE_MAP_STYLE")
        LocalBroadcastManager.getInstance(this).registerReceiver(mapStyleReceiver, filter)
    }

    /**********************************************************************************************/


    /*********************************************************************************************
     *   Sensor de velocidad (ACELEROMETRO)
     ********************************************************************************************/
    /**
     * Crear sensor de acelerometro y inicializar los TextViews
     */
    private fun createAcelerometerSensor() {
        // Inicializar los TextViews para mostrar los valores
        xValueText = findViewById(R.id.xValue)
        yValueText = findViewById(R.id.yValue)
        zValueText = findViewById(R.id.zValue)
        speedText = findViewById(R.id.speedValue)

        // Inicializar el SensorManager y el acelerómetro
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        // Registrar el listener del sensor
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Aplicar el estilo del mapa al reanudar la actividad
        //if (::map.isInitialized) {
            //applyMapStyle()
        //}

        // Verificar si el estilo ha cambiado
        val sharedPreferences = getSharedPreferences("MapStyles", Context.MODE_PRIVATE)
        val newMapStyleFile = sharedPreferences.getString("STYLE_FILENAME", "map_style_standard_111.json")!!

        if (newMapStyleFile != mapStyleFile) {
            mapStyleFile = newMapStyleFile
            applyMapStyle()
        }

    }

    override fun onPause() {
        super.onPause()
        // Detener el sensor cuando la actividad no esté visible
        sensorManager.unregisterListener(this)
    }

    /**
     * Actualizar los valores del acelerómetro
     */
    // Variables para almacenar la velocidad en cada eje
    private var ax: Double = 0.0
    private var ay: Double = 0.0
    private var az: Double = 0.0

    // Tiempo inicial
    var lastTimestamp = 0L
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            ax = event.values[0].toDouble()
            ay = event.values[1].toDouble()
            az = event.values[2].toDouble()

            // Calcular la magnitud de la aceleración con valores (X,Z)
            val accelerationMagnitude = sqrt((ax * ax) + (az * az))

            // Calcular el tiempo transcurrido
            val currentTimestamp = System.currentTimeMillis()
            val dt = (currentTimestamp - lastTimestamp) / 1000.0
            lastTimestamp = currentTimestamp

            // Convertir a km/h
            val speedKmh = accelerationMagnitude * dt

            // Mostrar la velocidad
            speedText.text = "Speed: %.2f km/h".format(speedKmh)

            // Mostrar los valores del acelerómetro en los TextViews
            xValueText.text = "X: $ax"
            yValueText.text = "Y: $ay"
            zValueText.text = "Z: $az"
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // No es necesario implementar este método en este caso
    }
}