package org.treeo.treeo.models

data class RegisterMobileUser(
    var firstName: String = "",
    var lastName: String = "",
    var country: String = "",
    var phoneNumber: String = "",
    var password: String? = "secret1234",
    var username: String? = null,
    var gdprAccepted: Boolean? = false
)
