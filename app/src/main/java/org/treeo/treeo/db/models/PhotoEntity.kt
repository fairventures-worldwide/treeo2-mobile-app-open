package org.treeo.treeo.db.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.treeo.treeo.models.UserLocation
import java.util.*

@Entity(
    tableName = "Photos",
    foreignKeys = [ForeignKey(
        entity = LandSurveyEntity::class,
        parentColumns = ["surveyId"],
        childColumns = ["surveyId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = TMEntity::class,
        parentColumns = ["treeMeasurementId"],
        childColumns = ["treeMeasurementId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PhotoEntity(
    val surveyId: Long?,
    val treeMeasurementId: Long?,
    val photoUUID: UUID,
    val measurementUUID: UUID?,
    val imagePath: String,
    val imageType: String?,
    @Embedded val metaData: MetaData
) {
    @PrimaryKey(autoGenerate = true)
    var photoId: Long = 0
}

data class MetaData(
    val timestamp: String?,
    val resolution: String?,
    var gpsCoordinates: UserLocation?,
    val gpsAccuracy: Float,
    val stepsTaken: String?,
    val azimuth: String?,
    val cameraOrientation: String?,
    val flashLight: Boolean
)
