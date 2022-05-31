package org.treeo.treeo.db.models.mappers

import org.treeo.treeo.db.models.*
import org.treeo.treeo.network.models.*
import org.treeo.treeo.util.BIG_AND_SMALL_TREES
import org.treeo.treeo.util.EntityMapper
import java.util.*
import javax.inject.Inject

class ActivityDtoToEntityMapper @Inject constructor() :
    EntityMapper<ActivityDTO, ActivityEntity> {
    override fun toEntity(obj: ActivityDTO): ActivityEntity {
        return ActivityEntity(
            activityUUID = UUID.randomUUID(),
            activityRemoteId = obj.id,
            dueDate = obj.dueDate,
            inProgress = false,
            isComplete = obj.completedByActivity,
            title = obj.title,
            description = obj.description,
            configuration = getActivityConfiguration(obj.configuration),
            type = obj.type,
            status = obj.status,
            plot = getActivityPlot(obj.plot),
            startDate = null,
            endDate = null,
            syncDate = null,
            measurementCount = 1,
            template = getActivityTemplate(obj.activityTemplate),
        )
    }

    override fun fromEntity(obj: ActivityEntity): ActivityDTO {
        TODO("Not yet implemented")
    }

    private fun getActivityTemplate(dto: ActivityTemplateDTO): ActivityTemplate {
        return ActivityTemplate(
            dto.id,
            dto.activityType,
            dto.code,
            dto.preQuestionnaireId,
            dto.postQuestionnaireId,
            TemplateConfiguration(
                false,
                BIG_AND_SMALL_TREES
            )
        )
    }

    private fun getActivityPlot(dto: ActivityPlotDTO?): ActivityPlot? {
        return if (dto == null)
            null
        else ActivityPlot(
            dto.id,
            dto.area,
            dto.externalId,
            null,
            dto.ownerID,
            dto.plotName,
        )
    }

    private fun getActivityConfiguration(dto: ActivityConfigurationDTO?): ActivityConfiguration? {
        return if (dto == null)
            null
        else ActivityConfiguration(
            dto.specieCodes,
            dto.manualDBH,
            dto.manualHeight,
            dto.treeHealth,
            dto.measurementComment,
        )
    }

    fun getQuestionnaireEntity(activityId: Long, dto: QuestionnaireDTO): QuestionnaireEntity {
        return QuestionnaireEntity(
            activityId,
            dto.id,
            dto.projectId,
            dto.type
        )
    }

    fun getPageEntity(questionnaireId: Long, dto: PageDTO): PageEntity {
        return PageEntity(
            null,
            questionnaireId,
            dto.pageType,
            dto.questionCode,
            dto.header,
            dto.description,
            dto.mandatory ?: true
        )
    }

    fun getOptionEntity(pageId: Long, dto: OptionDTO): OptionEntity {
        return OptionEntity(
            null,
            pageId,
            dto.code,
            false,
            dto.title
        )
    }

    fun getUserInputEntity(pageId: Long, dto: PageDTO): UserInputEntity {
        return UserInputEntity(
            null,
            pageId,
            null,
            dto.description
        )
    }
}
