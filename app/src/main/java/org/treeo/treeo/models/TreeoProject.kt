package org.treeo.treeo.models

data class TreeoProject(
    val id: Int,
    val name: String,
    val projectStatus: String,
    val organizationId: Int,
    val organization: Organization
)