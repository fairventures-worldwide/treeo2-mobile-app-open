package org.treeo.treeo.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.treeo.treeo.db.models.ForestInventoryEntity
import org.treeo.treeo.models.Activity
import org.treeo.treeo.models.BasicUserInfo
import org.treeo.treeo.network.models.ActivityDTO
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.repositories.ForestInventoryRepository
import org.treeo.treeo.repositories.MainRepository
import org.treeo.treeo.util.IDispatcherProvider
import org.treeo.treeo.util.calculateBytesLeftToUploadInQueue
import org.treeo.treeo.util.mappers.ModelEntityMapper
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dbMainRepository: DBMainRepository,
    private val dispatcher: IDispatcherProvider,
    private val mainRepository: MainRepository,
    private val entityMapper: ModelEntityMapper,
    private val forestInventoryRepository: ForestInventoryRepository,
) : AndroidViewModel(Application()) {

    private val _nextTwoActivities = MutableLiveData<List<Activity>>()
    val nextTwoActivities: LiveData<List<Activity>> get() = _nextTwoActivities

    private val _offlineSyncStatus = MutableLiveData<Int>()
    val offlineSyncStatus: LiveData<Int> get() = _offlineSyncStatus

    private val _dataToSync = MutableLiveData<Int>()
    val dataToSync: LiveData<Int> get() = _dataToSync

    private val _dataInBytes = MutableLiveData<Int>()
    val dataInBytes: LiveData<Int> get() = _dataInBytes

    private val _basicUserInfo = MutableLiveData<BasicUserInfo>()
    val basicUserInfo: LiveData<BasicUserInfo> get() = _basicUserInfo

    init {
        getNextOneTimeActivities()
        uploadQueueContent()
    }

    fun getRemotePlannedActivities() {
        viewModelScope.launch {
            val plannedActivities = mainRepository.retrievePlannedActivities()
            insertOrUpdateActivities(plannedActivities)
        }
    }

    private fun insertOrUpdateActivities(activities: List<ActivityDTO>) {
        viewModelScope.launch(dispatcher.io()) {
            dbMainRepository.insertOrUpdateActivities(activities)
        }
    }

    private fun getNextOneTimeActivities() {
        viewModelScope.launch {
            dbMainRepository.getPlannedActivities("onetime").collect { entities ->
                val list =
                    entityMapper.getListOfActivitiesFromEntities(entities)
                _nextTwoActivities.postValue(list)
            }
        }
    }

    suspend fun getBytesLeftToUploadInQueue(): Int {
        val uploadQueue = dbMainRepository.getUploadQueueOnce()
        return uploadQueue.calculateBytesLeftToUploadInQueue()
    }

    fun getOfflineSyncStatus() {
        viewModelScope.launch {
            dbMainRepository.getOfflineSyncStatus().collect {
                _offlineSyncStatus.postValue(it)
            }
        }
    }

    fun uploadQueueContent() {
        viewModelScope.launch {
            dbMainRepository.uploadItemsInQueue()
        }
    }

    fun getDataToSync() {
        viewModelScope.launch {
            val data = dbMainRepository.getUploadQueueOnce()
            if (data.isNotEmpty()) {
                _dataToSync.postValue(data.size)
            }
        }
    }

    fun setActivityStartDate(activityId: Long) {
        viewModelScope.launch {
            dbMainRepository.setActivityStartDate(activityId)
        }
    }

    fun acknowledgeSuccessfulSync() {
        dbMainRepository.onAcknowledgeSuccessfulSync()
    }

    fun getBasicUserInfo() {
        viewModelScope.launch {
            _basicUserInfo.postValue(mainRepository.getBasicUserInfo())
        }
    }

    fun getDateOfLastSync() = dbMainRepository.getDateOfLastSync()

    fun getTotalBytesAtStartOfSync() = dbMainRepository.getTotalBytesAtStartOfSync()

    fun syncTreeSpecies() {
        viewModelScope.launch {
            forestInventoryRepository.syncTreeSpecies()
        }
    }

    suspend fun checkIncompleteInventory(activityId: Long): ForestInventoryEntity? {
        return forestInventoryRepository.getIncompleteForestInventoryByActivityId(activityId)
    }

    suspend fun startNewForestInventory(activityId: Long?): Long {
        return forestInventoryRepository.createForestInventory(activityId)
    }
}
