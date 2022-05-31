package org.treeo.treeo.ui.treemeasurement

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.treeo.treeo.R
import org.treeo.treeo.db.models.ActivityEntity
import org.treeo.treeo.db.models.ForestInventoryEntity
import org.treeo.treeo.models.Activity
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.repositories.ForestInventoryRepository
import org.treeo.treeo.ui.treemeasurement.TMViewModel.Companion.currentActivityId
import org.treeo.treeo.util.IDispatcherProvider
import org.treeo.treeo.util.createNewAdhocActivityFromExistingActivity
import org.treeo.treeo.util.mappers.ModelEntityMapper
import javax.inject.Inject

@HiltViewModel
class BottomSheetDialogViewModel @Inject constructor(
    private val dispatcher: IDispatcherProvider,
    private val dbRepository: DBMainRepository,
    private val mapper: ModelEntityMapper,
    private val forestInventoryRepository: ForestInventoryRepository
) : ViewModel() {

    var treeType: String? = null
    var forestInventoryId: Long? = null

    private val _incompleteInventory = MutableLiveData<ForestInventoryEntity?>()
    private val _newInventoryId = MutableLiveData<Long>()
    val _adhocActivityId = MutableLiveData<Long>()
    private val _adhocActivity = MutableLiveData<Activity>()
    private val _incompleteAdhocActivity = MutableLiveData<Activity>()


    val incompleteInventory: LiveData<ForestInventoryEntity?> get() = _incompleteInventory
    val newInventoryId: LiveData<Long> get() = _newInventoryId
    val adhocActivityId: LiveData<Long> get() = _adhocActivityId
    val adhocActivity: LiveData<Activity> get() = _adhocActivity
    val incompleteAdhocActivity: LiveData<Activity> get() = _incompleteAdhocActivity

    fun createAdHocTreeMeasurementActivity() {
        viewModelScope.launch(dispatcher.io()) {
            withContext(dispatcher.io()) {
                activityId = dbRepository.createAdHocTreeMeasurementActivity()
                val activity = dbRepository.getActivity(activityId!!)
                treeType = activity.template.configuration?.measurementType
                _adhocActivityId.postValue(activity.activityId)
            }
        }
    }

    fun createActivityFromEntity(theActivityEntity: ActivityEntity, isOneTree: Boolean = false) {
        viewModelScope.launch(dispatcher.io()) {
            withContext(dispatcher.io()) {

                try {
                    val activityId =
                        dbRepository.createAdHocTreeMeasurementFromEntity( theActivityEntity)

                    currentActivityId = activityId

                    if (isOneTree) {
                        treeType = theActivityEntity.template.configuration?.measurementType
                        _adhocActivityId.postValue(activityId)

                    } else {
                        _adhocActivity.postValue(
                            mapper.getActivityFromEntity(
                                dbRepository.getActivity(
                                    activityId
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                }

            }
        }
    }

    fun startNewForestInventory() {
        viewModelScope.launch(dispatcher.io()) {
            val inventoryId = forestInventoryRepository.createForestInventory(null)
            initForestInventoryVariables(inventoryId)
        }
    }

    fun startNewForestInventoryByActivityId(id: Long) {
        viewModelScope.launch {
            val inventoryId = forestInventoryRepository.createForestInventoryByActivityId(id)
            initForestInventoryVariables(inventoryId)
        }
    }

    fun getIncompleteForestInventory() {
        viewModelScope.launch(dispatcher.io()) {
            _incompleteInventory.postValue(forestInventoryRepository.getIncompleteForestInventory())
        }
    }

    fun getOneTreeActivity(context: Context, activity: android.app.Activity) {
        viewModelScope.launch(dispatcher.io()) {
            val oneTreeAdhoc =
                dbRepository.getActivitiesByType(type = "adhoc", activityType = "one_tree")

            if (oneTreeAdhoc.isNotEmpty()) {
                val aEtty = createNewAdhocActivityFromExistingActivity(
                    mapper.getActivityFromEntity(oneTreeAdhoc.first())
                )
                createActivityFromEntity(aEtty, true)
            } else {
                activity.runOnUiThread {
                    Toast.makeText(
                        context,
                        context.getText(R.string.one_tree_not_available),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun getIncompleteAdhocActivities(plotArea: Long?) {
        viewModelScope.launch(dispatcher.io()) {
            var items =
                dbRepository.getIncompleteActivities(type = "adhoc", activityType = "tree_survey")

            if (plotArea == null) {
                items = items.filter { it.plot == null }
                _incompleteAdhocActivity.postValue((if (items.isEmpty()) null else items.last())?.let {
                    mapper.getActivityFromEntity(
                        it
                    )
                })
            } else {
                items = items.filter { it.plot != null }
                val act: List<Activity> =
                    items.map { value -> mapper.getActivityFromEntity(value) }.toList()
                val aList = act.filter { value -> value.plot?.area!!.toLong() == plotArea }
                _incompleteAdhocActivity.postValue((if (aList.isEmpty()) null else aList.last()))
            }

        }
    }


    fun initForestInventory(inventoryId: Long, isNew: Boolean = true) {
        viewModelScope.launch {
            initForestInventoryVariables(inventoryId, isNew)
        }
    }

    private suspend fun initForestInventoryVariables(inventoryId: Long, isNew: Boolean = true) {
        val forestInventory = forestInventoryRepository.getForestInventory(inventoryId)
        activityId = forestInventory.activityId
        forestInventoryId = forestInventory.forestInventoryId
        treeType = dbRepository.getActivity(activityId!!).template.configuration?.measurementType
        if (isNew) {
            _newInventoryId.postValue(inventoryId)
        }
    }

    fun clearPreviousInventoryAndStartNewOne(inventoryId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            val newInventoryId =
                forestInventoryRepository.deleteForestInventoryAndStartNewOne(inventoryId)
            initForestInventoryVariables(newInventoryId)
        }
    }

    fun clearPreviousInventoryAndStartNewOneBtyActivityId(inventoryId: Long, id: Long) {
        viewModelScope.launch(dispatcher.io()) {
            val newInventoryId =
                forestInventoryRepository.deleteForestInventoryAndStartNewOneByActivityId(
                    inventoryId
                )
            initForestInventoryVariables(newInventoryId)
        }
    }

    companion object {
        var activityId: Long? = null
    }
}
