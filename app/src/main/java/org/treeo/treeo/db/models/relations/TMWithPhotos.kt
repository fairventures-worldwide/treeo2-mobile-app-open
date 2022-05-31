package org.treeo.treeo.db.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.treeo.treeo.db.models.PhotoEntity
import org.treeo.treeo.db.models.TMEntity

data class TMWithPhotos (
    @Embedded val treeMeasurement: TMEntity,
    @Relation(parentColumn = "treeMeasurementId", entityColumn = "treeMeasurementId")
    val photos: List<PhotoEntity>
    )
