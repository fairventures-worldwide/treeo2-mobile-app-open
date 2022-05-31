package org.treeo.treeo.models

import com.google.gson.annotations.SerializedName
import org.treeo.treeo.network.models.ActivityDTO


data class UserActivity(
    @SerializedName("plannedActivites")
    var plannedActivities: List<ActivityDTO>
)
