package org.treeo.treeo.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.treeo.treeo.BuildConfig
import org.treeo.treeo.db.dao.ActivityDao
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.*
import org.treeo.treeo.db.models.mappers.ActivityDtoToEntityMapper
import org.treeo.treeo.db.models.relations.PageWithOptions
import org.treeo.treeo.db.models.relations.QuestionnaireWithPages
import org.treeo.treeo.models.LandSurvey
import org.treeo.treeo.models.Photo
import org.treeo.treeo.models.TreeMeasurement
import org.treeo.treeo.network.models.*
import org.treeo.treeo.network.workers.UploadQueueWorker
import org.treeo.treeo.util.*
import org.treeo.treeo.util.mappers.ModelEntityMapper
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class DBMainRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val landSurveyDao: LandSurveyDao,
    private val tmDao: TreeMeasurementDao,
    private val mapper: ActivityDtoToEntityMapper,
    private val context: Context,
    private val modelEntityMapper: ModelEntityMapper,
    private val preferences: SharedPreferences,
    private val dispatcher: IDispatcherProvider
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    suspend fun insertOrUpdateActivities(activities: List<ActivityDTO>) {
        activities.forEach {
            val localActivityCopy = activityDao.getActivityEntityByRemoteId(it.id)
            val activityEntity = mapper.toEntity(it)
            val activityId = if (localActivityCopy == null) {
                activityDao.insertActivity(activityEntity)
            } else {
                // Assign primary key for a successful update
                activityEntity.activityId = localActivityCopy.activityId

                activityDao.updateActivity(activityEntity)
                localActivityCopy.activityId
            }
            insertOrUpdateQuestionnaire(activityId, it.activityTemplate.questionnaire)
        }
    }

    private suspend fun insertOrUpdateQuestionnaire(
        activityId: Long,
        questionnaires: List<QuestionnaireDTO>
    ) {
        questionnaires.forEach {
            val entity = mapper.getQuestionnaireEntity(activityId, it)
            val questionnaireId = activityDao.insertOrUpdateQuestionnaire(entity)
            insertOrUpdatePage(questionnaireId, it.configuration.pages)
        }
    }

    private suspend fun insertOrUpdatePage(
        questionnaireId: Long,
        pages: List<PageDTO>
    ) {
        pages.forEach {
            val entity = mapper.getPageEntity(questionnaireId, it)
            val pageId = activityDao.insertOrUpdatePage(entity)
            when (it.pageType) {
                PAGE_TYPE_SHORT_TEXT, PAGE_TYPE_LONG_TEXT -> {
                    insertOrUpdateUserInput(pageId, it)
                }
                else -> insertOrUpdateOption(pageId, it.options)
            }
        }
    }

    private suspend fun insertOrUpdateUserInput(pageId: Long, pageDTO: PageDTO) {
        val entity = mapper.getUserInputEntity(pageId, pageDTO)
        activityDao.insertOrUpdateUserInput(entity)
    }

    private suspend fun insertOrUpdateOption(pageId: Long, options: List<OptionDTO>) {
        options.forEach {
            val entity = mapper.getOptionEntity(pageId, it)
            activityDao.insertOrUpdateOption(entity)
        }
    }

    fun getPlannedActivities(type: String): Flow<List<ActivityEntity>> {
        if (type == "adhoc") {
            return activityDao.getAdhocActivities(type)
        }
        return activityDao.getPlannedActivities(type)
    }

    fun getAllPlannedActivities(): Flow<List<ActivityEntity>> {
        return activityDao.getAllPlannedActivities()
    }

    suspend fun getActivity(activityId: Long): ActivityEntity {
        return activityDao.getActivity(activityId)
    }

    suspend fun getPageOptions(pageId: Long): List<OptionEntity> {
        return activityDao.getPageOptions(pageId)
    }

    suspend fun updateOption(id: Long, isSelected: Boolean) {
        activityDao.updateOption(id, isSelected)
    }

    suspend fun updateUserInput(pageId: Long, response: String) {
        activityDao.updateUserInput(pageId, response)
    }

    suspend fun getSelectedOptions(pageId: Long): List<OptionEntity> {
        return activityDao.getSelectedOptions(pageId)
    }

    fun getAllActivities(): Flow<List<ActivityEntity>> {
        return activityDao.getActivities()
    }

    suspend fun getCompletedActivities(): List<ActivityEntity> {
        return activityDao.getCompletedActivities()
    }

    suspend fun markActivityAsCompleted(activityId: Long, measurementCount: Int) {
        activityDao.markActivityAsCompleted(activityId)
        activityDao.addActivityMeasurementCount(activityId, measurementCount)
        activityDao.setActivityEndDate(
            activityId,
            Timestamp(System.currentTimeMillis()).toString()
        )
        activityDao.markActivityNotInProgress(activityId)
        addActivityDataToQueue(activityId)
    }

    private suspend fun addDataToUploadQueue(upload: UploadQueueEntity) {
        activityDao.addActivityToUploadQueue(upload)
    }

    suspend fun setActivityStartDate(activityId: Long) {
        activityDao.setActivityStartDate(
            activityId,
            Timestamp(System.currentTimeMillis()).toString()
        )
    }

    suspend fun getQuestionnaireWithPages(activityId: Long): List<QuestionnaireWithPages> {
        return activityDao.getQuestionnaireWithPages(activityId)
    }

    suspend fun getPageWithOptions(questionnaireId: Long): PageWithOptions {
        return activityDao.getPageWithOptions(questionnaireId)
    }

    suspend fun getQuestionnaire(activityId: Long): QuestionnaireEntity {
        return activityDao.getQuestionnaire(activityId)
    }

    suspend fun getQuestionnaires(activityId: Long): List<QuestionnaireEntity> {
        return activityDao.getQuestionnaires(activityId)
    }

    suspend fun getIncompleteActivities(type: String, activityType: String): List<ActivityEntity> {
        return activityDao.getIncompleteAdhocActivities(type = type, activityType = activityType)
    }

    suspend fun getActivitiesByType(type: String, activityType: String): List<ActivityEntity> {
        return activityDao.getActivitiesByType(type = type, activityType = activityType)
    }

    private suspend fun addActivityDataToQueue(activityId: Long) {
        // 1. check for, find activity questionnaire data and add to queue
        val questionnaires = activityDao.getQuestionnaires(activityId)
        if (questionnaires.isNotEmpty()) {
            questionnaires.forEach {
                val pages = activityDao.getQuestionnairePages(it.questionnaireId)
                val questionnaireDataMap = Gson().toJson(getQuestionnaireDataMap(pages))
                val activityData =
                    Gson().toJson(buildQuestionnaireUpload(activityId, questionnaireDataMap))

                addDataToUploadQueue(
                    UploadQueueEntity(
                        activityId,
                        activityData,
                        activityData.length,
                        ACTIVITY_TYPE_QUESTIONNAIRE,
                        true
                    )
                )
            }
        }

        // 2. check for, find activity land survey data and add to queue
        val landSurveyWithPhotos = landSurveyDao.getLandSurveyWithPhotosByActivity(activityId)
        landSurveyWithPhotos?.photos?.forEach {
            val measurement = buildLandSurveyMeasurementUpload(
                landSurveyWithPhotos.landSurvey,
                it
            )
            val activityData = Gson().toJson(measurement)

            addDataToUploadQueue(
                UploadQueueEntity(
                    activityId,
                    activityData,
                    activityData.length,
                    ACTIVITY_TYPE_LAND_SURVEY,
                    true
                )
            )
        }

        // 3. check for, find activity binary data and add to queue
        landSurveyWithPhotos?.photos?.forEach {
            val binaryData = buildBinaryDataDTO(it.imagePath, it.photoUUID.toString())
            addDataToUploadQueue(
                UploadQueueEntity(
                    activityId,
                    Gson().toJson(binaryData),
                    File(it.imagePath).length().toInt(),
                    "binary_upload",
                    true
                )
            )
        }

        // 4. Add tree monitoring activity with measurements and photos
        val treeMonitoring = activityDao.getTreeMonitoring(activityId)
        if (treeMonitoring.isNotEmpty()) {
            val activityData = Gson().toJson(
                buildTreeMonitoringActivityDTO(activityId)
            )
            addDataToUploadQueue(
                UploadQueueEntity(
                    activityId,
                    activityData,
                    activityData.length,
                    ACTIVITY_TYPE_TREE_MONITORING,
                    true
                )
            )

            val treeMeasurementData = tmDao.getTreeMeasurementWithPhotosByActivity(activityId)
            treeMeasurementData.forEach {
                if (it != null) {
                    // Build and save tree measurement to upload queue
                    val measurement = buildTreeMeasurementDTO(
                        it.treeMeasurement,
                        it.photos[0]
                    )
                    val uploadQueueData = Gson().toJson(measurement)
                    addDataToUploadQueue(
                        UploadQueueEntity(
                            activityId,
                            uploadQueueData,
                            uploadQueueData.length,
                            SUB_ACTIVITY_TYPE_TREE_MEASUREMENT,
                            true
                        )
                    )

                    // Build and save tree photo to upload queue
                    val binaryData = buildBinaryDataDTO(
                        it.photos[0].imagePath,
                        it.photos[0].photoUUID.toString()
                    )
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

    private suspend fun buildQuestionnaireUpload(
        activityId: Long,
        questionnaireDataMap: String
    ): ActivityUploadDTO {
        val activity = activityDao.getActivity(activityId)
        return ActivityUploadDTO(
            id = activity.activityUUID.toString(),
            activityTemplateID = activity.template.templateRemoteId,
            userID = null,
            plotID = activity.plot?.plotId,
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
            preQuestionnaireData = questionnaireDataMap,
            postQuestionnaireID = 0,
            postQuestionnaireData = null,
            deviceInformationID = null,
            measurementCount = activity.measurementCount,
        )
    }

    private suspend fun buildTreeMonitoringActivityDTO(
        activityId: Long
    ): ActivityUploadDTO {
        val activity = activityDao.getActivity(activityId)
        return ActivityUploadDTO(
            id = activity.activityUUID.toString(),
            activityTemplateID = activity.template.templateRemoteId,
            userID = null,
            plotID = activity.plot?.plotId,
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

    private suspend fun getQuestionnaireDataMap(pages: List<PageEntity>): Map<String, String> {
        val dataMap = mutableMapOf<String, String>()
        pages.forEach {
            val options = activityDao.getPageAnswers(it.pageId!!)
            val userInputs = activityDao.getUserInputs(it.pageId)
            val checkedOptions = options.joinToString { string -> string.code!! }
            val responses = userInputs.joinToString { userInput ->
                userInput.userResponse ?: ""
            }
            dataMap[it.questionCode!!] = checkedOptions + responses
        }

        return dataMap
    }

    private fun buildLandSurveyMeasurementUpload(
        landSurvey: LandSurveyEntity,
        photo: PhotoEntity
    ): MeasurementDTO {
        return MeasurementDTO(
            id = photo.photoUUID.toString(),
            activityID = landSurvey.activityUUID.toString(),
            dateTime = photo.metaData.timestamp,
            treeDBHmm = 0.0,
            treeHealth = "N/A",
            treeHeightMm = 0,
            stepsSinceLastMeasurement = 0,
            measurementType = photo.imageType,
            gpsLocation = "${photo.metaData.gpsCoordinates?.lat} ,${photo.metaData.gpsCoordinates?.lng}",
            gpsAccuracy = photo.metaData.gpsAccuracy,
            additionalData = Gson().toJson(
                mapOf(
                    "corners" to landSurvey.corners, "sequenceNumber" to landSurvey.sequenceNumber
                )
            )
        )
    }

    private fun buildTreeMeasurementDTO(
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

    private fun buildBinaryDataDTO(
        imagePath: String,
        measurementId: String
    ): BinaryDataDTO {
        return BinaryDataDTO(imagePath, measurementId)
    }

    fun getCompletedActivitiesForUpload(): Flow<List<ActivityEntity>> {
        return activityDao.getCompletedActivitiesForUpload()
    }

    fun getUploadQueue(): Flow<List<UploadQueueEntity>> {
        return activityDao.getUploadQueue()
    }

    suspend fun getUploadQueueOnce(): List<UploadQueueEntity> {
        return withContext(dispatcher.io()) {
            activityDao.getUploadQueueSync()
        }
    }

    suspend fun markQueueItemAsUploaded(activityId: Long, queueItemId: Long) {
        activityDao.markQueueEntityAsUploaded(activityId, queueItemId)
    }

    fun getOfflineSyncStatus(): Flow<Int> {
        return flow {
            activityDao.getUploadQueue().collect {
                val networkAvailable = context.isNetworkAvailable()
                val dataToSync = it.isNotEmpty()

                val offlineSyncStatus = when {
                    !networkAvailable && dataToSync -> OFFLINE_WITH_DATA_TO_SYNC
                    !networkAvailable && !dataToSync -> OFFLINE_WITHOUT_DATA_TO_SYNC
                    networkAvailable && !dataToSync -> getStatusWhenOnlineWithEmptyQueue()
                    else -> ONLINE_WITH_SYNC_IN_PROGRESS
                }

                emit(offlineSyncStatus)
            }
        }
    }

    private fun getStatusWhenOnlineWithEmptyQueue(): Int {
        val uploadAcknowledged = preferences.getBoolean(
            SUCCESSFUL_UPLOAD_ACKNOWLEDGED,
            false
        )
        val queueNotUploadedBefore =
            !preferences.contains(QUEUE_UPLOADED_BEFORE)

        return if (queueNotUploadedBefore || uploadAcknowledged)
            ONLINE_WITHOUT_DATA_TO_SYNC
        else
            ONLINE_SYNC_SUCCESSFUL
    }

    fun onAcknowledgeSuccessfulSync() {
        preferences.edit().apply {
            putBoolean(SUCCESSFUL_UPLOAD_ACKNOWLEDGED, true)
            apply()
        }
    }

    suspend fun markQuestionnaireAsCompleted(id: Long) {
        activityDao.markQuestionnaireAsCompleted(id)
    }

    suspend fun insertLandSurvey(landSurvey: LandSurvey) {
        val landSurveyEntity =
            modelEntityMapper.getLandSurveyEntityFromLandSurvey(landSurvey)
        landSurveyDao.insertLandSurvey(landSurveyEntity)
    }

    suspend fun insertLandSurveyPhotos(photos: List<Photo>) {
        val photoEntities =
            modelEntityMapper.getListOfPhotoEntitiesFromPhotos(photos)

        for (item in photoEntities) {
            landSurveyDao.insertPhotoEntity(item)
        }
    }

    suspend fun getLandSurveyWithPhotosByActivity(activityId: Long): Map<String, Any> {
        return try {
            val entity =
                landSurveyDao.getLandSurveyWithPhotosByActivity(activityId)

            val landSurvey: LandSurvey
            val landSurveyPhotos: List<Photo>

            if (entity != null) {
                landSurvey =
                    modelEntityMapper.getLandSurveyFromLandSurveyEntity(entity.landSurvey)
                landSurveyPhotos =
                    modelEntityMapper.getListOfPhotosFromPhotoEntities(entity.photos)

                mapOf(
                    "landSurvey" to landSurvey,
                    "photoList" to landSurveyPhotos
                )
            } else {
                emptyMap()
            }
        } catch (e: NullPointerException) {
            Log.e("Land Survey: ", "Null Land Survey DBMainRepository")
            return emptyMap()
        }
    }

    suspend fun getLandSurveyByActivity(activityId: Long): LandSurvey? {
        val entity = landSurveyDao.getLandSurveyByActivity(activityId)
        return if (entity != null) {
            modelEntityMapper.getLandSurveyFromLandSurveyEntity(entity)
        } else {
            null
        }
    }

    suspend fun setNumberOfCornersToLandSurvey(
        numOfCorners: Int,
        activityId: Long
    ) {
        landSurveyDao.setNumberOfCornersToLandSurvey(numOfCorners, activityId)
    }

    suspend fun insertLandSurveyPhoto(photo: Photo) {
        val entity = modelEntityMapper.getPhotoEntityFromPhoto(photo)
        landSurveyDao.insertPhotoEntity(entity)
    }

    suspend fun markLandSurveyAsCompleted(surveyId: Long) {
        landSurveyDao.markLandSurveyAsCompleted(surveyId)
    }

    suspend fun uploadItemsInQueue() {
        val uploadQueue = getUploadQueueOnce()
        if (uploadQueue.isNotEmpty()) {
            with(preferences.edit()) {

                // Save the total amount of data in bytes to upload.
                val totalBytes = uploadQueue.calculateBytesLeftToUploadInQueue()
                putInt(TOTAL_BYTES_TO_SYNC, totalBytes)

                putBoolean(SUCCESSFUL_UPLOAD_ACKNOWLEDGED, false)
                putBoolean(QUEUE_UPLOADED_BEFORE, true)

                // Update last sync date
                putString(DATE_OF_LAST_SYNC, formatLastSyncDate())
                apply()
            }

            createWorkManagerUploadRequest()
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun formatLastSyncDate(): String {
        val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        val date = Calendar.getInstance().time
        return dateFormat.format(date)
    }

    private fun createWorkManagerUploadRequest() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val uploadWorkRequest =
            OneTimeWorkRequestBuilder<UploadQueueWorker>()
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .build()
        workManager.enqueueUniqueWork(
            "uploadQueueContent",
            ExistingWorkPolicy.REPLACE,
            uploadWorkRequest
        )
    }

    suspend fun markActivityInProgress(activityId: Long) {
        activityDao.markActivityInProgress(activityId)
    }

    fun getDateOfLastSync(): String? =
        preferences.getString(DATE_OF_LAST_SYNC, null)

    fun getTotalBytesAtStartOfSync() =
        preferences.getInt(TOTAL_BYTES_TO_SYNC, 0)

    suspend fun insertTreeMeasurement(tree: TreeMeasurement): Long {
        return tmDao.insertTreeMeasurement(modelEntityMapper.treeMeasurementToEntity(tree))
    }

    suspend fun insertTreePhoto(photo: Photo) {
        val entity = PhotoEntity(
            surveyId = null,
            treeMeasurementId = photo.treeMeasurementId,
            photoUUID = photo.photoUUID,
            measurementUUID = photo.measurementUUID,
            imagePath = photo.imagePath,
            imageType = null,
            metaData = MetaData(
                timestamp = photo.metaData.timestamp,
                resolution = null,
                gpsCoordinates = photo.metaData.gpsCoordinates,
                gpsAccuracy = photo.metaData.gpsAccuracy,
                stepsTaken = null,
                azimuth = null,
                cameraOrientation = null,
                flashLight = photo.metaData.flashLight
            )
        )

        tmDao.insertTreePhoto(entity)
    }

    suspend fun createAdHocTreeMeasurementActivity(): Long {
        return tmDao.createAdHocTreeMeasurementActivity(
            createAdhocActivity(ACTIVITY_TYPE_TREE_MONITORING)
        )
    }

    suspend fun createAdHocTreeMeasurementFromEntity(activityEntity: ActivityEntity): Long {
        return tmDao.createAdHocTreeMeasurementActivity(activityEntity)
    }
}
