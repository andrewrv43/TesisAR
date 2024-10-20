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

interface RetrofitService {
    @POST("login")
    suspend fun getTok(@Body request: tokenRequest): Response<getTok>

    @POST("user")
    suspend fun createAccount(@Body request: userCreate): Response<resultCreacion>
}


object RetrofitServiceFactory {
    fun makeRetrofitService(token: String): RetrofitService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Connection", "close")  // Añadir "Connection: close"
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("http://18.208.131.223:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(RetrofitService::class.java)
    }
}

