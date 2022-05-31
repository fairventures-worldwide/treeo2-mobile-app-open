package org.treeo.treeo.models

data class LoginWithOTP(
    val phoneNumber: String,
    val code: String
)
