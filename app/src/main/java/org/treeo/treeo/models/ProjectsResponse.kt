package org.treeo.treeo.models
import com.google.gson.annotations.SerializedName


data class ProjectsResponse (
    @SerializedName("data")
    val data: List<TreeoProject>,
    @SerializedName("message")
    val message: String
)