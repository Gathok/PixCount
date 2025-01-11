package de.gathok.pixcount.manageColors

import de.gathok.pixcount.db.PixColor

data class ManageColorsState(
    val colorList: List<PixColor> = emptyList(),
)
