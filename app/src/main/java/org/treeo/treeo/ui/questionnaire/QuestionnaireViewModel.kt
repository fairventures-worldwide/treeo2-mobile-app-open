package org.treeo.treeo.ui.questionnaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.util.IDispatcherProvider
import javax.inject.Inject

@HiltViewModel
class QuestionnaireViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository,
    private val dispatcher: IDispatcherProvider,
    private val landSurveyDao: LandSurveyDao,
) : ViewModel() {

    fun updateOption(id: Long, isSelected: Boolean) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.updateOption(id, isSelected)
        }
    }

    fun updateUserInput(pageId: Long, response: String) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.updateUserInput(pageId, response)
        }
    }

    fun markQuestionnaireAsCompleted(id: Long) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.markQuestionnaireAsCompleted(id)
        }
    }

    fun markActivityAsCompleted(activityId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.markActivityAsCompleted(
                activityId,
                landSurveyDao.countLandSurveysForActivity(activityId),
            )
        }
    }
}
