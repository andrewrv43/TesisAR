package ups.tesis.detectoraltavelocidad.conexionec2

import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import ups.tesis.detectoraltavelocidad.conexionec2.models.getTok
import ups.tesis.detectoraltavelocidad.conexionec2.models.resultCreacion
import ups.tesis.detectoraltavelocidad.conexionec2.models.tokenRequest
import ups.tesis.detectoraltavelocidad.conexionec2.models.userCreate

interface RetrofitService {
//    @GET("login")
//    suspend fun getTok(@Body request: tokenRequest): getTok

    @POST("user")
    suspend fun createAccount(@Body request: userCreate): resultCreacion
}

object RetrofitServiceFactory {
    fun makeRetrofitService(token: String): RetrofitService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
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
