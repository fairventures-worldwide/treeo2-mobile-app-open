package org.treeo.treeo.util

import androidx.lifecycle.MutableLiveData
import org.treeo.treeo.BuildConfig

val BASE_URL = if (BuildConfig.DEBUG) "https://api.dev.treeo.one/" else "https://api.treeo.org/"

const val SHARED_PREFERENCES_NAME = "shared_preferences"
val errors = MutableLiveData<String>()

const val TREEO_DATABASE_NAME = "Treeo Database"

const val OFFLINE_WITH_DATA_TO_SYNC = 1
const val OFFLINE_WITHOUT_DATA_TO_SYNC = 2
const val ONLINE_WITH_DATA_TO_SYNC = 3
const val ONLINE_WITH_SYNC_IN_PROGRESS = 4
const val ONLINE_WITHOUT_DATA_TO_SYNC = 5
const val ONLINE_SYNC_SUCCESSFUL = 6
const val SYNC_TAG = "Offline Synchronization"
const val SUCCESSFUL_UPLOAD_ACKNOWLEDGED = "QUEUE_UPLOAD_RUNNING"
const val QUEUE_UPLOADED_BEFORE = "QUEUE_UPLOADED_BEFORE"
const val DATE_OF_LAST_SYNC = "dateOfLastSync"

const val SOIL_PHOTO = "soil_photo"
const val LAND_PHOTO = "land_photo"
const val PRE_QUESTIONNAIRE = "pre_questionnaire"

const val ACTIVITY_TYPE_QUESTIONNAIRE = "questionnaire"
const val ACTIVITY_TYPE_LAND_SURVEY = "land_survey"
const val ACTIVITY_TYPE_TREE_MONITORING = "tree_monitoring"
const val SUB_ACTIVITY_TYPE_TREE_MEASUREMENT = "tree_measurement"
const val TOTAL_BYTES_TO_SYNC = "TOTAL_BYTES_TO_SYNC"
const val ACTIVITY_FOREST_INVENTORY = "tree_survey"

const val USER_NAME = "USER_NAME"
const val USER_PHONE_NUMBER = "USER_PHONE_NUMBER"

const val PAGE_TYPE_SHORT_TEXT = "text_short"
const val PAGE_TYPE_LONG_TEXT = "text_long"
const val TREE_MEASUREMENT_SUCCESSFUL = "tree_measurement_auto"
const val TREE_MEASUREMENT_UNSUCCESSFUL = "tree_measurement_auto_not_detected"
const val TREE_MEASUREMENT_REJECTED = "tree_measurement_auto_rejected"
const val SMALL_TREE_ACCEPTED = "tree_evidence"
const val MEASUREMENT_SKIPPED = "tree_evidence"
const val SMALL_TREE_REJECTED = "tree_evidence_rejected"

const val MEASUREMENT = "measurement"
const val FOREST_INVENTORY = "forest inventory"
const val ADHOC_ACTIVITY_ID = "adhocActivityId"
const val REQUEST_CODE_MEASURE_TREE = 3456
const val SELECTED_LANGUAGE = "Selected Language"

const val INVENTORY_ID = "inventoryId"
const val SKIP_MEASUREMENT = "skipMeasurement"
const val TREE_TYPE = "treeType"
const val BIG_TREES = "big_tree"
const val SMALL_TREES = "small_tree"
const val BIG_AND_SMALL_TREES = "small_big_tree"
const val IS_FROM_WHOLE_FIELD_BOTTOM_NAV_TAP = "isFromBottomNavTap"
const val ACTIVITY_TYPE = "activityType"

// Additional Data Inputs
const val TREE_HEALTH = "treeHealth"
const val MANUAL_DBH = "manualDBH"
const val MANUAL_HEIGHT = "manualHeight"
const val TREE_COMMENT = "comment"
