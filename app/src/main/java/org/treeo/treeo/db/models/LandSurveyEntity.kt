package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "LandSurvey",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["activityId"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LandSurveyEntity(
    val activityId: Long,
    val measurementId: UUID,
    val activityUUID: UUID?,
    val sequenceNumber: Int,
    val corners: Int,
    val isCompleted: Boolean,
) {
    @PrimaryKey(autoGenerate = true)
    var surveyId: Long = 0
}

