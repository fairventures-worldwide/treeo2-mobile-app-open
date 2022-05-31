package org.treeo.treeo.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import org.treeo.treeo.R
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.*
import org.treeo.treeo.models.Activity
import org.treeo.treeo.models.Country
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*
import java.util.concurrent.TimeUnit

fun getErrorMessageFromJson(response: String): String {
    val obj = JSONObject(response)
    val gson = Gson()

    return when (obj["message"]) {
        is JSONArray -> {
            val messageList =
                gson.fromJson(obj["message"].toString(), List::class.java)
            messageList[0].toString()
        }
        else -> {
            obj["message"].toString()
        }
    }
}

fun getCountries(): MutableList<Country> {
    return mutableListOf(
        Country(R.drawable.uganda_flag, "Uganda", "+256"),
        Country(R.drawable.indonesia_flag, "Indonesia", "+62"),
        Country(R.drawable.czech_republic_flag, "Czech Republic", "+420"),
        Country(R.drawable.germany_flag, "Germany", "+49"),
        Country(R.drawable.card_bg, "Other", "+")
    )
}

fun closeKeyboard(view: View, context: Context) {
    val imm =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun showView(view: View) {
    view.visibility = View.VISIBLE
}

fun hideView(view: View) {
    view.visibility = View.GONE
}

fun enableView(view: View, status: Boolean = true) {
    view.isEnabled = status
}

fun disableView(view: View) {
    view.isEnabled = false
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var result = false
    connectivityManager.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            it.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?.apply {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                        else -> false
                    }
                }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            result = networkInfo != null && networkInfo.isAvailable
        }
    }
    return result
}

fun SharedPreferences.getDeviceInfoNeedStatus(context: Context): Boolean {
    return getBoolean(context.getString(R.string.device_info_saved), false)
}

fun setDeviceInfoNeedStatus(
    sharedPrefs: SharedPreferences,
    context: Context
) {
    with(sharedPrefs.edit()) {
        putBoolean(context.getString(R.string.device_info_saved), true)
        apply()
    }
}

fun SharedPreferences.getAccessToken(context: Context): String {
    return getString(context.getString(R.string.user_token), "").toString()
}

fun SharedPreferences.getUserRefreshToken(): String {
    return getString("refresh_token", "").toString()
}

fun isNumberLengthValid(phoneNumber: String): Boolean {
    return phoneNumber.length >= 12
}

fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

fun humanReadableByteCountSI(dataBytes: Int): String {
    var bytes = dataBytes
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val characterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        characterIterator.next()
    }
    return String.format(
        "%.1f %cB",
        bytes / 1000.0,
        characterIterator.current()
    )
}

fun List<UploadQueueEntity>.calculateBytesLeftToUploadInQueue(): Int {
    var totalBytes = 0
    forEach {
        totalBytes += it.dataBytes
    }
    return totalBytes
}

fun Fragment.getCameraOutputDirectory(): File {
    val contextWrapper = ContextWrapper(requireActivity().applicationContext)
    val directory = contextWrapper.getDir("imageDir", Context.MODE_PRIVATE)

    val fileNameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    return File(
        directory,
        SimpleDateFormat(
            fileNameFormat,
            Locale.US
        ).format(System.currentTimeMillis())
    ).apply { mkdir() }
}


fun getRemaining(leTime: String): String {

    val lete = leTime.split('.')[0].split('T').joinToString(" ").toDate().formatTo("yyyy-MM-dd")

    val formatter = SimpleDateFormat("yyyy-MM-dd")
    val datee = formatter.parse(lete)

    val diffInMillisec: Long = datee.time - formatter.parse(formatter.format(Date())).time

    val diffInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMillisec)

    if (diffInDays < 0) {
        return "Past due"
    }


    if (diffInDays <= 7) {
        return "$diffInDays ${if (diffInDays.toInt() == 1) "day" else "days"} left"
    }

    if (diffInDays in 8..29) {
        val weeks = (diffInDays / 7).toInt()
        val days = (diffInDays % 7).toInt()

        if (days > 0) {
            return "$weeks ${if (weeks == 1) "week" else "weeks"} and $days ${if (days == 1) "day" else "days"} left"
        }
        if (
            days == 0) {
            return "$weeks ${if (weeks == 1) "week" else "weeks"} left"
        }
    }

    if (diffInDays > 29) {

        val months = (diffInDays / 30).toInt()
        val weeks = ((diffInDays % months) / 7).toInt()
        val days = (diffInDays - (months * 30 + weeks * 7)).toInt()


        if (weeks > 0 && days > 0) {
            return "$months ${if (months == 1) "month" else "months"}, $weeks ${if (weeks == 1) "week" else "weeks"} and $days ${if (days == 1) "day" else "days"} left"
        }
        if (weeks > 0 && days == 0) {
            return "$months ${if (months == 1) "month" else "months"}, $weeks ${if (weeks == 1) "week" else "weeks"} left"
        }
        if (weeks == 0 && days > 0) {
            return "$months ${if (months == 1) "month" else "months"}, $days ${if (days == 1) "day" else "days"} left"
        }
        if (weeks > 0 && days > 0) {
            return "$months ${if (months == 1) "month" else "months"}, $weeks ${if (weeks == 1) "week" else "weeks"} and $days ${if (days == 1) "day" else "days"} left"
        }

    }
    return ""
}


fun String.toDate(
    dateFormat: String = "yyyy-MM-dd HH:mm:ss",
    timeZone: TimeZone = TimeZone.getTimeZone("UTC")
): Date {
    val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
    parser.timeZone = timeZone
    return parser.parse(this)
}

fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = timeZone
    return formatter.format(this)
}


fun createAdhocActivity(activityType: String): ActivityEntity {
    return ActivityEntity(
        activityUUID = UUID.randomUUID(),
        activityRemoteId = null,
        dueDate = null,
        inProgress = true,
        isComplete = false,
        isAdhoc = true,
        title = mapOf(
            "in" to "",
            "en" to "",
            "lg" to ""
        ),
        description = mapOf(
            "in" to "",
            "en" to "",
            "lg" to ""
        ),
        configuration = ActivityConfiguration(specieCodes = listOf(), null, null, null, null),
        type = null,
        status = null,
        measurementCount = 0,
        plot = null,
        startDate = Timestamp(System.currentTimeMillis()).toString(),
        endDate = "",
        syncDate = "",
        template = ActivityTemplate(
            templateRemoteId = 5,
            activityType = activityType,
            code = null,
            preQuestionnaireId = null,
            postQuestionnaireId = null,
            configuration = TemplateConfiguration(
                soilPhotoRequired = false,
                measurementType = BIG_AND_SMALL_TREES
            )
        )
    )
}

fun createNewAdhocActivityFromExistingActivity(activity: Activity): ActivityEntity {

    return ActivityEntity(
        activityUUID = UUID.randomUUID(),
        activityRemoteId = null,
        dueDate = null,
        inProgress = true,
        isComplete = false,
        isAdhoc = true,
        title = activity.title,
        description = activity.description,
        configuration = ActivityConfiguration(
            specieCodes = if (activity.configuration == null) listOf() else activity.configuration.specie_codes,
            null,
            null,
            null,
            null,
        ),
        type = activity.type,
        status = activity.status,
        measurementCount = 0,
        plot = activity.plot?.let {
            ActivityPlot(
                plotId = it.id,
                area = it.area,
                externalId = it.externalId,
                ownerID = it.ownerID,
                plotName = it.plotName,
                polygon = it.polygon
            )
        },
        startDate = Timestamp(System.currentTimeMillis()).toString(),
        endDate = "",
        syncDate = "",
        template = ActivityTemplate(
            templateRemoteId = 5,
            activityType = activity.template.activityType,
            code = activity.template.code,
            preQuestionnaireId = activity.template.preQuestionnaireId,
            postQuestionnaireId = activity.template.postQuestionnaireId,
            configuration = TemplateConfiguration(
                soilPhotoRequired = activity.template.configuration.soilPhotoRequired,
                measurementType = BIG_AND_SMALL_TREES
            )
        )
    )
}


fun getActivityGuideDrawableId(activityType: String): Int =
    if (activityType == ACTIVITY_TYPE_LAND_SURVEY)
        R.drawable.ic_land_survey
    else
        R.drawable.ic_tree_measurement

suspend fun LandSurveyDao.countActivityMeasurements(activityId: Long): Int {
    return countLandSurveysForActivity(activityId)
}

suspend fun TreeMeasurementDao.countActivityMeasurements(activityId: Long): Int {
    return countTreeMeasurementsForActivity(activityId)
}

fun <T> convertObjectToJson(model: T): String {
    return Gson().toJson(model)
}
