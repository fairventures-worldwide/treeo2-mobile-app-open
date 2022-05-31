package org.treeo.treeo.models

data class ValidateOTPRegistrationResponse(
    val token: String,
    val userId: Int,
    val username: String,
    val refreshToken: String,
)