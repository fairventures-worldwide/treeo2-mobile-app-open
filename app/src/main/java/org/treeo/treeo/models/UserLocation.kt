package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserLocation(
    val lat: String?,
    val lng: String?
) : Parcelable
