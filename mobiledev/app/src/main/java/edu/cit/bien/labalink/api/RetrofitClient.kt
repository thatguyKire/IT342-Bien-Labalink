package edu.cit.bien.labalink.api

import com.google.gson.Gson
import edu.cit.bien.labalink.BuildConfig
import edu.cit.bien.labalink.model.ApiResponse
import edu.cit.bien.labalink.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    lateinit var api: LabaLinkApi
        private set

    /**
     * @param onUnauthorized called when the server rejects our token
     *   (expired, revoked by logout elsewhere, or account disabled).
     */
    fun init(session: SessionManager, onUnauthorized: () -> Unit) {
        val baseUrl = BuildConfig.API_BASE_URL
        check(BuildConfig.DEBUG || baseUrl.startsWith("https://")) {
            "Release builds must use an https:// API_BASE_URL"
        }

        val authInterceptor = Interceptor { chain ->
            val token = session.token
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            if (response.code == 401 && token != null) {
                onUnauthorized()
            }
            response
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .apply {
                // Never log bodies (passwords, tokens) and never in release.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    })
                }
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LabaLinkApi::class.java)
    }
}

private val gson = Gson()

/** The server's human-readable error message, or [fallback]. */
fun Response<*>.errorMessage(fallback: String): String = try {
    errorBody()?.string()
        ?.let { gson.fromJson(it, ApiResponse::class.java)?.message }
        ?.takeIf { it.isNotBlank() }
        ?: fallback
} catch (e: Exception) {
    fallback
}
