package org.treeo.treeo.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UploadQueue")
data class UploadQueueEntity(
    var activityId: Long,
    var activityData: String,
    val dataBytes: Int,
    var type: String,
    var forUpload: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
