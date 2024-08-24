package ups.tesis.detectoraltavelocidad

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom

class MainActivity : AppCompatActivity() {
    lateinit var btnLogin:Button
    lateinit var struser:EditText
    lateinit var strupass:EditText
    lateinit var crearCuentatxt: TextView
    lateinit var layourcontenedor:LinearLayout
    lateinit var confpass:EditText
    lateinit var spacepassconf:Space
    var estado:Boolean = true
    lateinit var textoTitulo:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        //# region Inicializacion variables
        btnLogin = findViewById(R.id.login_btn)
        struser= findViewById(R.id.username)
        strupass=findViewById(R.id.password)
        crearCuentatxt= findViewById(R.id.textView3)
        crearCuentatxt.text = Html.fromHtml("<u>Crear Cuenta</u>",1)
        layourcontenedor=findViewById(R.id.layoutContenedor)
        confpass=findViewById(R.id.passwordconf)
        spacepassconf=findViewById(R.id.spacepassconf)
        textoTitulo=findViewById(R.id.textoTitulo)
        textoTitulo.setText(R.string.txtLogin)
        //#endregion
        //# region Funciones
        btnLogin.setOnClickListener{
            btnLogginOnClick()
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
    private fun btnLogginOnClick() {
        if(estado){
            println("Funciona y el usuario es: ${struser.text}")
        }else{
            //logica para crear cuenta
           if(confpass.text.toString()==strupass.text.toString()){
               if(crearCuenta()){
                   //layourcontenedor.layoutParams.height=256
                   confpass.visibility= View.GONE
                   spacepassconf.visibility=View.GONE
                   btnLogin.setText(R.string.txtLogin)
                   textoTitulo.setText(R.string.txtLogin)
                   estado=true
                   crearCuentatxt.text = Html.fromHtml("<u>${getString(R.string.txtCrearCuenta)}</u>",1)

               }
           }else{
               alertBox(titulo = "ALERTA", texto = R.string.txtContraseñaNoCoincide, btnTxt = "Continuar")
           }
        }
    }
    private fun btnCrearCuentaOnClick() {
        if (estado){
                confpass.visibility= View.VISIBLE
                spacepassconf.visibility=View.VISIBLE
                btnLogin.setText(R.string.txtCrearCuenta)
                textoTitulo.setText(R.string.txtCrearCuenta)
                estado=false
            crearCuentatxt.text = Html.fromHtml("<u>${getString(R.string.txtLogin)}</u>",1)


        }else{
            confpass.visibility= View.GONE
            spacepassconf.visibility=View.GONE
            btnLogin.setText(R.string.txtLogin)
            textoTitulo.setText(R.string.txtLogin)
            estado=true
            crearCuentatxt.text = Html.fromHtml("<u>${getString(R.string.txtCrearCuenta)}</u>",1)

        }
    }
    private fun crearCuenta(): Boolean {
        println("CREACION DE CUENTA")
        return true
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
}