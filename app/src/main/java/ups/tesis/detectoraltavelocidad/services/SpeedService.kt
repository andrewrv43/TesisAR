package ups.tesis.detectoraltavelocidad.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.util.JsonReader
import android.util.JsonToken
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.locationtech.jts.geom.*
import org.locationtech.jts.index.strtree.STRtree
import ups.tesis.detectoraltavelocidad.conexionec2.CargaDatos
import ups.tesis.detectoraltavelocidad.dataStore
import kotlin.math.abs

class SpeedService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var envioDatos: CargaDatos
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
    val maxSpeedLiveData = MutableLiveData<Double>()
    val streetNameLiveData = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeLocationComponents()
        envioDatos = CargaDatos()
        retrofitService = ref.initializeRetrofitService(ref.getFromPreferences("auth_token"))
        val dataStore = PreferenceDataStoreFactory.create {
            applicationContext.filesDir.resolve("datastore/local_regs.preferences_pb")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundService()
        startLocationUpdates()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("SpeedService", "Aplicación cerrada, deteniendo servicio")
        stopSelf() // Detiene el servicio
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

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        var lastSendDataTime = 0L
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    latitud = location.latitude
                    longitud = location.longitude
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    CoroutineScope(Dispatchers.IO).launch {
                        getCurrentLocation(currentLatLng)
                        getSpeed(location)
                        speedLiveData.postValue(speed)
                        maxSpeedLiveData.postValue(maxSpeed)
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
     * Obtener velocidad actual por medio de GPS y aplicar filtros para suavizado
     ************************************************************************************************/
    private val speedKalmanFilter = KalmanFilter(0.001)
    private val speedReadings = mutableListOf<Double>()
    private val SPEED_READINGS_MAX_SIZE = 5
    private var lastSpeed = 0.0
    private val MIN_ACCURACY = 10f // metros
    private val MAX_SPEED = 200.0 // km/h
    private val MAX_SPEED_INCREMENT = 10.0 // km/h por segundo
    private val MIN_SPEED_THRESHOLD = 1.0 // km/h

    private fun getSpeed(location: Location) {
        if (location.accuracy <= MIN_ACCURACY) {
            val currentTime = location.time
            val lastTime = lastLocation?.time ?: 0L
            val timeDelta = (currentTime - lastTime) / 1000.0

            val currentSpeed: Double = if (location.hasSpeed()) {
                location.speed * 3.6 // Convertir a km/h
            } else if (lastLocation != null && lastLocation!!.accuracy <= MIN_ACCURACY && timeDelta > 0) {
                val distanceInMeters = lastLocation!!.distanceTo(location)
                val speedMps = distanceInMeters / timeDelta
                speedMps * 3.6
            } else {
                0.0
            }

            // Validar velocidad máxima
            val validatedSpeed = currentSpeed.coerceIn(0.0, MAX_SPEED)

            // Evitar picos de velocidad
            val speedDelta = validatedSpeed - lastSpeed
            val maxAllowedSpeedDelta = MAX_SPEED_INCREMENT * timeDelta

            val finalSpeed = if (abs(speedDelta) <= maxAllowedSpeedDelta && validatedSpeed >= MIN_SPEED_THRESHOLD) {
                validatedSpeed
            } else {
                lastSpeed // Mantén la última velocidad válida
            }

            // Añadir a las lecturas para suavizado
            speedReadings.add(finalSpeed)
            if (speedReadings.size > SPEED_READINGS_MAX_SIZE) {
                speedReadings.removeAt(0)
            }

            // Suavizar con media móvil
            val smoothedSpeed = speedReadings.average()

            // Actualizar con filtro de Kalman
            speed = speedKalmanFilter.update(smoothedSpeed)
            lastSpeed = speed
        } else {
            // La precisión es baja, no confiar en esta lectura
            speed = speedKalmanFilter.update(0.0)
        }
        lastLocation = location
    }


    /************************************************************************************************
     * Cargar GeoJson como indice espacial
     ************************************************************************************************/
    private var isGeoJsonLoaded = false
    private lateinit var spatialIndex: STRtree
    private val geometryFactory = GeometryFactory()

    private suspend fun loadGeoJson() {
        withContext(Dispatchers.IO) {
            try {
                assets.open("quito01.geojson").use { inputStream ->
                    InputStreamReader(inputStream).use { inputStreamReader ->
                        JsonReader(inputStreamReader).use { jsonReader ->
                            // El objeto raíz es un FeatureCollection
                            jsonReader.beginObject()
                            var foundFeatures = false
                            spatialIndex = STRtree()

                            while (jsonReader.hasNext()) {
                                val name = jsonReader.nextName()
                                if (name == "features") {
                                    foundFeatures = true
                                    jsonReader.beginArray()
                                    while (jsonReader.hasNext()) {
                                        val feature = readJsonObject(jsonReader)
                                        val geometry = parseGeometry(feature.getJSONObject("geometry"))
                                        if (geometry != null && !geometry.isEmpty) {
                                            spatialIndex.insert(geometry.envelopeInternal, Pair(feature, geometry))
                                        }
                                    }
                                    jsonReader.endArray()
                                } else {
                                    jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()

                            if (foundFeatures) {
                                spatialIndex.build()
                                isGeoJsonLoaded = true
                                Log.d("SpeedService", "loadGeoJson() - Archivo cargado e índice espacial construido")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SpeedService", "Error cargando GeoJSON: ${e.message}")
            }
        }
    }
    // Método recursivo para leer cualquier estructura JSON
    private fun readJsonObject(jsonReader: JsonReader): JSONObject {
        val jsonObject = JSONObject()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            when (jsonReader.peek()) {
                JsonToken.BEGIN_OBJECT -> jsonObject.put(key, readJsonObject(jsonReader)) // Llama recursivamente para objetos
                JsonToken.BEGIN_ARRAY -> jsonObject.put(key, readJsonArray(jsonReader)) // Llama recursivamente para arrays
                JsonToken.STRING -> jsonObject.put(key, jsonReader.nextString())
                JsonToken.NUMBER -> jsonObject.put(key, jsonReader.nextDouble())
                JsonToken.BOOLEAN -> jsonObject.put(key, jsonReader.nextBoolean())
                JsonToken.NULL -> {
                    jsonReader.nextNull()
                    jsonObject.put(key, JSONObject.NULL)
                }
                else -> throw IllegalStateException("Tipo de dato inesperado en JSON")
            }
        }
        jsonReader.endObject()
        return jsonObject
    }
    private fun readJsonArray(jsonReader: JsonReader): JSONArray {
        val jsonArray = JSONArray()
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            when (jsonReader.peek()) {
                JsonToken.BEGIN_OBJECT -> jsonArray.put(readJsonObject(jsonReader)) // Objeto dentro del array
                JsonToken.BEGIN_ARRAY -> jsonArray.put(readJsonArray(jsonReader)) // Array anidado
                JsonToken.STRING -> jsonArray.put(jsonReader.nextString())
                JsonToken.NUMBER -> jsonArray.put(jsonReader.nextDouble())
                JsonToken.BOOLEAN -> jsonArray.put(jsonReader.nextBoolean())
                JsonToken.NULL -> {
                    jsonReader.nextNull()
                    jsonArray.put(JSONObject.NULL)
                }
                else -> throw IllegalStateException("Tipo de dato inesperado en JSON")
            }
        }
        jsonReader.endArray()
        return jsonArray
    }

    private fun parseGeometry(geometryJson: JSONObject): Geometry {
        return when (val type = geometryJson.getString("type")) {
            "LineString" -> {
                val coordsArray = geometryJson.getJSONArray("coordinates")
                val coordinates = Array(coordsArray.length()) { i ->
                    val coord = coordsArray.getJSONArray(i)
                    Coordinate(coord.getDouble(0), coord.getDouble(1))
                }
                geometryFactory.createLineString(coordinates)
            }
            else -> throw IllegalArgumentException("Tipo de geometría no soportado: $type")
        }
    }
    /**
     * Calcula el heading (dirección) de un segmento de carretera
     */
    private fun calculateHeading(segment: Geometry): Double {
        if (segment is LineString && segment.numPoints >= 2) {
            val coord1 = segment.getCoordinateN(0)
            val coord2 = segment.getCoordinateN(1)
            return Math.toDegrees(atan2(coord2.y - coord1.y, coord2.x - coord1.x))
        }
        return 0.0
    }

    /**
     * Calcula la diferencia angular entre dos headings (dirección)
     */
    private fun headingDifference(h1: Double, h2: Double): Double {
        val diff = abs(h1 - h2) % 360
        return if (diff > 180) 360 - diff else diff
    }

    /**
     * Busca el segmento de carretera más cercano a la ubicación actual
     */
    private suspend fun findNearestRoadSegment(latLng: LatLng, userHeading: Double): JSONObject? {
        val userLocation = Coordinate(latLng.longitude, latLng.latitude)
        val searchEnvelope = Envelope(userLocation)
        val searchRadius = 0.001 // Ajustar valor (en grados)

        // Expandir el área de búsqueda
        searchEnvelope.expandBy(searchRadius)

        val nearbyRoads = spatialIndex.query(searchEnvelope)

        var bestFeature: JSONObject? = null
        var lowestScore = Double.MAX_VALUE

        withContext(Dispatchers.IO) {
            for (candidate in nearbyRoads) {
                val (feature, geometry) = candidate as Pair<JSONObject, Geometry>

                // Proyectar la ubicación del usuario sobre el segmento de carretera
                val distance = geometry.distance(geometryFactory.createPoint(userLocation))
                val roadHeading = calculateHeading(geometry)
                val headingDiff = headingDifference(userHeading, roadHeading)

                // Función de puntuación que considera distancia y diferencia de rumbo
                val score = distance * 0.7 + headingDiff * 0.3 // Ajusta los pesos según sea necesario

                if (score < lowestScore) {
                    lowestScore = score
                    bestFeature = feature
                }
            }
        }

        // Umbral de puntuación (ajusta según sea necesario)
        val scoreThreshold = 2
        Log.d("SpeedService", "lowestScore: ${lowestScore}")
        return if (lowestScore <= scoreThreshold) bestFeature else null
    }


    private val recentPositions = mutableListOf<LatLng>()

    private fun addRecentPosition(latLng: LatLng) {
        recentPositions.add(latLng)
        if (recentPositions.size > 5) { // Mantén solo los últimos 5 puntos
            recentPositions.removeAt(0)
        }
    }

    private suspend fun getCurrentLocation(latLng: LatLng) {
        // Cargar JSON
        if (!isGeoJsonLoaded) {
            loadGeoJson()
        }

        val filteredLatLng = filterGps(latLng.latitude, latLng.longitude)
        addRecentPosition(filteredLatLng)

        // Buscar el segmento de carretera más cercano
        val userHeading = calculateUserHeading()
        direccion = findNearestRoadSegment(latLng, userHeading)
        direccion?.let { feature ->
            maxSpeed = getMaxSpeed(feature).toDoubleOrNull() ?: 0.0

            val properties = feature.getJSONObject("properties")
            val streetName = properties.optString("name", "Desconocido")

            streetNameLiveData.postValue(streetName)
        } ?: run {
            maxSpeed = 0.0
            streetNameLiveData.postValue("Desconocido")
        }
    }

    private fun calculateUserHeading(): Double {
        if (recentPositions.size >= 2) {
            val lastPos = recentPositions[recentPositions.size - 1]
            val prevPos = recentPositions[recentPositions.size - 2]
            return calculateBearing(prevPos, lastPos)
        }
        return 0.0
    }

    private fun calculateBearing(start: LatLng, end: LatLng): Double {
        val startLat = Math.toRadians(start.latitude)
        val startLng = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLng = Math.toRadians(end.longitude)

        val dLng = endLng - startLng
        val y = sin(dLng) * cos(endLat)
        val x = cos(startLat) * sin(endLat) - sin(startLat) * cos(endLat) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private val kalmanFilterLat = KalmanFilter(0.0001)
    private val kalmanFilterLng = KalmanFilter(0.0001)

    private fun filterGps(lat: Double, lng: Double): LatLng {
        val filteredLat = kalmanFilterLat.update(lat)
        val filteredLng = kalmanFilterLng.update(lng)
        return LatLng(filteredLat, filteredLng)
    }




    private val ref = Referencias(context = this)
    private lateinit var retrofitService: RetrofitService
    /**
     * Envio de datos a endpoint
     */
    private suspend fun sendData(context: Context = this@SpeedService) {
            val newRegister = envRegistro(
                latitud = latitud.toString(),
                longitud = longitud.toString(),
                direccion = direccion,
                fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    .toString(),
                speed = "%.2f".format(Locale.US,speed),
                streetMaxSpeed = "%.2f".format(Locale.US,maxSpeed)
            )
            Log.d("SpeedService", "sendData()")
            envioDatos.saveInfoToSv(dataStore,retrofitService, newRegister)

    }
}

class KalmanFilter(private val q: Double) {
    private var x = 0.0 // Estimación
    private var p = 1.0 // Error de estimación
    private val r = 1.0 // Varianza de la medición

    fun update(z: Double): Double {
        // Predicción
        p += q
        // Actualización
        val k = p / (p + r)
        x += k * (z - x)
        p *= (1 - k)
        return x
    }
}
