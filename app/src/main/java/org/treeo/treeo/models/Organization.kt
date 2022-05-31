package org.treeo.treeo.models

data class Organization(
    val id: Int,
    val name: String,
    val country: String,
    val code: String,
    val status: String,
    val activeFrom: String,
    val activeTo: String)