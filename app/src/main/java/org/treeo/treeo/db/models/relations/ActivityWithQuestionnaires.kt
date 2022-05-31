package org.treeo.treeo.db.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.treeo.treeo.db.models.ActivityEntity
import org.treeo.treeo.db.models.QuestionnaireEntity

data class ActivityWithQuestionnaires(
    @Embedded val activity: ActivityEntity,
    @Relation(parentColumn = "activityId", entityColumn = "activityId")
    val questionnaires: List<QuestionnaireEntity>
)