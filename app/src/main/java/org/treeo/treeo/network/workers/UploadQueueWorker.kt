package org.treeo.treeo.network.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.treeo.treeo.db.dao.ActivityDao
import org.treeo.treeo.db.models.UploadQueueEntity
import org.treeo.treeo.network.RequestManager
import org.treeo.treeo.network.models.ActivityUploadDTO
import org.treeo.treeo.network.models.BinaryDataDTO
import org.treeo.treeo.network.models.MeasurementDTO
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.util.*
import java.io.File

@HiltWorker
class UploadQueueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: SharedPreferences,
    private val requestManager: RequestManager,
    private val dbMainRepository: DBMainRepository,
    private val activityDao: ActivityDao,
    private val dispatcher: IDispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uploadQueue = getUploadQueueOnce()
        return if (uploadQueue.isNotEmpty()) {
            var shouldRetryUpload = false
            for (item in uploadQueue) {
                val uploadFailed = uploadQueueItem(item)
                if (uploadFailed != null) {
                    shouldRetryUpload = true
                }
            }
            if (shouldRetryUpload) Result.retry() else Result.success()
        } else {
            Result.success()
        }
    }

    private suspend fun getUploadQueueOnce(): List<UploadQueueEntity> {
        return withContext(dispatcher.io()) {
            activityDao.getUploadQueueSync()
        }
    }

    /**
     * Uploads a single queue item depending on its content type.
     * @param item The item to be uploaded.
     * @return true if the upload failed, null if the upload was successful.
     */
    private suspend fun uploadQueueItem(item: UploadQueueEntity): Boolean? {
        var shouldRetryUpload: Boolean?
        try {
            var responseCode = 0
            when (item.type) {
                "questionnaire" -> {
                    val questionnaireData = Gson().fromJson(
                        item.activityData,
                        ActivityUploadDTO::class.java
                    )
                    responseCode =
                        uploadQuestionnaireData(mapOf("activity" to questionnaireData))
                }
                ACTIVITY_TYPE_LAND_SURVEY -> {
                    val measurementData = Gson().fromJson(
                        item.activityData,
                        MeasurementDTO::class.java
                    )

                    responseCode = uploadMeasurementData(measurementData)
                }
                "binary_upload" -> {
                    val binaryData = Gson().fromJson(
                        item.activityData,
                        BinaryDataDTO::class.java
                    )
                    responseCode = uploadBinaryData(binaryData)
                }

                ACTIVITY_TYPE_TREE_MONITORING, ACTIVITY_FOREST_INVENTORY -> {
                    val treeMonitoringData = Gson().fromJson(
                        item.activityData,
                        ActivityUploadDTO::class.java
                    )
                    responseCode =
                        uploadQuestionnaireData(mapOf("activity" to treeMonitoringData))
                }

                SUB_ACTIVITY_TYPE_TREE_MEASUREMENT -> {
                    val measurementData = Gson().fromJson(
                        item.activityData,
                        MeasurementDTO::class.java
                    )

                    responseCode = uploadMeasurementData(measurementData)
                }
            }

            shouldRetryUpload = if (responseCode == 201) {
                deleteUploadedActivity(item.activityId, item.id)
                null
            } else if (responseCode == 400 || responseCode == 404 || responseCode == 422 || responseCode == 500) {
                true
            } else {
                true
            }
        } catch (e: Exception) {
            shouldRetryUpload = true
        }
        return shouldRetryUpload
    }

    private suspend fun uploadQuestionnaireData(
        dataMap: Map<String, ActivityUploadDTO>
    ): Int {
        return requestManager.createActivity(dataMap)
    }

    private suspend fun uploadMeasurementData(measurement: MeasurementDTO): Int {
        return requestManager.uploadMeasurementData(measurement)
    }

    private suspend fun uploadBinaryData(binaryDataDTO: BinaryDataDTO): Int {
        val file = File(binaryDataDTO.file)

        val fileRequestBody = file
            .asRequestBody("multipart/form-file".toMediaTypeOrNull())

        val filePart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            fileRequestBody
        )

        val measurementId = binaryDataDTO.measurementId

        val measurementRequestBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            measurementId
        )

        return requestManager.uploadBinaryData(
            filePart,
            measurementRequestBody
        )
    }

    private suspend fun deleteUploadedActivity(activityId: Long, queueItemId: Long) {

        // TODO: Delete queue item instead of simply marking it as not for upload
        dbMainRepository.markQueueItemAsUploaded(activityId, queueItemId)
    }
}
