package org.treeo.treeo.models

data class FacebookUser(
    val email: String,
    val firstName: String,
    val lastName: String,
    val statusCode: Int,
    val token: String,
    val username: Any
)
