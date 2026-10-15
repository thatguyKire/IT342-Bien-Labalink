package edu.cit.bien.labalink.model

// Request/response shapes of the LabaLink REST API (see docs/TRD.md).
// Money arrives as JSON numbers; the server is the source of truth for
// every amount, so these are display-only on the device.

data class ApiResponse(
    val success: Boolean,
    val message: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class GoogleTokenRequest(
    val idToken: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val role: String,
    val email: String,
    val username: String,
    val userId: Long
)

data class MachineResponse(
    val id: Long,
    val machineName: String,
    val machineType: String,
    val status: String,
    val hourlyRate: Double,
    val createdAt: String?
)

/** userId and price are decided by the server, never sent by the app. */
data class BookingRequest(
    val machineId: Long,
    val qrToken: String,
    val durationMinutes: Int? = null
)

data class BookingResponse(
    val id: Long,
    val machineId: Long,
    val machineName: String,
    val machineType: String,
    val status: String,
    val startTime: String,
    val endTime: String?,
    val durationMinutes: Int?,
    val totalPrice: Double,
    val createdAt: String?
)

data class WalletResponse(
    val walletBalance: Double
)

data class TopUpRequest(
    val amount: Double
)

data class PaymentResponse(
    val id: Long,
    val amount: Double,
    val currency: String?,
    val providerReference: String?,
    val status: String,
    val createdAt: String?,
    val paidAt: String?,
    val checkoutUrl: String?
)
