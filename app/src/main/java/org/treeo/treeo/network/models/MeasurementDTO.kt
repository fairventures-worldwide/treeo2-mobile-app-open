package org.treeo.treeo.network.models

import com.google.gson.annotations.SerializedName

data class MeasurementDTO(
    val id: String?,
    val activityID: String?,
    val dateTime: String?,
    val treeDBHmm: Any?,
    val treeHealth: String?,
    val treeHeightMm: Int?,
    val stepsSinceLastMeasurement: Int?,

    @SerializedName("measurement_type")
    val measurementType: String?,
    val gpsLocation: String?,
    val gpsAccuracy: Float?,
    val additionalData: String?
)
