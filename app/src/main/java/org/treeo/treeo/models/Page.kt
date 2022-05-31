package org.treeo.treeo.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Page(
    val pageId: Long,
    val pageType: String,
    val questionCode: String,
    var header: @RawValue Map<String, Any>,
    val description: @RawValue Map<String, Any>,
    var options: List<Option>?,
    val mandatory: Boolean
) : Parcelable
