package edu.cit.bien.labalink.model

data class BookingResponse(
    val id: Long,
    val userEmail: String,
    val username: String,
    val machineId: Long,
    val machineName: String,
    val machineType: String,
    val status: String,
    val startTime: String,
    val endTime: String?,
    val totalPrice: Double,
    val createdAt: String
)