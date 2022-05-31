package org.treeo.treeo.models

data class RegisterUser(
    var firstName: String="",
    var lastName: String="",
    var password: String="",
    var email: String="",
    var country: String="",
    var username: String="",
    var phoneNumber: String=""
)
