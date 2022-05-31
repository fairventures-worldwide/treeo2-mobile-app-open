package org.treeo.treeo.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.treeo.treeo.db.models.LandSurveyEntity
import org.treeo.treeo.db.models.PhotoEntity
import org.treeo.treeo.db.models.relations.LandSurveyWithPhotos
import java.util.*

@Dao
interface LandSurveyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandSurvey(landSurvey: LandSurveyEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotoEntity(photo: PhotoEntity)

    @Query("SELECT * FROM LandSurvey WHERE activityId=:activityId")
    suspend fun getLandSurveyWithPhotosByActivity(activityId: Long): LandSurveyWithPhotos?

    @Query("SELECT * FROM LandSurvey WHERE surveyId=:id")
    suspend fun getLandSurveyWithPhotosById(id: Long): LandSurveyWithPhotos

    @Query("SELECT * FROM LandSurvey")
    suspend fun getLandSurveyWithPhotos(): List<LandSurveyWithPhotos>

    @Query("SELECT * FROM LandSurvey WHERE activityId=:activityId")
    suspend fun getLandSurveyByActivity(activityId: Long): LandSurveyEntity?

    @Query("SELECT * FROM LandSurvey WHERE activityUUID=:uuid")
    suspend fun getLandSurveyByActivityUUID(uuid: UUID): LandSurveyEntity?

    @Query("UPDATE LandSurvey SET corners=:numOfCorners WHERE activityId=:activityId")
    suspend fun setNumberOfCornersToLandSurvey(numOfCorners: Int, activityId: Long)

    @Query("UPDATE LandSurvey SET isCompleted=1 WHERE surveyId=:surveyId")
    suspend fun markLandSurveyAsCompleted(surveyId: Long)

    @Query("SELECT COUNT(measurementId) FROM LandSurvey WHERE activityId=:activityId")
    suspend fun countLandSurveysForActivity(activityId: Long): Int
}
