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
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import ups.tesis.detectoraltavelocidad.conexionec2.Referencias
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitService
import ups.tesis.detectoraltavelocidad.conexionec2.models.envRegistro
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.os.Handler
import android.os.Looper
import android.widget.Button

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, /*GoogleMap.OnMapClickListener,*/ SensorEventListener
    /*,LocationListener*/ {
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 60000//5 minutos en milisegundos
    private lateinit var map: GoogleMap
    private lateinit var infoBtn: ImageView
    private lateinit var infoBtn2: Button
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private var direccion: JSONObject? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    companion object { const val REQUEST_CODE_LOCATION = 1000 }

    private var mapStyleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Recargar el estilo del mapa cuando se recibe el broadcast
            applyMapStyle()
        }
    }

    /* Variables para sensor de velocidad */
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var xValueText: TextView
    private lateinit var yValueText: TextView
    private lateinit var zValueText: TextView
    private lateinit var speedText: TextView
    val ref = Referencias(context = this)
    lateinit var retrofitService: RetrofitService

    /**
     * Funcion que se ejecuta cuando se inicia la actividad MapsActivity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        createMapFragment()
        registerBroadcastReceiver()
        createAcelerometerSensor()

        loadGeoJson() // Carga de mapa de Quito JSON

        glowContainer = findViewById(R.id.glowContainer)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        glowContainer.startAnimation(pulseAnimation)
        retrofitService=ref.initializeRetrofitService(ref.getFromPreferences("auth_token"))
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
        handler.postDelayed(runnable, interval)
    }

    override fun onPause() {
        super.onPause()
        // Detener el sensor cuando la actividad no esté visible
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
        handler.removeCallbacks(runnable)
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

        // Obtener referencia a los botones de la interfaz
        infoBtn = findViewById(R.id.infoBtn)
        infoBtn2 = findViewById(R.id.infoBtn2)

        // Establecer el listener de click para lanzar InfoActivity
        infoBtn.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }
        infoBtn2.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura la solicitud de ubicación
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
            .setMinUpdateIntervalMillis(500)
            .build()

        var lastSendDataTime = 0L
        // Define el callback de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    latitud = location.latitude
                    longitud = location.longitude
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    getCurrentLocation(currentLatLng)
                    getSpeed(location)
                    val currentTime = System.currentTimeMillis()
                    if (speed > maxSpeed) {
                        if (currentTime - lastSendDataTime >= 3000) {
                            lastSendDataTime = currentTime
                            lifecycleScope.launch {
                                sendData()
                            }
                        }
                    }
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

    private var lastMarker: Marker? = null

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

    private fun logPrintSpeed(speed: Double, maxSpeed: Double) {
        Log.e("logPrintSpeed", "Velocidad actual: $speed km/h - Velocidad maxima: $maxSpeed km/h")
    }
    /**
     * ---------------------------------------------------------------------------------------------------------------------------------------------
     * FEATURE IMPLEMENTACION DE DETECCION DE UBICACION POR MAPA LOCAL
     * ---------------------------------------------------------------------------------------------------------------------------------------------
     */
    private lateinit var geoJsonData: JSONArray
    // Cargar mapa de calles de Quito
    private fun loadGeoJson() {
        try {
            val inputStream = assets.open("quito01.geojson")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val geoJsonString = bufferedReader.use { it.readText() }
            val geoJsonObject = JSONObject(geoJsonString)
            geoJsonData = geoJsonObject.getJSONArray("features")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Encontrar la calle mas cercana por ubicacion
    private fun findNearestRoadSegment(latLng: LatLng): JSONObject? {
        var nearestFeature: JSONObject? = null
        var minDistance = Double.MAX_VALUE

        for (i in 0 until geoJsonData.length()) {
            val feature = geoJsonData.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val geometryType = geometry.getString("type")

            if (geometryType == "LineString") {
                val coordinates = geometry.getJSONArray("coordinates")

                // Recorremos los puntos del LineString
                for (j in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(j)
                    val lon = point.getDouble(0)
                    val lat = point.getDouble(1)

                    val distance = haversine(latLng.latitude, latLng.longitude, lat, lon)
                    if (distance < minDistance) {
                        minDistance = distance
                        nearestFeature = feature
                    }
                }
            }
        }

        // Rango umbral minimo en metros
        val distanceThreshold = 50.0 // metros
        return if (minDistance <= distanceThreshold) nearestFeature else null
    }
    // Función Haversine para calcular la distancia entre dos puntos en una esfera
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
    private fun getCurrentLocation(latLng: LatLng) {
        // Cargar JSON
        if (!::geoJsonData.isInitialized) {
            loadGeoJson()
        }
        // Buscar el segmento de carretera más cercano
        val nearestFeature = findNearestRoadSegment(latLng)
        direccion = nearestFeature

        if (nearestFeature != null) {
            val streetName = nearestFeature.getJSONObject("properties").optString("name", "Calle desconocida")
            maxSpeed = getMaxSpeed(nearestFeature).toDouble()

            runOnUiThread {
                //Toast.makeText(this, "Estás en: $streetName. Límite de velocidad: $maxSpeed km/h", Toast.LENGTH_LONG).show()
                updateMapLocation(latLng, streetName)
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "No se encontró información de la calle.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Establecer limites de velocidad por tipos de calles
    private fun getMaxSpeed(feature: JSONObject): String {
        val properties = feature.getJSONObject("properties")

        val highwayType = properties.optString("highway", "")
        // Límites de velocidad por defecto según el tipo de carretera
        return when (highwayType) {
            "trunk" -> "70"
            "primary" -> "50"
            "secondary" -> "50"
            "tertiary" -> "30"
            "residential" -> "30"
            else -> "Desconocido"
        }
    }



    /*********************************************************************************************
     * Sistema de navegacion GPS
     ********************************************************************************************/

    private var lastLocation: Location? = null
    private var speed: Double = 0.0
    private var maxSpeed: Double = 0.0

    /**
     * Obtener velocidad actual por medio de GPS
     */
    private fun getSpeed(location: Location) {
        if (location.hasSpeed()) {
            speed = location.speed * 3.6 // Convertir a km/h
            //speed = 110.0
        } else if (lastLocation != null) {
            val distanceInMeters = lastLocation!!.distanceTo(location)
            val timeInSeconds = (location.time - lastLocation!!.time) / 1000.0

            if (timeInSeconds > 0) {
                val speedMps = distanceInMeters / timeInSeconds
                speed = speedMps * 3.6
            }
        } else {
            speed = 0.0
        }
        speedText.text = "Velocidad: %.2f km/h".format(speed)
        updateGlow(speed, maxSpeed)
        lastLocation = location
    }






    /*********************************************************************************************
     *   Actualizacion de marcas de colores en los bordes
     ********************************************************************************************/
    private lateinit var glowContainer: FrameLayout

    /**
     * Actualizar el color del borde en función de la velocidad y el límite
     */
    private fun updateGlow(speed: Double, limit: Double) {
        when {
            // Si la velocidad es mayor a +10 km/h del límite, rojo
            speed > limit + 10 -> {
                glowContainer.setBackgroundResource(R.drawable.border_glow_red)
            }
            // Si la velocidad es mayor al límite pero dentro de 10 km/h, amarillo
            speed > limit -> {
                glowContainer.setBackgroundResource(R.drawable.border_glow_yellow)
            }
            // Si la velocidad está dentro del límite o por debajo, verde
            else -> {
                glowContainer.setBackgroundResource(R.drawable.border_glow_green)
            }
        }
    }






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
    private var lastTimestamp = 0L
    override fun onSensorChanged(event: SensorEvent) {
        /*if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
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
        }*/
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /* No es necesario implementar este método */ }


    /**
     * Envio de datos a endpoint
     */
    private suspend fun sendData() {
        /*
        val jsonObject = JSONObject().apply {
            put("latitud", latitud)
            put("longitud", longitud)
            put("direccion", direccion)
            put("speed", "%.2f".format(speed))
            put("maxSpeed", "%.2f".format(maxSpeed))
            put("fecha", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))  // Fecha y hora actual
        }*/

        val newRegister=envRegistro(
            latitud = latitud.toString(),
            longitud = longitud.toString(),
            direccion = direccion,
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()).toString(),
            speed = "%.2f".format(speed),
            streetMaxSpeed = "%.2f".format(maxSpeed)
        )

        // Imprimir Log
        logPrintSpeed(speed, maxSpeed)

        ref.saveInfoToSv(retrofitService, newRegister)
        //ref.loadLocalRegsSv()
    }

    private val runnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                ref.loadLocalRegsSv()
            }
            // Programar la siguiente ejecución en 10 minutos
            handler.postDelayed(this, interval)
        }
    }
}