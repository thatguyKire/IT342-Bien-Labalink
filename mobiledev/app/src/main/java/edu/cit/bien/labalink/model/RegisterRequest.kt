package edu.cit.bien.labalink.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)