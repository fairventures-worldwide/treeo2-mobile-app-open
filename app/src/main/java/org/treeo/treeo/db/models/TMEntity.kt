package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class TMEntity(
    val activityId: Long?,
    val forestInventoryId: Long?,
    val measurementUUID: UUID,
    val activityUUID: UUID?,
    val numberOfAttempts: Int,
    val treeDiameter: Double?,
    val specie: String?,
    val duration: Long?,
    val measurement_type: String?,
    val treePolygon: String?,
    val cardPolygon: String?,
    val carbonDioxide: String?,
    val manualDiameter: String?,
    val stages: String?,
    val treeHealth: String?,
    val treeHeightMm: Int?,
    val comment: String?,
) {
    @PrimaryKey(autoGenerate = true)
    var treeMeasurementId: Long = 0
}
