package org.treeo.treeo.network.models

import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import org.treeo.treeo.db.converters.ListConverter

data class ActivityDTO(
    @SerializedName("id")
    val id: Long,
    @SerializedName("dueDate")
    val dueDate: String,
    @SerializedName("completedByActivity")
    val completedByActivity: Boolean,
    @SerializedName("title")
    val title: Map<String, String>,
    @SerializedName("description")
    val description: Map<String, String>,
    @SerializedName("configuration")
    val configuration: ActivityConfigurationDTO,
    @SerializedName("type")
    val type: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("plot")
    val plot: ActivityPlotDTO,
    @SerializedName("activityTemplate")
    val activityTemplate: ActivityTemplateDTO,
)

data class ActivityConfigurationDTO(
    @SerializedName("specie_codes")
    @TypeConverters(ListConverter::class)
    val specieCodes: List<String>?,
    val manualDBH: String?,
    val manualHeight: String?,
    val treeHealth: String?,
    val measurementComment: String?,
)

data class ActivityPlotDTO(
    @SerializedName("id")
    val id: Long,
    @SerializedName("area")
    val area: Long? = null,
    @SerializedName("externalId")
    val externalId: String? = null,
    @SerializedName("ownerID")
    val ownerID: Long? = null,
    @SerializedName("status")
    val status: String?,
    @SerializedName("plotName")
    val plotName: String? = null,
)

data class ActivityTemplateDTO(
    @SerializedName("id")
    val id: Long,
    @SerializedName("activityType")
    val activityType: String,
    @SerializedName("code")
    val code: Long,
    @SerializedName("pre_questionnaireID")
    val preQuestionnaireId: Long,
    @SerializedName("post_questionnaireID")
    val postQuestionnaireId: Long,
    @SerializedName("projectID")
    val projectId: Long,
    @SerializedName("questionnaire")
    val questionnaire: List<QuestionnaireDTO>
)

data class TemplateConfigurationDTO(
    @SerializedName("soilPhotoRequired")
    val soilPhotoRequired: Boolean,
    val measurementType: String?,
)

data class QuestionnaireDTO(
    @SerializedName("id")
    val id: Long,
    @SerializedName("projectID")
    val projectId: Long,
    @SerializedName("type")
    val type: String,
    @SerializedName("configuration")
    val configuration: ConfigurationDTO
)

data class ConfigurationDTO(
    @SerializedName("pages")
    val pages: List<PageDTO>,
    @SerializedName("title")
    val title: Map<String, String>
)

data class PageDTO(
    @SerializedName("header")
    val header: Map<String, String>,
    @SerializedName("options")
    val options: List<OptionDTO>,
    @SerializedName("pageType")
    val pageType: String,
    @SerializedName("description")
    val description: Map<String, String>,
    @SerializedName("questionCode")
    val questionCode: String,
    val mandatory: Boolean?
)

data class OptionDTO(
    @SerializedName("code")
    val code: String,
    @SerializedName("title")
    val title: Map<String, String>
)
