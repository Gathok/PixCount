package de.gathok.pixcount

import de.gathok.pixcount.dbObjects.PixCategory
import de.gathok.pixcount.dbObjects.PixColor
import de.gathok.pixcount.dbObjects.PixList

data class MainState(
    val curPixList: PixList? = null,
    val curCategories: List<PixCategory> = emptyList(),
    val allPixLists: List<PixList> = emptyList(),
    val colorList: List<PixColor> = emptyList(),
)
