package org.treeo.treeo.util.mappers

import org.treeo.treeo.db.models.*
import org.treeo.treeo.models.*
import org.treeo.treeo.models.ActivityConfiguration
import org.treeo.treeo.models.ActivityPlot
import org.treeo.treeo.models.ActivityTemplate
import org.treeo.treeo.models.TemplateConfiguration
import org.treeo.treeo.network.models.TreeSpeciesDTO
import javax.inject.Inject

class ModelEntityMapper @Inject constructor() {

    fun getActivityFromEntity(entity: ActivityEntity): Activity {
        return Activity(
            id = entity.activityId,
            uuid = entity.activityUUID,
            remoteId = entity.activityRemoteId,
            dueDate = entity.dueDate,
            inProgress = entity.inProgress,
            isCompleted = entity.isComplete,
            title = entity.title,
            description = entity.description,
            plot = getActivityPlotFromEntity(entity),
            template = getActivityTemplateFromEntity(entity),
            status = entity.status,
            type = entity.type,
            configuration = getActivityConfigurationFromEntity(entity)
        )
    }

    private fun getActivityTemplateFromEntity(entity: ActivityEntity): ActivityTemplate {
        return ActivityTemplate(
            entity.template?.templateRemoteId,
            entity.template?.activityType,
            entity.template?.code,
            entity.template?.preQuestionnaireId,
            entity.template?.postQuestionnaireId,
            TemplateConfiguration(
                entity.template?.configuration!!.soilPhotoRequired
            )
        )
    }


    private fun getActivityPlotFromEntity(entity: ActivityEntity): ActivityPlot? {
        return entity.plot?.let {
            ActivityPlot(
                it.plotId,
                it.area,
                it.externalId,
                it.polygon,
                it.ownerID,
                it.plotName,
            )
        }
    }


    private fun getActivityConfigurationFromEntity(entity: ActivityEntity): ActivityConfiguration? {
        return  ActivityConfiguration(
                specie_codes = entity.configuration?.specieCodes,
                retryTimes = entity.configuration?.retryTimes
            )

    }


    fun getListOfActivitiesFromEntities(entities: List<ActivityEntity>): List<Activity> {
        return entities.map { getActivityFromEntity(it) }
    }

    private fun getPageFromEntity(entity: PageEntity): Page {
        return Page(
            entity.pageId!!,
            entity.pageType!!,
            entity.questionCode!!,
            entity.header,
            entity.description,
            null,
            entity.mandatory
        )
    }

    fun getListOfPageFromEntities(entities: List<PageEntity>): List<Page> {
        return entities.map { getPageFromEntity(it) }
    }

    private fun getOptionFromEntity(entity: OptionEntity): Option {
        return Option(
            entity.optionId!!,
            entity.pageId!!,
            entity.code!!,
            entity.isSelected,
            entity.title
        )
    }

    fun getListOfOptionsFromEntities(entities: List<OptionEntity>): List<Option> {
        return entities.map { getOptionFromEntity(it) }
    }

    fun getOptionEntityFromModel(option: Option): OptionEntity {
        return OptionEntity(
            option.optionId,
            option.pageId,
            option.code,
            option.isSelected,
            option.title
        )
    }

    // Map land survey entity from land survey
    fun getLandSurveyEntityFromLandSurvey(landSurvey: LandSurvey): LandSurveyEntity {
        return LandSurveyEntity(
            landSurvey.activityId,
            landSurvey.surveyUUID,
            landSurvey.activityUUID,
            landSurvey.sequenceNumber,
            landSurvey.corners,
            landSurvey.isCompleted
        )
    }

    // Map photo entity from photo
    fun getPhotoEntityFromPhoto(photo: Photo): PhotoEntity {
        return PhotoEntity(
            photo.surveyId,
            photo.treeMeasurementId,
            photo.photoUUID,
            photo.measurementUUID,
            photo.imagePath,
            photo.imageType,
            getMetaDataFromPhotoMetaData(photo.metaData)
        )
    }

    private fun getMetaDataFromPhotoMetaData(metaData: PhotoMetaData): MetaData {
        return MetaData(
            metaData.timestamp,
            metaData.resolution,
            metaData.gpsCoordinates,
            metaData.gpsAccuracy,
            metaData.stepsTaken,
            metaData.azimuth,
            metaData.cameraOrientation,
            metaData.flashLight
        )
    }

    fun getListOfPhotoEntitiesFromPhotos(photos: List<Photo>): List<PhotoEntity> {
        return photos.map { getPhotoEntityFromPhoto(it) }
    }

    // Map land survey from land survey entity
    fun getLandSurveyFromLandSurveyEntity(entity: LandSurveyEntity): LandSurvey {
        return LandSurvey(
            entity.surveyId,
            entity.activityId,
            entity.measurementId,
            entity.activityUUID,
            entity.sequenceNumber,
            entity.corners,
            entity.isCompleted
        )
    }

    // Map photo from photo entity
    private fun getPhotoFromPhotoEntity(photo: PhotoEntity): Photo {
        return Photo(
            photo.surveyId,
            photo.treeMeasurementId,
            photo.photoUUID,
            photo.measurementUUID,
            photo.imagePath,
            photo.imageType,
            getPhotoMetaDataFromMetaData(photo.metaData)
        )
    }

    private fun getPhotoMetaDataFromMetaData(metaData: MetaData): PhotoMetaData {
        return PhotoMetaData(
            metaData.timestamp,
            metaData.resolution,
            metaData.gpsCoordinates,
            metaData.gpsAccuracy,
            metaData.stepsTaken,
            metaData.azimuth,
            metaData.cameraOrientation,
            metaData.flashLight
        )
    }

    fun getListOfPhotosFromPhotoEntities(entities: List<PhotoEntity>): List<Photo> {
        return entities.map { getPhotoFromPhotoEntity(it) }
    }

    fun treeMeasurementToEntity(treeMeasurement: TreeMeasurement): TMEntity {
        treeMeasurement.run {
            return TMEntity(
                activityId,
                inventoryId,
                measurementUUID,
                activityUUID,
                numberOfAttempts,
                treeDiameter,
                specie,
                duration,
                measurementType,
                treePolygon,
                cardPolygon,
                carbonDioxide,
                manualDiameter,
                stages,
                treeHealth,
                treeHeightMm,
                comment,
            )
        }
    }

    fun treeSpecieToEntity(treeSpecie: TreeSpeciesDTO.Specie): TreeSpecieEntity = TreeSpecieEntity(
        treeSpecie.id,
        treeSpecie.code,
        treeSpecie.isActive,
        treeSpecie.version,
        treeSpecie.latinName,
        treeSpecie.trivialName,
        treeSpecie.iconURL,
    )
}
