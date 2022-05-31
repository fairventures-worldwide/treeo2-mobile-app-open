package org.treeo.treeo.models

data class ValidateResponseData(
    val phoneNumber: String,
    val valid: Boolean,
    val errorStatus: String,
)