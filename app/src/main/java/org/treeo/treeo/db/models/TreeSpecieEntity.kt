package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TreeSpecieEntity(

    @PrimaryKey
    val id: Int,

    val code: String,
    val isActive: Boolean,
    val version: Double,
    val latinName: String,
    val trivialName: Map<String, Any>,
    val iconURL: String,
)
