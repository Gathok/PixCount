package de.gathok.pixcount.manageColors

import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import org.mongodb.kbson.ObjectId

data class ManageColorsState(
    val colorList: List<PixColor> = emptyList(),
    val allCategories: List<PixCategory> = emptyList(),
    val colorUses: Map<ObjectId, Int> = emptyMap(),
)
