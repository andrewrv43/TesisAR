package ups.tesis.detectoraltavelocidad.conexionec2.models

import com.google.gson.JsonObject
import org.json.JSONObject

data class getTok(
    val token: String
)

data class tokenRequest(
    val username: String,
    val password: String
)

data class userCreate(
    val user: String,
    val pwd: String
)

data class resultCreacion(
    val fecha_creacion: String,
    val id: String,
    val pwd: String,
    val user: String
)
data class timeLeft(
    val time_left: String
)

data class newRecordResponse(
    val id: String
)
data class envRegistro(
    val latitud: String,
    val longitud: String,
    val direccion: JSONObject?,
    val fecha: String,
    val speed: String,
    val streetMaxSpeed: String
)