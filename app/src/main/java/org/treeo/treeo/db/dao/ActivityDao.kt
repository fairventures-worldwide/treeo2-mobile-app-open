package org.treeo.treeo.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.treeo.treeo.db.models.*
import org.treeo.treeo.db.models.relations.PageWithOptions
import org.treeo.treeo.db.models.relations.QuestionnaireWithPages

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuestionnaire(questionnaire: QuestionnaireEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePage(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOption(option: OptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserInput(userInput: UserInputEntity)

    @Query("SELECT * FROM Activity")
    fun getActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM Activity WHERE isComplete=0 AND isAdhoc=0 LIMIT 2")
    fun getNextTwoActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM Activity WHERE isComplete=0 AND type=:type AND inProgress = 0 LIMIT 2")
    fun getPlannedActivities(type: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM Activity WHERE type=:type AND isComplete = 0 AND inProgress = 0")
    fun getAdhocActivities(type: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM Activity")
    fun getAllPlannedActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM Activity WHERE activityId = :activityId AND (activityType='one_tree' OR activityType='tree_survey') ")
    fun getTreeMonitoring(activityId: Long): List<ActivityEntity>

    @Query("SELECT * FROM Activity WHERE activityId = :activityId")
    suspend fun getActivity(activityId: Long): ActivityEntity

    @Query("SELECT * FROM Activity WHERE activityRemoteId = :activityRemoteId")
    suspend fun getActivityEntityByRemoteId(activityRemoteId: Long): ActivityEntity?

    @Query("SELECT * FROM Questionnaire WHERE activityId=:activityId")
    suspend fun getQuestionnaireWithPages(activityId: Long): List<QuestionnaireWithPages>

    @Query("SELECT * FROM Page WHERE questionnaireId=:id")
    suspend fun getPageWithOptions(id: Long): PageWithOptions

    @Query("SELECT * FROM Option WHERE pageId=:id")
    suspend fun getPageOptions(id: Long): List<OptionEntity>

    @Query("UPDATE Option SET isSelected=:isSelected WHERE optionId=:id")
    suspend fun updateOption(id: Long, isSelected: Boolean)

    @Query("UPDATE UserInput SET userResponse=:response WHERE pageId=:pageId")
    suspend fun updateUserInput(pageId: Long, response: String)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("SELECT * FROM Option WHERE isSelected=1 AND pageId =:pageId")
    suspend fun getSelectedOptions(pageId: Long): List<OptionEntity>

    @Query("UPDATE Questionnaire SET isCompleted=1 WHERE questionnaireId=:id")
    suspend fun markQuestionnaireAsCompleted(id: Long)

    @Query("UPDATE Activity SET inProgress =1 WHERE activityId =:activityId")
    suspend fun markActivityInProgress(activityId: Long)

    @Query("UPDATE Activity SET measurementCount = :count WHERE activityId =:activityId")
    suspend fun addActivityMeasurementCount(activityId: Long, count: Int)

    @Query("UPDATE Activity SET inProgress =0 WHERE activityId =:activityId")
    suspend fun markActivityNotInProgress(activityId: Long)

    @Query("UPDATE Activity SET isComplete =1, inProgress=0 WHERE activityId =:activityId AND isComplete=0")
    suspend fun markActivityAsCompleted(activityId: Long)

    @Query("SELECT * FROM Activity WHERE isComplete =1")
    suspend fun getCompletedActivities(): List<ActivityEntity>

    @Query("SELECT * FROM Activity WHERE inProgress =1 AND type=:type AND activityType =:activityType")
    suspend fun getIncompleteAdhocActivities(
        type: String,
        activityType: String
    ): List<ActivityEntity>

    @Query("SELECT * FROM Activity WHERE type=:type AND activityType =:activityType")
    suspend fun getActivitiesByType(type: String, activityType: String): List<ActivityEntity>

    @Query("SELECT * FROM Activity WHERE isComplete =1")
    fun getCompletedActivitiesForUpload(): Flow<List<ActivityEntity>>

    @Insert
    suspend fun addActivityToUploadQueue(upload: UploadQueueEntity)

    @Query("UPDATE Activity SET startDate=:date WHERE activityId=:activityId")
    suspend fun setActivityStartDate(activityId: Long, date: String)

    @Query("UPDATE Activity SET endDate=:date WHERE activityId=:activityId")
    suspend fun setActivityEndDate(activityId: Long, date: String)

    @Query("SELECT * FROM UploadQueue WHERE forUpload=1")
    suspend fun getItemsForUploadSync(): UploadQueueEntity

    @Query("SELECT * FROM Page WHERE questionnaireId=:questionnaireId")
    suspend fun getQuestionnairePages(questionnaireId: Long): List<PageEntity>

    @Query("SELECT * FROM Option WHERE pageId=:pageId AND isSelected=1")
    suspend fun getPageAnswers(pageId: Long): List<OptionEntity>

    @Query("SELECT * FROM UserInput WHERE pageId=:pageId")
    suspend fun getUserInputs(pageId: Long): List<UserInputEntity>

    @Query("SELECT * FROM Questionnaire WHERE activityId=:activityId")
    suspend fun getQuestionnaire(activityId: Long): QuestionnaireEntity

    @Query("SELECT * FROM Questionnaire WHERE activityId=:activityId")
    suspend fun getQuestionnaires(activityId: Long): List<QuestionnaireEntity>

    @Query("SELECT * FROM UploadQueue WHERE forUpload=1")
    fun getUploadQueue(): Flow<List<UploadQueueEntity>>

    @Query("UPDATE UploadQueue SET forUpload=0 WHERE activityId=:activityId AND id=:queueItemId")
    suspend fun markQueueEntityAsUploaded(activityId: Long, queueItemId: Long)

    @Query("DELETE FROM Activity WHERE activityId=:activityId")
    suspend fun deleteActivityEntity(activityId: Long)

    @Query("DELETE FROM Questionnaire WHERE activityId=:activityId")
    suspend fun deleteQuestionnaireEntity(activityId: Long)

    @Query("DELETE FROM Option WHERE pageId=:pageId")
    suspend fun deleteOptionEntity(pageId: Long)

    @Query("DELETE FROM Page WHERE pageId=:pageId")
    suspend fun deletePageEntity(pageId: Long)

    @Query("SELECT * FROM UploadQueue WHERE forUpload=1")
    suspend fun getUploadQueueSync(): List<UploadQueueEntity>
}