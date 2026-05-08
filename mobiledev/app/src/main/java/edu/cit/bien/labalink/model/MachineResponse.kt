package edu.cit.bien.labalink.model

data class MachineResponse(
    val id: Long,
    val machineName: String,
    val machineType: String,
    val status: String,
    val qrToken: String,
    val hourlyRate: Double,
    val createdAt: String
)