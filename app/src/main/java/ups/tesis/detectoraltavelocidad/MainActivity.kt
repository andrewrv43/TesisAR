package ups.tesis.detectoraltavelocidad

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitService
import ups.tesis.detectoraltavelocidad.conexionec2.RetrofitServiceFactory
import ups.tesis.detectoraltavelocidad.conexionec2.models.getTok
import ups.tesis.detectoraltavelocidad.conexionec2.models.resultCreacion
import ups.tesis.detectoraltavelocidad.conexionec2.models.tokenRequest
import ups.tesis.detectoraltavelocidad.conexionec2.models.userCreate
import android.content.Context
import android.content.SharedPreferences
import android.widget.ProgressBar
import android.app.Dialog
import android.view.LayoutInflater
import kotlinx.coroutines.delay
import ups.tesis.detectoraltavelocidad.conexionec2.Referencias

class MainActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var blurView: View
    lateinit var btnLogin:Button
    lateinit var struser:EditText
    lateinit var strupass:EditText
    lateinit var crearCuentatxt: TextView
    lateinit var layourcontenedor:LinearLayout
    lateinit var confpass:EditText
    var estado:Boolean = true
    lateinit var textoTitulo:TextView
    var usrInfo:MutableMap<String, Any> = mutableMapOf()
    lateinit var retrofitService:RetrofitService
    lateinit var ref: Referencias

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        progressBar=findViewById(R.id.progressBar2)
        blurView = findViewById(R.id.blurView)
        ref = Referencias(this)
        lifecycleScope.launch {
            checkIfTokenExists()
        }

        //# region Inicializacion variables
        btnLogin = findViewById(R.id.login_btn)
        struser= findViewById(R.id.username)
        strupass=findViewById(R.id.password)
        crearCuentatxt= findViewById(R.id.textView3)
        crearCuentatxt.text = Html.fromHtml("<u>Crear Cuenta</u>",1)
        layourcontenedor=findViewById(R.id.layoutContenedor)
        confpass=findViewById(R.id.passwordconf)
        textoTitulo=findViewById(R.id.textoTitulo)
        textoTitulo.setText(R.string.txtLogin)
        //#endregion
        //# region Funciones
        btnLogin.setOnClickListener{
            lifecycleScope.launch {
                btnLogginOnClick()
            }
        }
        crearCuentatxt.setOnClickListener{
            btnCrearCuentaOnClick()
        }
        //verifica que no existan espacios en la cadena de struser
        struser.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                println("before....")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                println("funcioncambios")
                s?.let{
                    if(it.contains(" ")){
                        struser.setText(it.toString().replace(" ",""))

                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                println("funcionposterior")
                struser.setSelection(struser.text.length)
            }


        })
        //# endregion
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private suspend fun btnLogginOnClick() {
        if(estado){
            val loginReq = tokenRequest(
                username = struser.text.toString(),
                password = strupass.text.toString()
            )
            // TODO: Eliminar el admin como iniciar sesion esta por ahora para eliminar la verificacion obligatoria
            if (makeGetTokenRequest(loginReq) || struser.text.toString()=="admin"){
                changeToMaps()
            }
        }else{
           if(confpass.text.toString()==strupass.text.toString()){
               if(crearCuenta()){
                   creacion_login()
               }
           }else{
               alertBox(titulo = "ALERTA", texto = R.string.txtContraseñaNoCoincide, btnTxt = "Continuar")
           }
        }
    }
    private fun btnCrearCuentaOnClick() {
        if (estado){
               creacion_login(1)

        }else{
            creacion_login()
        }
    }
    private suspend fun crearCuenta(): Boolean {
        val creationReq = userCreate(
            user = struser.text.toString(),
            pwd = strupass.text.toString()
        )
        val response = makeCreateAccountRequest(creationReq)
        return response?.let {
            if (it.isSuccessful) {
                val result = it.body()
                result?.let { res ->
                    println("Cuenta creada exitosamente: ${res.user}, ID: ${res.id}")
                    usrInfo = mutableMapOf("id" to res.id, "user" to res.user)
                }
                true
            } else {
                println("Error en la creación: Código ${it.code()} - ${it.message()}")
                false
            }
        } ?: false
    }

    private fun creacion_login(proc: Int = 0) {
        if (proc == 1) {
            // Crear cuenta
            confpass.visibility = View.VISIBLE
            btnLogin.setText(R.string.txtCrearCuenta)
            textoTitulo.setText(R.string.txtCrearCuenta)
            estado = false
            crearCuentatxt.text = Html.fromHtml("<u>${getString(R.string.txtLogin)}</u>", 1)
        } else {
            // Iniciar sesión
            confpass.visibility = View.GONE
            btnLogin.setText(R.string.txtLogin)
            textoTitulo.setText(R.string.txtLogin)
            estado = true
            crearCuentatxt.text = Html.fromHtml("<u>${getString(R.string.txtCrearCuenta)}</u>", 1)
        }
    }
    private fun alertBox(titulo:String, texto: Int, btnTxt:String){
        val artDialogBuilder=AlertDialog.Builder(this@MainActivity)
        artDialogBuilder.setTitle(titulo)
        artDialogBuilder.setMessage(texto)
        artDialogBuilder.setCancelable(false)
        artDialogBuilder.setPositiveButton(btnTxt){_,_->

        }
        artDialogBuilder.create().show()
    }

    private suspend fun makeCreateAccountRequest(request: userCreate): Response<resultCreacion>? {
        return try {
            val response = retrofitService.createAccount(request)

            if (response.isSuccessful) {
                return response
            } else {
                withContext(Dispatchers.Main) {
                    when (response.code()) {
                        409 -> {
                            Toast.makeText(this@MainActivity, "Este nombre de usuario ya existe, por favor ingrese otro.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this@MainActivity, "Error al crear la cuenta, por favor intente de nuevo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    private suspend fun makeGetTokenRequest(request: tokenRequest): Boolean {
        return try {
            val response: Response<getTok> = retrofitService.getTok(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                responseBody?.let {
                    usrInfo["token"] = it.token
                    println("Token obtenido exitosamente: ${it.token}")
                    ref.saveToPreferences(it.token, "auth_token")
                    ref.saveToPreferences(request.username,"username")
                    ref.saveToPreferences(request.password,"password")
                    retrofitService = ref.initializeRetrofitService(it.token)
                    true
                } ?: run {
                    println("El cuerpo de la respuesta es nulo")
                    false
                }
            } else {
                when (response.code()) {
                    401 -> {
                        // Manejo específico para error 401
                        val errorResponse = response.errorBody()?.string()
                        errorResponse?.let {
                            val errorMessage = JSONObject(it).optString("message", "Usuario o contraseña incorrectos")
                            println("Error de autenticación: $errorMessage")
                            Toast.makeText(
                                this@MainActivity,
                                "Error: $errorMessage",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else -> {
                        // Manejo de otros códigos de error
                        println("Error en la obtención de token: Código ${response.code()} - ${response.message()}")
                        Toast.makeText(
                            this@MainActivity,
                            "Error desconocido. Código: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                false
            }
        } catch (e: Exception) {
            // Manejo de excepciones
            println("Excepción: ${e.message}")
            Toast.makeText(this@MainActivity, "Error de autenticación. Intente nuevamente.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private suspend fun checkIfTokenExists() {
        progressBar.bringToFront()
        progressBar.visibility = View.VISIBLE
        blurView.visibility = View.VISIBLE

        val token = ref.getFromPreferences("auth_token")

        if (token != "") {
            retrofitService = ref.initializeRetrofitService(token)
            if(getLifeTimeToken()>2) {
                changeToMaps()
            }
        } else {
            ("")
        }
        delay(1000L)
        progressBar.visibility = View.GONE
        blurView.visibility = View.GONE
    }

    private suspend fun getLifeTimeToken(): Int {
        val response = retrofitService.getTimeLeft()
        return if (response.isSuccessful) {
            val responseBody = response.body()
            responseBody?.let {
                it.time_left.toInt()
            } ?: 0
        } else {
            0
        }
    }

    private fun changeToMaps(){
        val intent=Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

}