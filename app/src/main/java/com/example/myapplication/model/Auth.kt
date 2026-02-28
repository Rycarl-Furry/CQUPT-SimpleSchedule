package com.example.myapplication.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val success: Boolean,
    val ticket: String
)
