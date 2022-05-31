package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class LandSurvey(
    val surveyId: Long? = null,
    val activityId: Long,
    val surveyUUID: UUID,
    val activityUUID: UUID?,
    val sequenceNumber: Int,
    val corners: Int,
    val isCompleted: Boolean
) : Parcelable
