package edu.cit.bien.labalink.model

data class QrActivationRequest(
    val userId: Long,
    val qrToken: String
)