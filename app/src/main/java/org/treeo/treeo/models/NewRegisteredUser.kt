package org.treeo.treeo.models

data class NewRegisteredUser(
    val email: String,
    val firstName: String,
    val isActive: Boolean,
    val lastName: String
)
