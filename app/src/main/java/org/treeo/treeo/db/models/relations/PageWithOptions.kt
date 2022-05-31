package org.treeo.treeo.db.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.treeo.treeo.db.models.OptionEntity
import org.treeo.treeo.db.models.PageEntity

data class PageWithOptions(
    @Embedded val page: PageEntity,
    @Relation(parentColumn = "pageId", entityColumn = "pageId")
    val options: List<OptionEntity>
)