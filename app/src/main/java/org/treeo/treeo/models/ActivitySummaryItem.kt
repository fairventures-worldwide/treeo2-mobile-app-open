package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
open class ActivitySummaryItem : Parcelable {
    var activity: Activity? = null
}

@Parcelize
data class QuestionnaireSummaryItem(
    val questionnaireId: Long,
    val isCompleted: Boolean,
    val type: String,
    val pages: List<Page>
) : Parcelable, ActivitySummaryItem()


@Parcelize
data class LandSurveySummaryItem(
    val photos: List<Photo>,
    val landSurvey: LandSurvey
) : Parcelable, ActivitySummaryItem()

