package de.gathok.pixcount.list

import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import org.mongodb.kbson.ObjectId

data class ListState(
    val curPixListId: ObjectId? = null,
    val curPixList: PixList? = null,
    val curCategories: List<PixCategory> = emptyList(),
    val colorList: List<PixColor> = emptyList(),
)
