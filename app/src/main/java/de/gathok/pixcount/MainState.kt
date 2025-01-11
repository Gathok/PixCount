package de.gathok.pixcount

import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList

data class MainState(
    val curPixList: PixList? = null,
    val curCategories: List<PixCategory> = emptyList(),
    val allPixLists: List<PixList> = emptyList(),
    val colorList: List<PixColor> = emptyList(),
)
