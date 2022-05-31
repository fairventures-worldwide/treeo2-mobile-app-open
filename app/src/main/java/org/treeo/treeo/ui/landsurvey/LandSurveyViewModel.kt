package org.treeo.treeo.ui.landsurvey

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.models.LandSurvey
import org.treeo.treeo.models.Photo
import org.treeo.treeo.repositories.DBMainRepository
import javax.inject.Inject

@HiltViewModel
class LandSurveyViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository,
    private val landSurveyDao: LandSurveyDao
) : ViewModel() {

    private val _landSurveyWithPhotos = MutableLiveData<Map<String, Any>>()
    val landSurveyWithPhotos: LiveData<Map<String, Any>> get() = _landSurveyWithPhotos

    private val _landSurvey = MutableLiveData<LandSurvey>()
    val landSurvey: LiveData<LandSurvey> get() = _landSurvey

    fun getLandSurvey(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _landSurvey.postValue(dbMainRepository.getLandSurveyByActivity(activityId))
        }
    }

    fun insertLandSurveyPhoto(photo: Photo) {
        viewModelScope.launch {
            dbMainRepository.insertLandSurveyPhoto(photo)
        }
    }

    fun insertLandSurveyPhotos(photos: List<Photo>) {
        viewModelScope.launch {
            dbMainRepository.insertLandSurveyPhotos(photos)
        }
    }

    fun getLandSurveyWithPhotosByActivity(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _landSurveyWithPhotos.postValue(
                dbMainRepository.getLandSurveyWithPhotosByActivity(activityId)
            )
        }
    }

    fun setNumberOfCornersToLandSurvey(numOfCorners: Int, activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMainRepository.setNumberOfCornersToLandSurvey(numOfCorners, activityId)
        }
    }

    fun markLandSurveyAsCompleted(surveyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMainRepository.markLandSurveyAsCompleted(surveyId)
        }
    }

    fun markActivityAsCompleted(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbMainRepository.markActivityAsCompleted(
                activityId,
                landSurveyDao.countLandSurveysForActivity(activityId),
            )
        }
    }
}
