package org.treeo.treeo.ui.treemeasurement

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.TreeSpecieEntity
import org.treeo.treeo.models.*
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.repositories.ForestInventoryRepository
import org.treeo.treeo.util.*
import org.treeo.treeo.util.mappers.ModelEntityMapper
import java.sql.Timestamp
import java.util.*
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class TMViewModel @Inject constructor(
    private val dispatcher: IDispatcherProvider,
    private val dbRepository: DBMainRepository,
    private val forestInventoryRepository: ForestInventoryRepository,
    private val treeMeasurementDao: TreeMeasurementDao,
    private val preferences: SharedPreferences,
    private val entityMapper: ModelEntityMapper,
    @ApplicationContext val context: Context,
) : ViewModel() {

    private lateinit var measureJob: Job
    var numOfAttempts = 0
    private val _cardPolygon = MutableLiveData<String>()
    private val _treeLines = MutableLiveData<String>()
    private val _treeDiameter = MutableLiveData<Double>()
    private val _smallTreeIsClear = MutableLiveData<Boolean>()
    private val _treeSpecies = MutableLiveData<List<TreeSpecieEntity>>()
    private val _adhocActivities = MutableLiveData<List<Activity>>()
    private val _additionalDataConfigLoaded = MutableLiveData<Boolean>()
    val cardPolygon: LiveData<String> get() = _cardPolygon
    val treeLines: LiveData<String> get() = _treeLines
    val treeDiameter: LiveData<Double> get() = _treeDiameter
    val smallTreeIsClear: LiveData<Boolean> get() = _smallTreeIsClear
    val treeSpecies: LiveData<List<TreeSpecieEntity>> get() = _treeSpecies
    val adhocActivities: LiveData<List<Activity>> get() = _adhocActivities
    val additionalDataConfigLoaded: LiveData<Boolean> get() = _additionalDataConfigLoaded
    var ptrObjectHolder: Long = 0;
    var currentRetryTimes: Int? = 3

    private val _hasInitializedMeasurementObject = MutableLiveData(false)
    val hasInitializedMeasurementObject: LiveData<Boolean> get() = _hasInitializedMeasurementObject

    var treeMeasurement: TreeMeasurement? = null
    private var treePhoto: Photo? = null
    private var imagePath = ""

    private var specie: String? = null
    private var duration: Long = 0
    var roiStages: String = "stage1"
    var recordingSmallTree = false
    var treeType: String = BIG_AND_SMALL_TREES

    var measurementDone = false

    val tmSummaryInfoMap = hashMapOf<String, String>()

    // Default values
    private val additionalDataConfig = hashMapOf(
        MANUAL_DBH to LABEL_OPTIONAL,
        MANUAL_HEIGHT to LABEL_OPTIONAL,
        TREE_HEALTH to LABEL_OPTIONAL,
        TREE_COMMENT to LABEL_OPTIONAL,
    )

    fun getAssetManager(): AssetManager? {
        return context.assets
    }

    fun measureTreeDiameter(uri: Uri) {
        val imageStream = context.contentResolver.openInputStream(uri)
        val selectedImage = BitmapFactory.decodeStream(imageStream)
        val bitmapImg = selectedImage.copy(Bitmap.Config.ARGB_8888, true)
        val sharedPreference =
            context.getSharedPreferences("CAMERA_OVERLAY_SIZE", Context.MODE_PRIVATE)
        measureJob = viewModelScope.launch(dispatcher.io()) {
            try {
                val roiX: Float = sharedPreference.getFloat("cardAreaX", 0.0F)
                val roiY: Float = sharedPreference.getFloat("cardAreaY", 0.0F)
                val roiX2: Float = sharedPreference.getFloat("cardAreaX2", 0.0F)
                val roiY2: Float = sharedPreference.getFloat("cardAreaY2", 0.0F)

                val src = Mat()

                Utils.bitmapToMat(bitmapImg, src)
                Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR)

                val start = System.nanoTime()
                val treeData: String =
                    getTreeData(src.nativeObjAddr, roiX, roiY, roiX2, roiY2, roiStages)
                val dataList: List<String> = listOf(*treeData.split("*").toTypedArray())
                _cardPolygon.postValue(dataList[3])
                _treeLines.postValue(dataList[5])
                _treeDiameter.postValue(dataList[1].toDouble())

                val end = System.nanoTime()
                measurementDone = true
                duration = (end - start) / 1000000

            } catch (e: Exception) {
                Log.e("Stormy ", "Tree Measure Error: ${e.localizedMessage}")
            }
        }
    }

    fun cancelTreeMeasuring() {
        viewModelScope.launch(dispatcher.io()) {
            if (measureJob.isActive) {
                measureJob.cancelAndJoin()
            }
        }
    }

    fun resetDiameter() {
        _treeDiameter.value = 0.0
        _cardPolygon.value = ""
        _treeLines.value = ""
        roiStages = "stage1";
    }

    fun resetAttempts() {
        numOfAttempts = 0
    }

    fun incrementAttempts() {
        numOfAttempts += 1
    }

    fun createAdHocTreeMeasurement(
        imagePath: String,
        location: UserLocation?,
        gpsAccuracy: Float,
        diameter: Double?,
        flashlight: Boolean,
        treePolygon: String?,
        cardPolygon: String?,
        algoStages: String?,
        inventoryId: Long?,
    ) {
        if (imagePath.isNotBlank()) {
            this.imagePath = imagePath
            viewModelScope.launch(dispatcher.io()) {
                withContext(dispatcher.io()) {
                    createTreeMeasurementObject(
                        diameter,
                        treePolygon,
                        cardPolygon,
                        algoStages,
                        inventoryId
                    )
                }
                withContext(dispatcher.io()) {
                    createTMPhotoObject(imagePath, location, gpsAccuracy, flashlight)
                }
            }
        }
    }

    fun getNextAdhocActivities() {
        viewModelScope.launch {
            dbRepository.getPlannedActivities("adhoc").collect { entities ->
                adhocActivityList =
                    entityMapper.getListOfActivitiesFromEntities(entities)
                for (item in adhocActivityList) {
                    item.uuid = UUID.randomUUID()
                }
                _adhocActivities.postValue(adhocActivityList)
            }
        }
    }

    private suspend fun createTreeMeasurementObject(
        diameter: Double?,
        treePolygon: String?,
        cardPolygon: String?,
        algoStages: String?,
        inventoryId: Long?,
    ) {
        val activity = dbRepository.getActivity(currentActivityId)
        treeMeasurement = TreeMeasurement(
            measurementId = null,
            measurementUUID = UUID.randomUUID(),
            activityId = currentActivityId,
            inventoryId = inventoryId,
            activityUUID = currentActivityUUID ?: activity.activityUUID,
            numberOfAttempts = numOfAttempts,
            treeDiameter = diameter,
            specie = specie,
            duration = duration,
            treePolygon = treePolygon,
            cardPolygon = cardPolygon,
            measurementType = currentActivityType,
            carbonDioxide = null,
            manualDiameter = null,
            stages = algoStages,
            treeHealth = null,
            treeHeightMm = null,
            comment = null,
        )
    }

    private fun createTMPhotoObject(
        imagePath: String,
        location: UserLocation?,
        gpsAccuracy: Float,
        flashlight: Boolean
    ) {
        treePhoto = Photo(
            surveyId = null,
            treeMeasurementId = 0,
            photoUUID = UUID.randomUUID(),
            measurementUUID = treeMeasurement?.measurementUUID,
            imagePath = imagePath,
            imageType = "treeMeasurement",
            metaData = PhotoMetaData(
                timestamp = Timestamp(System.currentTimeMillis()).toString(),
                resolution = null,
                gpsCoordinates = location,
                gpsAccuracy = gpsAccuracy,
                stepsTaken = null,
                azimuth = null,
                cameraOrientation = null,
                flashLight = flashlight
            )
        )
    }

    fun getImagePath(): String {
        return treePhoto?.imagePath ?: imagePath
    }

    fun getActivityId(): Long {
        return treeMeasurement?.activityId!!
    }

    fun setSpecie(specie: String) {
        treeMeasurement?.specie = specie
    }

    fun setMeasurementType(measurementCode: String) {
        treeMeasurement?.measurementType = measurementCode
    }

    fun setCO2(carbonDioxide: String?) {
        treeMeasurement?.carbonDioxide = carbonDioxide
    }

    fun saveTreeMeasurement(addToInventory: Boolean = false) {
        viewModelScope.launch(dispatcher.io()) {
            val measurementId = withContext(dispatcher.io()) {
                treeMeasurement?.let {
                    if (addToInventory) {
                        forestInventoryRepository.addForestTreeMeasurement(it)
                    } else {
                        dbRepository.insertTreeMeasurement(it)
                    }
                }
            }
            treePhoto?.treeMeasurementId = measurementId
            treePhoto?.let { dbRepository.insertTreePhoto(it) }
        }
    }

    fun markActivityAsCompleted(activityId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = treeMeasurementDao.getTreeMeasurementActivityCount(activityId)
            dbRepository.markActivityAsCompleted(activityId, count)
        }
    }

    private external fun getTreeData(
        mat: Long,
        roiX: Float,
        roiY: Float,
        roiX2: Float,
        roiY2: Float,
        stage: String
    ): String

    fun calculateTreeCarbon(): Double {
        // Not yet implemented
        return 0.0
    }

    fun specieNotSpecified() = treeMeasurement?.specie == null

    fun processForestInventorySummary(inventoryId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            val measurements = forestInventoryRepository.getInventoryMeasurements(inventoryId)

            var numberOfTrees = 0
            var diameterSum = 0.0
            for (measurement in measurements) {
                if (measurement.specie != null && measurement.treeDiameter != null) {
                    numberOfTrees++
                    diameterSum += measurement.treeDiameter
                }
            }
            val averageDiameter = diameterSum / numberOfTrees

            tmSummaryInfoMap.apply {
                set("Total Trees", numberOfTrees.toString())
                set("Avg. Diameter", "${String.format("%.2f", averageDiameter)} mm")
            }
        }
    }

    fun completeForestInventoryActivity(inventoryId: Long) {
        viewModelScope.launch(dispatcher.io()) {
            forestInventoryRepository.markForestInventoryAsComplete(inventoryId)
        }
    }

    external fun init()

    external fun cleanup()

    fun initializeMeasurementObject() {
        viewModelScope.launch(dispatcher.default()) {
            try {
                init()
                _hasInitializedMeasurementObject.postValue(true);
            } catch (e: Exception) {
                // Init failed
            }
        }
    }

    fun checkIfSmallTreeIsClear(imageUri: Uri) {
        // CHANGE: Not yet implemented, marking image as clear for now.
        _smallTreeIsClear.postValue(true)
    }

    fun getTreeSpecies() {
        viewModelScope.launch {
            val speciesList = forestInventoryRepository.getTreeSpecies()
            val activity = dbRepository.getActivity(currentActivityId)
            val specieCodes = activity.configuration?.specieCodes ?: listOf()
            val filteredSpeciesList = speciesList.filter {
                specieCodes.contains(it.code)
            }
            _treeSpecies.postValue(filteredSpeciesList)
        }
    }

    fun getAdditionalDataConfig() {
        viewModelScope.launch {
            val activity = dbRepository.getActivity(currentActivityId)
            activity.configuration?.apply {
                additionalDataConfig[MANUAL_DBH] = manualDBH ?: LABEL_OPTIONAL
                additionalDataConfig[MANUAL_HEIGHT] = manualHeight ?: LABEL_OPTIONAL
                additionalDataConfig[TREE_HEALTH] = treeHealth ?: LABEL_OPTIONAL
                additionalDataConfig[TREE_COMMENT] = measurementComment ?: LABEL_OPTIONAL
            }
            _additionalDataConfigLoaded.postValue(true)
        }
    }

    fun getUserLanguage(): String = preferences.getString(SELECTED_LANGUAGE, "en")!!

    fun getHiddenAdditionalDataInputFields() =
        additionalDataConfig.entries.filter { it.value == LABEL_HIDDEN }.map { it.key }.toList()

    fun getOptionalAdditionalDataInputFields() =
        additionalDataConfig.entries.filter { it.value == LABEL_OPTIONAL }.map { it.key }.toList()

    fun getRequiredAdditionalDataInputFields() =
        additionalDataConfig.entries.filter { it.value == LABEL_REQUIRED }.map { it.key }.toList()

    companion object {
        var adhocActivityList = listOf<Activity>()
        var currentActivityId: Long = -1
        var currentActivityUUID: UUID? = null
        var currentActivityType: String? = ""

        private const val LABEL_OPTIONAL = "optional"
        private const val LABEL_HIDDEN = "hidden"
        private const val LABEL_REQUIRED = "required"
    }
}
