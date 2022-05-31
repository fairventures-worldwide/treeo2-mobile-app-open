package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "UserInput",
    foreignKeys = [ForeignKey(
        entity = PageEntity::class,
        parentColumns = ["pageId"],
        childColumns = ["pageId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class UserInputEntity(
    @PrimaryKey(autoGenerate = true)
    val inputId: Long?,
    val pageId: Long?,
    val userResponse: String?,
    val description: Map<String, Any>,
    val isMandatory: Boolean = false
)
