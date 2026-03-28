package edu.cit.bien.labalink.api

import edu.cit.bien.labalink.model.ApiResponse
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.model.LoginResponse
import edu.cit.bien.labalink.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // Removed the extra "api/" so it appends cleanly to the BASE_URL
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse>

    // Removed the extra "api/" here as well
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}