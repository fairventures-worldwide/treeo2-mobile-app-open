package org.treeo.treeo.db.models

import androidx.room.*
import org.treeo.treeo.db.converters.ListConverter
import java.util.*

@Entity(tableName = "Activity")
data class ActivityEntity(
    val activityUUID: UUID?,
    val activityRemoteId: Long?,
    val dueDate: String?,
    val inProgress: Boolean = false,
    val isComplete: Boolean = false,
    val title: Map<String, Any>,
    val description: Map<String, Any>,
    val type: String?,
    val status: String?,
    val isAdhoc: Boolean = false,
    val startDate: String?,
    val endDate: String?,
    val syncDate: String?,
    val measurementCount: Int?,
    @Embedded val configuration: ActivityConfiguration?,
    @Embedded val template: ActivityTemplate,
    @Embedded val plot: ActivityPlot?,
) {
    @PrimaryKey(autoGenerate = true)
    var activityId: Long = 0
}

data class ActivityTemplate(
    val templateRemoteId: Long?,
    val activityType: String?,
    val code: Long?,
    val preQuestionnaireId: Long?,
    val postQuestionnaireId: Long?,
    @Embedded val configuration: TemplateConfiguration?
)

data class TemplateConfiguration(
    val soilPhotoRequired: Boolean,
    val measurementType: String?
)

data class ActivityPlot(
    val plotId: Long,
    val area: Long?,
    val externalId: String?,
    val polygon: String?,
    val ownerID: Long?,
    val plotName: String?
)

data class ActivityConfiguration(
    @field:TypeConverters(ListConverter::class)
    val specieCodes: List<String>? = listOf(),
    val manualDBH: String?,
    val manualHeight: String?,
    val treeHealth: String?,
    val measurementComment: String?,
    val retryTimes: Int = 3,
)
