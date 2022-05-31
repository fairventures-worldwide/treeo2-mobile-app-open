package org.treeo.treeo.db.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.treeo.treeo.db.models.LandSurveyEntity
import org.treeo.treeo.db.models.PhotoEntity

class LandSurveyWithPhotos(
    @Embedded val landSurvey: LandSurveyEntity,
    @Relation(parentColumn = "surveyId", entityColumn = "surveyId")
    val photos: List<PhotoEntity>
)