package org.treeo.treeo.repositories

import com.google.gson.Gson
import kotlinx.coroutines.delay
import org.treeo.treeo.BuildConfig
import org.treeo.treeo.db.dao.ActivityDao
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.ForestInventoryEntity
import org.treeo.treeo.db.models.PhotoEntity
import org.treeo.treeo.db.models.TMEntity
import org.treeo.treeo.db.models.UploadQueueEntity
import org.treeo.treeo.models.TreeMeasurement
import org.treeo.treeo.network.RequestManager
import org.treeo.treeo.network.models.ActivityUploadDTO
import org.treeo.treeo.network.models.BinaryDataDTO
import org.treeo.treeo.network.models.MeasurementDTO
import org.treeo.treeo.util.ACTIVITY_FOREST_INVENTORY
import org.treeo.treeo.util.SUB_ACTIVITY_TYPE_TREE_MEASUREMENT
import org.treeo.treeo.util.createAdhocActivity
import org.treeo.treeo.util.mappers.ModelEntityMapper
import java.io.File
import java.sql.Timestamp
import javax.inject.Inject

class ForestInventoryRepository @Inject constructor(
    private val tmDao: TreeMeasurementDao,
    private val modelEntityMapper: ModelEntityMapper,
    private val activityDao: ActivityDao,
    private val requestManager: RequestManager,
) {

    suspend fun createForestInventory(activityId: Long?): Long {
        var activityId = activityId
        val activity = createAdhocActivity(ACTIVITY_FOREST_INVENTORY)
        if (activityId == null) {
            activityId = tmDao.createAdHocTreeMeasurementActivity(activity)
        }
        return tmDao.insertForestInventory(
            ForestInventoryEntity(
                activityId,
                ForestInventoryEntity.STATUS_STARTED
            )
        )
    }

    suspend fun createForestInventoryByActivityId(id: Long): Long {
        return tmDao.insertForestInventory(
            ForestInventoryEntity(
                id,
                ForestInventoryEntity.STATUS_STARTED
            )
        )
    }

    suspend fun deleteForestInventoryAndStartNewOne(inventoryId: Long): Long {
        tmDao.run {
            deleteInventoryMeasurements(inventoryId)
            deleteIncompleteInventory(inventoryId)
        }

        val activity = createAdhocActivity(ACTIVITY_FOREST_INVENTORY)
        val activityId = tmDao.createAdHocTreeMeasurementActivity(activity)
        return tmDao.insertForestInventory(
            ForestInventoryEntity(
                activityId,
                ForestInventoryEntity.STATUS_STARTED
            )
        )
    }

    suspend fun deleteForestInventoryAndStartNewOneByActivityId(inventoryId: Long): Long {
        tmDao.run {
            deleteInventoryMeasurements(inventoryId)
        }
        return inventoryId
    }

    suspend fun addForestTreeMeasurement(treeMeasurement: TreeMeasurement): Long {
        return tmDao.insertTreeMeasurement(modelEntityMapper.treeMeasurementToEntity(treeMeasurement))
    }

    suspend fun getIncompleteForestInventoryByActivityId(id: Long): ForestInventoryEntity? {
        return tmDao.getIncompleteForestInventoryByActivityId(id)
    }

    suspend fun getIncompleteForestInventory(): ForestInventoryEntity? {
        return tmDao.getIncompleteForestInventory()
    }

    suspend fun markForestInventoryAsComplete(inventoryId: Long) {
        tmDao.updateForestInventoryStatus(inventoryId, ForestInventoryEntity.STATUS_FINISHED)
        addForestInventoryToUploadQueue(inventoryId)
    }

    suspend fun getLatestForestInventoryActivity(): ForestInventoryEntity? {
        return tmDao.getLatestForestInventory()
    }

    suspend fun getInventoryMeasurements(inventoryId: Long): List<TMEntity> {
        return tmDao.getTreeMeasurements(inventoryId)
    }

    suspend fun getForestInventory(inventoryId: Long): ForestInventoryEntity {
        return tmDao.getForestInventory(inventoryId)
    }

    private suspend fun addForestInventoryToUploadQueue(inventoryId: Long) {
        val inventory = tmDao.getForestInventory(inventoryId)
        val activityId = inventory.activityId
        activityDao.markActivityAsCompleted(activityId)

        val measurementCount = tmDao.countTreeMeasurementsForActivity(activityId)
        activityDao.addActivityMeasurementCount(activityId, measurementCount)

        activityDao.setActivityEndDate(
            activityId,
            Timestamp(System.currentTimeMillis()).toString()
        )
        activityDao.markActivityNotInProgress(activityId)

        val count = tmDao.getTreeMeasurementActivityCount(activityId)
        activityDao.addActivityMeasurementCount(activityId, count)

        // 1. create activity
        val activity = activityDao.getTreeMonitoring(activityId)
        if (activity.isNotEmpty()) {
            val activityData = Gson().toJson(
                buildTreeMeasurementDataUpload(activityId)
            )
            addDataToUploadQueue(
                UploadQueueEntity(
                    activityId,
                    activityData,
                    activityData.length,
                    ACTIVITY_FOREST_INVENTORY,
                    true
                )
            )
        }
        delay(2000L)

        // 2. upload measurements and binary files
        val treeMeasurementData = tmDao.getTreeMeasurementWithPhotosByActivity(activityId)
        if (treeMeasurementData.isNotEmpty()) {
            treeMeasurementData.forEach {
                val measurement = it?.let { tmWithPhotos ->
                    buildTreeMeasurementUpload(
                        tmWithPhotos.treeMeasurement,
                        tmWithPhotos.photos[0]
                    )
                }
                val activityDataMeasurement = Gson().toJson(measurement)
                addDataToUploadQueue(
                    UploadQueueEntity(
                        activityId,
                        activityDataMeasurement,
                        activityDataMeasurement.length,
                        SUB_ACTIVITY_TYPE_TREE_MEASUREMENT,
                        true
                    )
                )

                val binaryData = it?.photos?.get(0)
                    ?.let { photoEntity ->
                        buildBinaryDataUpload(
                            photoEntity.imagePath,
                            photoEntity.photoUUID.toString()
                        )
                    }
                if (it != null) {
                    addDataToUploadQueue(
                        UploadQueueEntity(
                            activityId,
                            Gson().toJson(binaryData),
                            File(it.photos[0].imagePath).length().toInt(),
                            "binary_upload",
                            true
                        )
                    )
                }
            }
        }
    }

    private suspend fun buildTreeMeasurementDataUpload(
        activityId: Long
    ): ActivityUploadDTO {
        val activity = activityDao.getActivity(activityId)
        return ActivityUploadDTO(
            id = activity.activityUUID.toString(),
            activityTemplateID = activity.template?.templateRemoteId,
            userID = null,
            plotID = null,
            startDate = activity.startDate,
            endDate = activity.endDate,
            synced = "",
            restarted = 0,
            mobileAppVersion = BuildConfig.VERSION_NAME,
            outsidePolygon = listOf(),
            fullyCompleted = activity.isComplete,
            labels = listOf(),
            comment = "",
            commentAudio = "",
            totalSteps = 0,
            preQuestionnaireID = 0,
            preQuestionnaireData = null,
            postQuestionnaireID = 0,
            postQuestionnaireData = null,
            deviceInformationID = null,
            measurementCount = activity.measurementCount,
        )
    }

    private suspend fun addDataToUploadQueue(upload: UploadQueueEntity) {
        activityDao.addActivityToUploadQueue(upload)
    }

    private fun buildTreeMeasurementUpload(
        treeMeasurement: TMEntity,
        photo: PhotoEntity
    ): MeasurementDTO {
        return MeasurementDTO(
            id = photo.photoUUID.toString(),
            activityID = treeMeasurement.activityUUID.toString(),
            dateTime = photo.metaData.timestamp,
            treeDBHmm = treeMeasurement.treeDiameter?.toInt(),
            treeHealth = treeMeasurement.treeHealth ?: "N/A",
            treeHeightMm = treeMeasurement.treeHeightMm ?: 0,
            stepsSinceLastMeasurement = 0,
            measurementType = treeMeasurement.measurement_type,
            gpsLocation = "${photo.metaData.gpsCoordinates?.lat} ,${photo.metaData.gpsCoordinates?.lng}",
            gpsAccuracy = photo.metaData.gpsAccuracy,
            additionalData = Gson().toJson(
                mapOf(
                    "attempts" to treeMeasurement.numberOfAttempts,
                    "species" to treeMeasurement.specie,
                    "durationInMs" to treeMeasurement.duration,
                    "treePolygon" to treeMeasurement.treePolygon,
                    "cardPolygon" to treeMeasurement.cardPolygon,
                    "carbonDioxide" to treeMeasurement.carbonDioxide,
                    "manualDiameter" to treeMeasurement.manualDiameter,
                    "stages" to treeMeasurement.stages,
                    "comment" to treeMeasurement.comment,
                )
            )
        )
    }

    private fun buildBinaryDataUpload(
        imagePath: String,
        measurementId: String
    ): BinaryDataDTO {
        return BinaryDataDTO(imagePath, measurementId)
    }

    suspend fun syncTreeSpecies() {
        val species = requestManager.getTreeSpecies()
        if (species.isNotEmpty()) {
            for (specie in species) {
                tmDao.insertTreeSpecie(modelEntityMapper.treeSpecieToEntity(specie))
            }
        }
    }

    suspend fun getTreeSpecies() = tmDao.getTreeSpecies()
}
