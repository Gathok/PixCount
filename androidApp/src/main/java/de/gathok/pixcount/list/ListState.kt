package de.gathok.pixcount.list

import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.list.util.ListUndoActions
import org.mongodb.kbson.ObjectId
import java.util.Stack

data class ListState(
    val curPixListId: ObjectId? = null,
    val curPixList: PixList? = null,
    val curCategories: List<PixCategory> = emptyList(),
    val colorList: List<PixColor> = emptyList(),
    val undoStack: Stack<Pair<ListUndoActions, List<Any?>>> = Stack(),
)
