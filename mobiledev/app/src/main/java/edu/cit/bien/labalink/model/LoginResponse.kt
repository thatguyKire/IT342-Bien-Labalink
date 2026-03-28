package edu.cit.bien.labalink.model

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val role: String,
    val email: String,
    val username: String
)