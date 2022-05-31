package org.treeo.treeo.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.treeo.treeo.models.Activity
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.util.IDispatcherProvider
import org.treeo.treeo.util.mappers.ModelEntityMapper
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository,
    private val dispatcher: IDispatcherProvider,
    private val mapper: ModelEntityMapper
) : ViewModel() {

    private val _completedActivities = MutableLiveData<List<Activity>>()
    val completedActivities: LiveData<List<Activity>> get() = _completedActivities

    fun getAllActivities() {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.getAllActivities().collect {
                val activityList = mapper.getListOfActivitiesFromEntities(it)
                _completedActivities.postValue(activityList)
            }
        }
    }

    fun setActivityStartDate(activityId: Long) {
        viewModelScope.launch {
            dbMainRepository.setActivityStartDate(activityId)
        }
    }
}
