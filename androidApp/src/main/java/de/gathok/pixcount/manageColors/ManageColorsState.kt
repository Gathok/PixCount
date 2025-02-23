package de.gathok.pixcount.manageColors

import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.manageColors.util.ManageColorsUndoActions
import org.mongodb.kbson.ObjectId
import java.util.Stack

data class ManageColorsState(
    val colorList: List<PixColor> = emptyList(),
    val allCategories: List<PixCategory> = emptyList(),
    val colorUses: Map<ObjectId, Int> = emptyMap(),
    val undoStack: Stack<Pair<ManageColorsUndoActions, List<Any?>>> = Stack()
)
