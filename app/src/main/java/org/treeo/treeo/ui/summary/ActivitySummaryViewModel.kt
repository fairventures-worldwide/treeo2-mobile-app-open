package org.treeo.treeo.ui.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.treeo.treeo.models.*
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.util.ACTIVITY_TYPE_LAND_SURVEY
import org.treeo.treeo.util.IDispatcherProvider
import org.treeo.treeo.util.PRE_QUESTIONNAIRE
import org.treeo.treeo.util.mappers.ModelEntityMapper

import java.util.*
import javax.inject.Inject

@HiltViewModel
class ActivitySummaryViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository,
    private val dispatcher: IDispatcherProvider,
    private val mapper: ModelEntityMapper
) : ViewModel() {

    private val _activitySummaryItems = MutableLiveData<List<ActivitySummaryItem>>()
    val activitySummaryItems: LiveData<List<ActivitySummaryItem>> get() = _activitySummaryItems

    fun getActivitySummaryItems(activityId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            val summaryItemList = mutableListOf<ActivitySummaryItem>()

            val activity = withContext(dispatcher.io()) {
                getActivity(activityId)
            }

            val questionnaires = withContext(dispatcher.io()) {
                dbMainRepository.getQuestionnaireWithPages(activityId)
            }

            questionnaires.forEach { questionnaireWithPages ->
                val pages = mapper.getListOfPageFromEntities(questionnaireWithPages.pages)

                for (page in pages){

                    page.options = mapper.getListOfOptionsFromEntities(
                        dbMainRepository.getPageOptions(page.pageId)
                    )

//
                }

                val questionnaire = QuestionnaireSummaryItem(
                    questionnaireWithPages.questionnaire.questionnaireId,
                    questionnaireWithPages.questionnaire.isCompleted,
                    questionnaireWithPages.questionnaire.type,
                    pages
                )

                questionnaire.activity = activity

                if (questionnaire.type == PRE_QUESTIONNAIRE) {
                    summaryItemList.add(0, questionnaire)
                } else {
                    summaryItemList.add(questionnaire)
                }
            }

            if (activity.template.activityType == ACTIVITY_TYPE_LAND_SURVEY) {
                val landSurvey = dbMainRepository.getLandSurveyByActivity(activityId)

                val landSurveyItem = if (landSurvey != null) {
                    val landSurveyWithPhotos = dbMainRepository
                        .getLandSurveyWithPhotosByActivity(activityId)

                    val photoList: List<Photo> = landSurveyWithPhotos["photoList"] as List<Photo>

                    LandSurveySummaryItem(photoList, landSurvey)

                } else {
                    dbMainRepository.insertLandSurvey(
                        LandSurvey(
                            activityId = activity.id,
                            surveyUUID = UUID.randomUUID(),
                            activityUUID = activity.uuid,
                            sequenceNumber = 0,
                            corners = 0,
                            isCompleted = false
                        )
                    )

                    LandSurveySummaryItem(
                        emptyList(),
                        dbMainRepository.getLandSurveyByActivity(activityId)!!
                    )
                }

                landSurveyItem.activity = activity
                summaryItemList.add(1, landSurveyItem)
            }

            _activitySummaryItems.postValue(summaryItemList)
        }
    }

    private suspend fun getActivity(activityId: Long): Activity {
        return mapper.getActivityFromEntity(dbMainRepository.getActivity(activityId))
    }

    fun markActivityInProgress(activityId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.markActivityInProgress(activityId)
        }
    }

}

