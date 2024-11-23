package ups.tesis.detectoraltavelocidad.conexionec2


import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import ups.tesis.detectoraltavelocidad.conexionec2.models.getTok
import ups.tesis.detectoraltavelocidad.conexionec2.models.resultCreacion
import ups.tesis.detectoraltavelocidad.conexionec2.models.tokenRequest
import ups.tesis.detectoraltavelocidad.conexionec2.models.userCreate
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Query
import ups.tesis.detectoraltavelocidad.conexionec2.models.envRegistro
import ups.tesis.detectoraltavelocidad.conexionec2.models.localDataSent
import ups.tesis.detectoraltavelocidad.conexionec2.models.newRecordResponse
import ups.tesis.detectoraltavelocidad.conexionec2.models.obtRegsId
import ups.tesis.detectoraltavelocidad.conexionec2.models.showRegs
import ups.tesis.detectoraltavelocidad.conexionec2.models.timeLeft

interface RetrofitService {
    @POST("login")
    suspend fun getTok(@Body request: tokenRequest): Response<getTok>

    @POST("user")
    suspend fun createAccount(@Body request: userCreate): Response<resultCreacion>

    @GET("token/time_left")
    suspend fun getTimeLeft(): Response<timeLeft>

    @POST("sp_nvrecord")
    suspend fun newRecord(@Body request: envRegistro): Response<newRecordResponse>

    @POST("sp_localsend")
    suspend fun saveBatch(@Body request: List<envRegistro>): Response<localDataSent>

    @GET("get_spdrecord_user")
    suspend fun getSpdRecordUser(@Query("limit") limit: Int): Response<obtRegsId>
}


object RetrofitServiceFactory {
    fun makeRetrofitService(token: String): RetrofitService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Connection", "keep-alive")  // Añadir "Connection: alive"
                    .build()
                val response = chain.proceed(request)


                if (response.header("Content-Length") == null) {
                    response.newBuilder()
                        .removeHeader("Content-Length")
                        .build()
                } else {
                    response
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("http://98.81.148.196:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(RetrofitService::class.java)
    }
}

