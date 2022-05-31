package org.treeo.treeo.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.treeo.treeo.models.DeviceInformation
import java.io.File
import java.io.IOException

@ExperimentalCoroutinesApi
class DeviceInfoUtils(var context: Context, var dispactcher: IDispatcherProvider) {

    suspend fun getDeviceInformation(
        activityManager: ActivityManager,
        sensorManager: SensorManager,
        packageManager: PackageManager,
        cameraManager: CameraManager
    ): DeviceInformation {

        var advertisingID: String
        var androidVersion: String
        var cameraInformation: String
        var card: String
        var freeRAM: String
        var manufacturer: String
        var model: String
        var screenResolution: String
        var sensors: List<String>
        var storage: String
        var totalRAM: String

        model = Build.MODEL
        manufacturer = Build.MANUFACTURER
        androidVersion = Build.VERSION.RELEASE

        val converter = 0x100000L
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        val availableMegs: Long = mi.availMem / converter
        val totalMegs: Long = mi.totalMem / converter

        freeRAM = "${availableMegs}MB"
        totalRAM = "${totalMegs}MB"

        val internalStatFs = StatFs(Environment.getRootDirectory().getAbsolutePath())
        var internalTotal = 0L

        val externalStatFs = StatFs(Environment.getExternalStorageDirectory().getAbsolutePath())
        var externalTotal = 0L

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                internalTotal =
                    internalStatFs.blockCountLong * internalStatFs.blockSizeLong / (converter)
                if (isSdCardOnDevice(context)) {
                    externalTotal =
                        externalStatFs.blockCountLong * externalStatFs.blockSizeLong / (converter)
                }
            } else {
                internalTotal =
                    internalStatFs.blockCount.toLong() * internalStatFs.blockSize.toLong() / (converter)

                if (isSdCardOnDevice(context)) {
                    externalTotal =
                        externalStatFs.blockCount.toLong() * externalStatFs.blockSize.toLong() / (converter)

                }
            }

            storage = "${internalTotal}MB"
            card = "${externalTotal}MB"

        } else {
            storage = "protected"
            card = "protected"
        }

        val deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val sensorsMutable = mutableListOf<String>()
        deviceSensors.forEach { sensor ->
            sensorsMutable.add(sensor.name)
        }
        sensors = sensorsMutable

        advertisingID = determineAdvertisingInfo()

        screenResolution = getScreenResolution()

        cameraInformation = getCameraResolution(cameraManager)


        return DeviceInformation(
            advertisingID,
            androidVersion,
            cameraInformation,
            freeRAM,
            manufacturer,
            model,
            screenResolution,
            sensors,
            totalRAM
        )
    }

    fun isSdCardOnDevice(context: Context): Boolean {
        val storages: Array<File?> = context.getExternalFilesDirs(null)
        return if (storages.size > 1 && storages[0] != null && storages[1] != null) true else false
    }


    private fun isSystemPackage(appInfo: ApplicationInfo): Boolean {
        return appInfo.flags and ApplicationInfo.FLAG_SYSTEM !== 0
    }

    private fun getScreenResolution(): String {

        return "{${Resources.getSystem().getDisplayMetrics().widthPixels}" +
                ",${Resources.getSystem().getDisplayMetrics().heightPixels}}"
    }

    private suspend fun determineAdvertisingInfo() = CoroutineScope(dispactcher.io()).async {
        var id = ""
        try {
            val advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            // Check this in case the user disabled it from settings
            if (!advertisingIdInfo.isLimitAdTrackingEnabled) {
                id = advertisingIdInfo.id
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        }
        return@async id
    }.await()

    private fun getCameraResolution(cameraManager: CameraManager): String {
        var maxWidth = 0
        var maxHeight = 0
        var resoulution_in_MP = 0f
        for (cameraId in cameraManager.cameraIdList) {
            try {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lens_facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

                if (lens_facing != null && lens_facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val streamConfigurationMap = cameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )
                    val outputSizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)

                    if (outputSizes != null) {
                        for (size in outputSizes) {
                            if (size.width > maxWidth) {
                                maxWidth = size.width
                            }
                            if (size.height > maxHeight) {
                                maxHeight = size.height
                            }
                        }
                    }

                    resoulution_in_MP = (maxWidth * maxHeight) / 1000000f
                }

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        return String.format("%.1f", resoulution_in_MP)
    }
}

