package ups.tesis.detectoraltavelocidad.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ups.tesis.detectoraltavelocidad.MapsActivity
import ups.tesis.detectoraltavelocidad.R
import ups.tesis.detectoraltavelocidad.conexionec2.Referencias
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitService
import ups.tesis.detectoraltavelocidad.conexionec2.models.envRegistro
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SpeedService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private var direccion: JSONObject? = null

    private var lastLocation: Location? = null
    private var speed: Double = 0.0
    private var maxSpeed: Double = 0.0

    companion object {
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    val speedLiveData = MutableLiveData<Double>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeLocationComponents()
        Log.d("SpeedService", "onCreate()")
        retrofitService = ref.initializeRetrofitService(ref.getFromPreferences("auth_token"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundService()
        startLocationUpdates()
        Log.d("SpeedService", "onStartCommand()")
        return START_STICKY
    }

    inner class LocalBinder : Binder() {
        fun getService(): SpeedService = this@SpeedService
    }
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return LocalBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Ubicación",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            Log.d("SpeedService", "createNotificationChannel()")
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aplicación en ejecución")
            .setContentText("Monitoreando ubicación y velocidad")
            .setSmallIcon(R.drawable.logo_app_tesis)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d("SpeedService", "startForegroundService()")
    }

    private fun initializeLocationComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        var lastSendDataTime = 0L
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    latitud = location.latitude
                    longitud = location.longitude
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    CoroutineScope(Dispatchers.Default).launch {
                        getCurrentLocation(currentLatLng)
                        getSpeed(location)
                        speedLiveData.postValue(speed)
                        Log.d("SpeedService", "Recolectando velocidad y ubicacion en segundo plano")

                        val currentTime = System.currentTimeMillis()
                        if (speed > maxSpeed) {
                            if (currentTime - lastSendDataTime >= 3000) {
                                lastSendDataTime = currentTime
                                withContext(Dispatchers.IO) {
                                    sendData()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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





    /************************************************************************************************
     * Obtener dirección actual por medio de GPS
     ************************************************************************************************/
    private suspend fun getCurrentLocation(latLng: LatLng) {
        // Cargar JSON
        if (!::geoJsonData.isInitialized) {
            withContext(Dispatchers.IO) {
                loadGeoJson()
            }
        }
        // Buscar el segmento de carretera más cercano
        direccion = findNearestRoadSegment(latLng)
        try {
            maxSpeed = getMaxSpeed(direccion).toDouble()
        } catch (e: Exception) {
            maxSpeed = 0.0
            Log.e("getCurrentLocation", "Error al parsear maxSpeed: ${e.message}")
        }
    }

    private lateinit var geoJsonData: JSONArray
    // Cargar mapa de calles de Quito
    private suspend fun loadGeoJson() {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = assets.open("quito01.geojson")
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val geoJsonString = bufferedReader.use { it.readText() }
                val geoJsonObject = JSONObject(geoJsonString)
                geoJsonData = geoJsonObject.getJSONArray("features")
                Log.d("SpeedService", "loadGeoJson()")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Encontrar la calle mas cercana por ubicacion
    private suspend fun findNearestRoadSegment(latLng: LatLng): JSONObject? {
        var nearestFeature: JSONObject? = null
        var minDistance = Double.MAX_VALUE
        withContext(Dispatchers.IO) {
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
    // Establecer limites de velocidad por tipos de calles
    private fun getMaxSpeed(feature: JSONObject?): String {
        val properties = feature?.getJSONObject("properties")

        val highwayType = properties?.optString("highway", "")
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

    /************************************************************************************************
     * Obtener velocidad actual por medio de GPS
     ************************************************************************************************/
    private fun getSpeed(location: Location) {
        if (location.hasSpeed()) {
            //speed = location.speed * 3.6 // Convertir a km/h
            speed = 777.0
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
        lastLocation = location
    }



    private val ref = Referencias(context = this) // <----- Revisar esto
    private lateinit var retrofitService: RetrofitService
    /**
     * Envio de datos a endpoint
     */
    private suspend fun sendData() {
        withContext(Dispatchers.IO) {
            val newRegister = envRegistro(
                latitud = latitud.toString(),
                longitud = longitud.toString(),
                direccion = direccion,
                fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    .toString(),
                speed = "%.2f".format(speed),
                streetMaxSpeed = "%.2f".format(maxSpeed)
            )
            Log.d("SpeedService", "sendData()")
            //ref.saveInfoToSv(retrofitService, newRegister)
        }
    }
}