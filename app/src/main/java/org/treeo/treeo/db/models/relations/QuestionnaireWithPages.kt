package org.treeo.treeo.db.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.treeo.treeo.db.models.PageEntity
import org.treeo.treeo.db.models.QuestionnaireEntity

data class QuestionnaireWithPages(
    @Embedded val questionnaire: QuestionnaireEntity,
    @Relation(parentColumn = "questionnaireId", entityColumn = "questionnaireId")
    val pages: List<PageEntity>
)