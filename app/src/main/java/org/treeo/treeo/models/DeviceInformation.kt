package org.treeo.treeo.models

data class DeviceInformation(
    val advertisingID: String,
    val androidVersion: String,
    val cameraInformation: String,
    val freeRAM: String,
    val manufacturer: String,
    val model: String,
    val screenResolution: String,
    val sensors: List<String>,
    val totalRAM: String
)
