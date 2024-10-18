package ups.tesis.detectoraltavelocidad.conexionec2.models

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