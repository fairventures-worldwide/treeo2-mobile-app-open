package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Option(
    val optionId: Long,
    val pageId: Long,
    val code: String,
    var isSelected: Boolean,
    var title: @RawValue Map<String, Any>
) : Parcelable