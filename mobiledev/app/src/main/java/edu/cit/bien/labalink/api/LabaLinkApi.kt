package edu.cit.bien.labalink.api

import edu.cit.bien.labalink.model.ApiResponse
import edu.cit.bien.labalink.model.BookingRequest
import edu.cit.bien.labalink.model.BookingResponse
import edu.cit.bien.labalink.model.GoogleTokenRequest
import edu.cit.bien.labalink.model.LoginRequest
import edu.cit.bien.labalink.model.LoginResponse
import edu.cit.bien.labalink.model.MachineResponse
import edu.cit.bien.labalink.model.PaymentResponse
import edu.cit.bien.labalink.model.RegisterRequest
import edu.cit.bien.labalink.model.TopUpRequest
import edu.cit.bien.labalink.model.WalletResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Customer-facing API. Every call except register/login/google is
 * authenticated by the bearer token that AuthInterceptor attaches, and the
 * server derives "who am I" from that token, never from request data.
 */
interface LabaLinkApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleTokenRequest
    ): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse>

    @GET("api/machines")
    suspend fun getMachines(): Response<List<MachineResponse>>

    @GET("api/bookings/me")
    suspend fun getMyBookings(): Response<List<BookingResponse>>

    @POST("api/bookings")
    suspend fun createBooking(
        @Body request: BookingRequest
    ): Response<BookingResponse>

    @POST("api/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Long): Response<BookingResponse>

    @GET("api/payments/wallet")
    suspend fun getWallet(): Response<WalletResponse>

    @GET("api/payments/me")
    suspend fun getMyPayments(): Response<List<PaymentResponse>>

    @POST("api/payments/checkout")
    suspend fun createCheckout(
        @Body request: TopUpRequest
    ): Response<PaymentResponse>

    @POST("api/payments/{id}/sync")
    suspend fun syncPayment(@Path("id") id: Long): Response<PaymentResponse>
}
