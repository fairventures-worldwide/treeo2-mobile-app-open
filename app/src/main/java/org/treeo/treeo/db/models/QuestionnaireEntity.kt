package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Questionnaire",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["activityId"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class QuestionnaireEntity(
    var activityId: Long,
    val questionnaireRemoteId: Long,
    val projectId: Long,
    val type: String,
    val isCompleted: Boolean = false,
) {
    @PrimaryKey(autoGenerate = true)
    var questionnaireId: Long = 0
}

