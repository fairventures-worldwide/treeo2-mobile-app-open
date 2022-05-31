package org.treeo.treeo.models

import android.os.Parcelable
import androidx.room.TypeConverters
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.treeo.treeo.db.converters.ListConverter
import java.util.*

@Parcelize
class Activity(
    val id: Long,
    var uuid: UUID?,
    val remoteId: Long?,
    val dueDate: String?,
    val inProgress: Boolean,
    val isCompleted: Boolean,
    var title: @RawValue Map<String, Any>,
    val description: @RawValue Map<String, Any>,
    val configuration: ActivityConfiguration?,
    val type: String?,
    val status: String?,
    val plot: ActivityPlot?,
    val template: ActivityTemplate
) : Parcelable

@Parcelize
data class ActivityTemplate(
    val templateRemoteId: Long?,
    var activityType: String?,
    val code: Long?,
    val preQuestionnaireId: Long?,
    val postQuestionnaireId: Long?,
    val configuration: TemplateConfiguration
) : Parcelable

@Parcelize
data class TemplateConfiguration(
    val soilPhotoRequired: Boolean
) : Parcelable


@Parcelize
data class ActivityPlot(
    val id: Long,
    val area: Long?,
    val externalId: String?,
    val polygon: String?,
    val ownerID: Long?,
    val plotName: String?
) : Parcelable


@Parcelize
data class ActivityConfiguration(
    @TypeConverters(ListConverter::class)
    val specie_codes: List<String>?,
    val retryTimes: Int?,
) : Parcelable
