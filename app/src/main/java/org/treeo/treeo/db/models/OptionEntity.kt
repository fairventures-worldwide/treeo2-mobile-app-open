package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Option",
    foreignKeys = [ForeignKey(
        entity = PageEntity::class,
        parentColumns = ["pageId"],
        childColumns = ["pageId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class OptionEntity(
    @PrimaryKey(autoGenerate = true)
    val optionId: Long?,
    val pageId: Long?,
    val code: String?,
    val isSelected: Boolean = false,
    val title: Map<String, Any>
)

