package org.treeo.treeo.network.models

data class TreeSpeciesDTO(
    val count: Int,
    val rows: List<Specie>,
) {
    data class Specie(
        val id: Int,
        val code: String,
        val isActive: Boolean,
        val version: Double,
        val latinName: String,
        val trivialName: Map<String, Any>,
        val iconURL: String,
    )
}
