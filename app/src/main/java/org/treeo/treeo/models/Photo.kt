package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Photo(
    var surveyId: Long?,
    var treeMeasurementId: Long?,
    val photoUUID: UUID,
    val measurementUUID: UUID?,
    var imagePath: String,
    val imageType: String?,
    val metaData: PhotoMetaData
) : Parcelable

@Parcelize
data class PhotoMetaData(
    val timestamp: String?,
    val resolution: String?,
    var gpsCoordinates: UserLocation?,
    val gpsAccuracy: Float,
    val stepsTaken: String?,
    val azimuth: String?,
    val cameraOrientation: String?,
    val flashLight: Boolean
) : Parcelable
