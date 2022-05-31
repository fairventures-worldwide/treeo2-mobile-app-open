package org.treeo.treeo.models

data class SmsLoginResponse(
    val email: String,
    val token: String,
    val userId: Int,
    val username: String,
    val refreshToken: String,
)
