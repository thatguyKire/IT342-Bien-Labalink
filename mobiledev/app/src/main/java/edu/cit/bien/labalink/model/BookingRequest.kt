package edu.cit.bien.labalink.model

data class BookingRequest(
    val userId: Long,
    val machineId: Long,
    val startTime: String,
    val endTime: String?,
    val totalPrice: Double
)