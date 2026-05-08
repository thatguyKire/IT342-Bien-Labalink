package edu.cit.bien.labalink.api

import edu.cit.bien.labalink.model.ApiResponse
import edu.cit.bien.labalink.model.BookingRequest
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.model.LoginResponse
import edu.cit.bien.labalink.model.MachineResponse
import edu.cit.bien.labalink.model.PaymentHistoryItem
import edu.cit.bien.labalink.model.PaymentIntentResponse
import edu.cit.bien.labalink.model.RegisterRequest
import edu.cit.bien.labalink.model.WalletResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/google-login")
    suspend fun googleLogin(
        @Body request: Map<String, String>
    ): Response<LoginResponse>

    @GET("api/machines")
    suspend fun getAllMachines(
    ): Response<List<MachineResponse>>

    @GET("api/bookings")
    suspend fun getAllBookings(
        @Query("status") status: String? = null
    ): Response<List<BookingResponse>>

    @POST("api/bookings")
    suspend fun createBooking(
        @Body request: BookingRequest
    ): Response<BookingResponse>

    @GET("api/payments/wallet/{userId}")
    suspend fun getWalletBalance(
        @Path("userId") userId: Long
    ): Response<WalletResponse>

    @POST("api/payments/create-intent")
    suspend fun createPaymentIntent(
        @Body request: Map<String, Any>
    ): Response<PaymentIntentResponse>

    @POST("api/payments/confirm")
    suspend fun confirmPayment(
        @Body request: Map<String, String>
    ): Response<PaymentHistoryItem>

    @GET("api/payments/user/{email}")
    suspend fun getPaymentHistory(
        @Path("email") email: String
    ): Response<List<PaymentHistoryItem>>

    @POST("api/bookings")
    suspend fun activateMachineByQr(
        @Body request: Map<String, Any>
    ): Response<BookingResponse>
}