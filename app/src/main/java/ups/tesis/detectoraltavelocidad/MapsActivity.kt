package ups.tesis.detectoraltavelocidad

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import ups.tesis.detectoraltavelocidad.conexionec2.Referencias
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitService
import ups.tesis.detectoraltavelocidad.permissions.LocationPermissions
import ups.tesis.detectoraltavelocidad.services.SpeedService
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import ups.tesis.detectoraltavelocidad.conexionec2.CargaDatos
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "local_regs")

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, /*GoogleMap.OnMapClickListener,*/ LocationPermissions.PermissionCallback {
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 60000
    private lateinit var map: GoogleMap
    private lateinit var infoBtn: ImageView
    private lateinit var infoBtn2: Button
    private lateinit var profileBtn: ImageView
    private lateinit var profileBtn2: Button
    private lateinit var toggleButton: ImageButton

    private lateinit var streetText: TextView
    private lateinit var speedText: TextView
    private lateinit var maxSpeedText: TextView

    private lateinit var data: CargaDatos
    private var lastLocation: Location? = null
    private var speed: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var showingFirstIcon = true


    val ref = Referencias(context = this)
    private lateinit var retrofitService: RetrofitService
    private var backPressedTime: Long = 0
    private lateinit var toast: Toast

    private lateinit var locationPermissions: LocationPermissions

    private var mapStyleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Recargar el estilo del mapa cuando se recibe el broadcast
            applyMapStyle()
        }
    }

    private var speedService: SpeedService? = null
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SpeedService.LocalBinder
            speedService = binder.getService()
            isBound = true
            speedService?.let { service ->
                service.speedLiveData.observe(this@MapsActivity, Observer { speed ->
                    // Actualiza la UI con la velocidad
                    speedText.text = "%.1f".format(Locale.US,speed)
                    updateGlow(speed, maxSpeed)
                })
                service.maxSpeedLiveData.observe(this@MapsActivity, Observer { maxSpeed ->
                    this@MapsActivity.maxSpeed = maxSpeed
                    maxSpeedText.text = "%.0f".format(Locale.US,maxSpeed)
                })
                service.streetNameLiveData.observe(this@MapsActivity, Observer { streetName ->
                    streetText.text = "Calle: $streetName"
                    Log.d("SpeedService", "MapsActivity recibe nombre de calle: $streetName")
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            speedService = null
        }
    }
    override fun onStart() {
        super.onStart()
        Intent(this, SpeedService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }


    /**
     * Funcion que se ejecuta cuando se inicia la actividad MapsActivity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        locationPermissions = LocationPermissions(this, this)

        createMapFragment()
        registerBroadcastReceiver()
        createLayoutVariables()


        retrofitService=ref.initializeRetrofitService(ref.getFromPreferences("auth_token"))
        data= CargaDatos()
        if(ref.hayConexionAInternet(this)){
            actualizacionApp()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMyLocationClickListener(this)
        /*map.setOnMapClickListener(this)*/
        locationPermissions.checkAndRequestLocationPermissions()

        // Aplicar el estilo del mapa
        applyMapStyle()
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, interval)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destruir el BroadcastReceiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mapStyleReceiver)
        stopSpeedService()
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
        profileBtn = findViewById(R.id.perfilBtn)
        profileBtn2 = findViewById(R.id.perfilBtn2)

        // Establecer el listener de click para lanzar ProfileActivity
        profileBtn.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        profileBtn2.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        glowContainer = findViewById(R.id.glowContainer)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)
        glowContainer.startAnimation(pulseAnimation)

        toggleButton = findViewById(R.id.toggleButton)
        // Establecer el listener de click para cambiar el icono
        toggleButton.setOnClickListener {
            if (showingFirstIcon) {
                toggleButton.setImageResource(R.drawable.switch_off) // Icono OFF
                glowContainer.clearAnimation()
                glowContainer.setBackgroundResource(0)
            } else {
                toggleButton.setImageResource(R.drawable.switch_on) // Icono ON
                glowContainer.startAnimation(pulseAnimation)
                glowContainer.setBackgroundResource(R.drawable.border_glow_green)
            }
            showingFirstIcon = !showingFirstIcon
        }
    }

    /**
     *  Registra el BroadcastReceiver para recibir actualizaciones de estilo
     */
    private fun registerBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mapStyleReceiver, IntentFilter("com.example.UPDATE_MAP_STYLE"))
    }





    /************************************************************************************************
     *  SOLICITUD DE PERMISOS PARA FUNCIONAMIENTO DE LA APLICACION                                  *
     ************************************************************************************************/
    /**
     *  Activar la ubicacion del usuario
     */
    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        map.isMyLocationEnabled = true
    }
    override fun onPermissionGranted() {
        if (::map.isInitialized) {
            enableLocation()
            startSpeedService() // Iniciar servicio
        }
    }
    /**
     *  Maneja la respuesta de los permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
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







    /***********************************************************************************************
     *   ESTILO DEL MAPA
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
    private fun updateMapLocation(latLng: LatLng, streetName: String) {
        if (::map.isInitialized) {
            lastMarker?.remove()
            lastMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Estás en: $streetName")
            )
        }
    }

    /*********************************************************************************************
     *   Actualizacion de marcas de colores en los bordes
     ********************************************************************************************/
    private lateinit var glowContainer: FrameLayout

    /**
     * Actualizar el color del borde en función de la velocidad y el límite
     */
    private fun updateGlow(speed: Double, limit: Double) {
        if (!showingFirstIcon) {
            glowContainer.clearAnimation()
            glowContainer.setBackgroundResource(0)
            return
        }
        when {
            // Si la velocidad es mayor al límite, rojo
            speed > limit -> {
                glowContainer.setBackgroundResource(R.drawable.border_glow_red)
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
     * Inicializar los TextViews
     */
    private fun createLayoutVariables() {
        // Inicializar los TextViews para mostrar los valores
        streetText = findViewById(R.id.streetValue)
        speedText = findViewById(R.id.speedValue)
        maxSpeedText = findViewById(R.id.maxSpeedValue)
    }

    private val runnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                data.loadLocalRegsSv(this@MapsActivity.dataStore,retrofitService)
            }
            // Programar la siguiente ejecución en 10 minutos
            handler.postDelayed(this, interval)
        }
    }
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            toast.cancel()
            super.onBackPressed()
            return
        } else {
            toast = Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT)
            toast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }


    /*********************************************************************************************
     *   SPEEDSERVICE
     *   SERVICIO DE ACTUALIZACION DE VELOCIDAD Y UBICACION
     ********************************************************************************************/
    private fun startSpeedService() {
        // Iniciar el servicio de actualización de ubicación si la versión de Android es mayor o igual a Orea
        val intent = Intent(this, SpeedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    private fun stopSpeedService() {
        stopService(Intent(this, SpeedService::class.java))
        Log.d("MapsActivity", "Actividad destruida, servicio detenido")
    }

    private fun actualizacionApp(){
        lifecycleScope.launch {
            ref.actualizacion(retrofitService)
        }
    }
}