package org.treeo.treeo.models

data class RegisteredMobileUser(
    val email: Any,
    val firstName: String,
    val id: Int,
    val isActive: Boolean,
    val lastName: String
)
