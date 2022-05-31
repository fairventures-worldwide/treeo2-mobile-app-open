package org.treeo.treeo.models

import java.util.*

data class TreeMeasurement(
    val measurementId: Long?,
    val measurementUUID: UUID,
    val activityId: Long?,
    val inventoryId: Long?,
    val activityUUID: UUID?,
    val numberOfAttempts: Int,
    val treeDiameter: Double?,
    var specie: String?,
    var duration: Long?,
    var measurementType: String?,
    val treePolygon: String?,
    val cardPolygon: String?,
    var carbonDioxide: String?,
    var manualDiameter: String?,
    var stages: String?,
    var treeHealth: String?,
    var treeHeightMm: Int?,
    var comment: String?,
)
