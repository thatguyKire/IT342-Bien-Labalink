package edu.cit.bien.labalink.model

data class WalletResponse(
    val walletBalance: Double
)

data class PaymentIntentResponse(
    val id: Long,
    val clientSecret: String,
    val providerReference: String,
    val amount: Double,
    val status: String
)

data class PaymentHistoryItem(
    val id: Long,
    val amount: Double,
    val providerReference: String,
    val status: String,
    val createdAt: String
)