package edu.cit.bien.labalink.api

import edu.cit.bien.labalink.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 🟢 The hardcoded BASE_URL has been removed!

    var authToken: String? = null

    private val loggingInterceptor =
        HttpLoggingInterceptor()
            .apply {
                level = HttpLoggingInterceptor
                    .Level.BODY
            }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // 🟢 Dynamically pulls Debug or Release URL
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    // (If you have other APIs like machineApi or bookingApi,
    // update their .baseUrl() to BuildConfig.BASE_URL as well!)
}