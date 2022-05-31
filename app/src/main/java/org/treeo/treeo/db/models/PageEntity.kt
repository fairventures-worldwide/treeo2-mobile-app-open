package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Page",
    foreignKeys = [ForeignKey(
        entity = QuestionnaireEntity::class,
        parentColumns = ["questionnaireId"],
        childColumns = ["questionnaireId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val pageId: Long?,
    val questionnaireId: Long?,
    val pageType: String?,
    val questionCode: String?,
    val header: Map<String, Any>,
    val description: Map<String, Any>,
    val mandatory: Boolean
)

