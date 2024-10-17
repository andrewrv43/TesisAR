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
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

import kotlin.math.sqrt
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, /*GoogleMap.OnMapClickListener,*/ SensorEventListener
    /*,LocationListener*/ {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var infoBtn: ImageView
    companion object { const val REQUEST_CODE_LOCATION = 1000 }
    private var lastMarker: Marker? = null
    private var mapStyleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Recargar el estilo del mapa cuando se recibe el broadcast
            applyMapStyle()
        }
    }
    private val FILE_NAME = "geocoder_cache.txt"

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
        registerBroadcastReceiver()
        createAcelerometerSensor()

        // Obtener referencia al ImageView
        infoBtn = findViewById(R.id.infoBtn)

        // Establecer el listener de click
        infoBtn.setOnClickListener {
            // Crear un Intent para lanzar actividad InfoActivity
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMyLocationClickListener(this)
        /*map.setOnMapClickListener(this)*/

        // Aplicar el estilo del mapa
        applyMapStyle()

        enableLocation()
    }

    override fun onResume() {
        super.onResume()
        // Registrar el listener del sensor
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        // Detener el sensor cuando la actividad no esté visible
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destruir el BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mapStyleReceiver)
    }

    /**
     * Crear fragmento del mapa
     */
    private fun createMapFragment() {
        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura la solicitud de ubicación
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 segundos
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Define el callback de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    getStreetName(currentLatLng)
                }
            }
        }
    }

    /**
     *  Registra el BroadcastReceiver para recibir actualizaciones de estilo
     */
    private fun registerBroadcastReceiver() {
        val filter = IntentFilter("com.example.UPDATE_MAP_STYLE")
        LocalBroadcastManager.getInstance(this).registerReceiver(mapStyleReceiver, filter)
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
    /*override fun onMapClick(p0: LatLng) {
        lastMarker?.remove()

        val marker = MarkerOptions()
            .position(p0)
            .title("Posicion seleccionada")
        lastMarker = map.addMarker(marker)

        Toast.makeText(this, "Localizacion: ${p0.latitude} , ${p0.longitude}", Toast.LENGTH_SHORT).show()
    }*/

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
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val roadSelection = sharedPref.getInt("road_selection", 3)
        val landmarkSelection = sharedPref.getInt("landmark_selection", 3)
        val labelSelection = sharedPref.getInt("label_selection", 3)
        val darkMode = sharedPref.getBoolean("dark_mode", false)

        val mapStyleFile = if (darkMode) {
            "map_style_dark_${roadSelection}${landmarkSelection}${labelSelection}.json"
        } else {
            "map_style_standard_${roadSelection}${landmarkSelection}${labelSelection}.json"
        }


        try {
            // Cargar el estilo desde los recursos (res/raw)
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, resources.getIdentifier(
                        mapStyleFile.substringBeforeLast('.'),
                        "raw",
                        packageName
                    )
                )
            )
            println("SE CARGO EL ESTILO: $mapStyleFile ---------------------------------------------")

            if (!success) {
                Log.e("MapsActivity", "Error al cargar el estilo.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "No se encontro el estilo ${mapStyleFile}.", e)
        }
    }





    /***********************************************************************************************
     *      ACTUALIZACION DE LOCALIZACION
     **********************************************************************************************/
    /**
     *  Inicia la actualizacion de la localizacion
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun updateMapLocation(latLng: LatLng, streetName: String) {
        if (::map.isInitialized) {
            lastMarker?.remove()
            lastMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Estás en: $streetName")
            )
            //map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun getStreetName(latLng: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
            try {
                val addresses: List<Address>? = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    5
                )

                if (!addresses.isNullOrEmpty()) {
                    val address: Address = addresses[0]
                    val streetName = address.thoroughfare ?: "Calle desconocida"

                    // Guardar en archivo
                    saveStreetNameToFile(latLng, address)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MapsActivity, "Estás en: $streetName", Toast.LENGTH_SHORT).show()
                        updateMapLocation(latLng, streetName)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MapsActivity, "No se encontró la calle.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Error al usar Geocoder: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun saveStreetNameToFile(latLng: LatLng, streetName: Address) {
        try {
            val fileOutputStream: FileOutputStream = openFileOutput(FILE_NAME, MODE_APPEND)
            /*val json = """
            {
                "latitude": ${latLng.latitude},
                "longitude": ${latLng.longitude},
                "streetName": "${streetName.replace("\"", "\\\"")}"
            }
        """.trimIndent()*/
            val json = """
            {
                "latitude": ${latLng.latitude},
                "longitude": ${latLng.longitude},
                "streetName": "$streetName"
            }
        """.trimIndent()
            fileOutputStream.write("$json\n".toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    /*private fun readStreetNameFromFile(latLng: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileInputStream: FileInputStream = openFileInput(FILE_NAME)
                val inputStreamReader = InputStreamReader(fileInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val lines = bufferedReader.readLines()
                bufferedReader.close()

                var nearestStreet: String? = null
                var minDistance = Double.MAX_VALUE

                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val savedLat = parts[0].toDoubleOrNull()
                        val savedLon = parts[1].toDoubleOrNull()
                        val savedStreet = parts[2]

                        if (savedLat != null && savedLon != null) {
                            val savedLatLng = LatLng(savedLat, savedLon)
                            val distance = distanceBetween(latLng, savedLatLng)
                            if (distance < minDistance) {
                                minDistance = distance
                                nearestStreet = savedStreet
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (nearestStreet != null && minDistance <= 50) { // Umbral de 50 metros
                        Toast.makeText(this@MainActivity, "Estás en (offline): $nearestStreet", Toast.LENGTH_SHORT).show()
                        updateMapLocation(latLng, nearestStreet)
                    } else {
                        Toast.makeText(this@MainActivity, "No se encontró la calle en el cache.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "No hay datos en el cache.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error al leer el cache: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun distanceBetween(p1: LatLng, p2: LatLng): Double {
        val earthRadius = 6371000.0 // en metros
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }*/









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

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /* No es necesario implementar este método */ }
}