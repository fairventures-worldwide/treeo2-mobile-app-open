package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ForestInventoryEntity(
    val activityId: Long,
    val status: Int,
) {
    @PrimaryKey(autoGenerate = true)
    var forestInventoryId: Long = 0

    companion object {
        const val STATUS_STARTED = 0
        const val STATUS_FINISHED = 1
    }
}
