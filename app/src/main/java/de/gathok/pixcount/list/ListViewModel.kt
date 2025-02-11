package de.gathok.pixcount.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.MyApp
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.list.util.ListUndoActions
import de.gathok.pixcount.util.Months
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mongodb.kbson.ObjectId
import java.util.Stack

class ListViewModel: ViewModel() {

    private val realm = MyApp.realm

    private val _allPixLists = realm
        .query<PixList>()
        .asFlow()
        .map { results ->
            results.list.associateBy { it.id }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = realm.query<PixList>().find().associateBy { it.id }
        )

    private val _colorList = realm
        .query<PixColor>()
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow(ListState())

    val state = combine(
        _state, _allPixLists.onStart { emit(emptyMap()) }, _colorList.onStart { emit(emptyList()) }
    ) { state, allPixLists, colorList ->
        val currentPixList = allPixLists[state.curPixListId]
        state.copy(
            colorList = colorList,
            curPixList = currentPixList,
            curCategories = currentPixList?.categories ?: emptyList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ListState()
    )

    fun setPixListId(pixListId: ObjectId?) {
        _state.value = _state.value.copy(
            curPixListId = pixListId,
            undoStack = Stack(),
        )
    }

    fun getInvalideNames(): List<String> {
        return _allPixLists.value.values.map { it.name }
    }

    fun updatePixListName(newName: String, undo: Boolean = false) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(state.value.curPixList ?: throw IllegalArgumentException("curPixList is invalid or outdated"))
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!undo) {
                    addToStack(
                        ListUndoActions.UPDATE_PIX_LIST_NAME,
                        listOf(managedPixList.name)
                    )
                }
                managedPixList.name = newName

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // PixCategory functions -------------------------------------------------
    fun createPixCategory(name: String, color: PixColor, pixList: PixList, undo: Boolean = false) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(color)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                val managedCategory = copyToRealm(
                    PixCategory(name = name, color = managedColor))
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.add(managedCategory))
                    throw IllegalArgumentException("Category already exists")

                copyToRealm(managedPixList, UpdatePolicy.ALL)

                if (!undo) {
                    addToStack(
                        ListUndoActions.UNDO_CREATE_PIX_CATEGORY,
                        listOf(managedCategory.id, managedPixList)
                    )
                }
            }
        }
    }

    fun updatePixCategory(category: PixCategory, newName: String?, newColor: PixColor?, undo: Boolean = false) {
        viewModelScope.launch {
            realm.write {
                val managedCategory = findLatest(category)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                if (!undo) {
                    addToStack(
                        ListUndoActions.UPDATE_PIX_CATEGORY,
                        listOf(managedCategory, managedCategory.name, managedCategory.color)
                    )
                }
                if (newName != null) {
                    managedCategory.name = newName
                }
                if (newColor != null) {
                    val managedColor = findLatest(newColor)
                        ?: throw IllegalArgumentException("color is invalid or outdated")
                    managedCategory.color = copyToRealm(managedColor, UpdatePolicy.ALL)
                }

                copyToRealm(managedCategory, UpdatePolicy.ALL)
            }
        }
    }

    fun deletePixCategory(category: PixCategory, pixList: PixList, undo: Boolean = false) {
        viewModelScope.launch {
            val removedEntries: List<Pair<Months, Int>> = pixList.deleteCategory(category)
            realm.write {
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                val managedCategory = findLatest(category)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                delete(managedCategory)
                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }

            if (!undo) {
                addToStack(
                    ListUndoActions.UNDO_DELETE_PIX_CATEGORY,
                    listOf(category.id, category.name, category.color, pixList, removedEntries)
                )
            }
        }
    }

    // PixEntry functions ---------------------------------------------------
    fun setPixEntry(day: Int, month: Months, category: PixCategory?, pixList: PixList, undo: Boolean = false) {
        viewModelScope.launch {
            realm.write {
                val managedCategory =
                    if (category == null) PixCategory()
                    else findLatest(category)
                        ?: throw IllegalArgumentException("category is invalid or outdated")

                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!undo) {
                    addToStack(
                        ListUndoActions.SET_PIX_ENTRY,
                        listOf(day, month, managedPixList.entries?.getEntry(day, month), pixList)
                    )
                }
                managedPixList.entries?.setEntry(day, month, managedCategory)

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // Update Category Order ------------------------------------------------
    fun updateCategoryOrder(newOrder: List<PixCategory>, pixList: PixList, undo: Boolean = false) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!undo) {
                    addToStack(
                        ListUndoActions.UPDATE_CATEGORY_ORDER,
                        listOf(managedPixList.categories.toList(), pixList)
                    )
                }
                managedPixList.categories.clear()
                newOrder.forEach { category ->
                    val managedCategory = findLatest(category)
                        ?: throw IllegalArgumentException("category is invalid or outdated")
                    managedPixList.categories.add(managedCategory)
                }
                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // Undo operations --------------------------------------------------------
    private fun addToStack(action: ListUndoActions, params: List<Any?>) {
        _state.value.undoStack.add(action to params)
    }

    private fun undoCreatePixCategory(categoryId: ObjectId, pixList: PixList) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                val managedCategory = managedPixList.categories.find { it.id == categoryId }
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                managedPixList.categories.remove(managedCategory)

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    private fun undoDeletePixCategory(id: ObjectId, name: String, color: PixColor, pixList: PixList, entries: List<Pair<Months, Int>>) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(color)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                val managedCategory = copyToRealm(
                    PixCategory(id = id, name = name, color = managedColor))
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.add(managedCategory))
                    throw IllegalArgumentException("Category already exists")

                entries.forEach { (month, day) ->
                    managedPixList.entries?.setEntry(day, month, managedCategory)
                }

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun undoLastAction() {
        if (_state.value.undoStack.isEmpty()) return
        _state.value.undoStack.pop().let { (action, params) ->
            when (action) {
                ListUndoActions.UPDATE_PIX_LIST_NAME -> {
                    updatePixListName(params[0] as String, true)
                }
                ListUndoActions.UNDO_CREATE_PIX_CATEGORY -> {
                    undoCreatePixCategory(
                        params[0] as ObjectId,
                        params[1] as PixList
                    )
                }
                ListUndoActions.UPDATE_PIX_CATEGORY -> {
                    updatePixCategory(
                        params[0] as PixCategory,
                        params[1] as String,
                        params[2] as PixColor,
                        true
                    )
                }
                ListUndoActions.UNDO_DELETE_PIX_CATEGORY -> {
                    undoDeletePixCategory(
                        params[0] as ObjectId,
                        params[1] as String,
                        params[2] as PixColor,
                        params[3] as PixList,
                        params[4] as List<Pair<Months, Int>>
                    )
                }
                ListUndoActions.SET_PIX_ENTRY -> {
                    setPixEntry(
                        params[0] as Int,
                        params[1] as Months,
                        params[2] as PixCategory?,
                        params[3] as PixList,
                        true
                    )
                }
                ListUndoActions.UPDATE_CATEGORY_ORDER -> {
                    updateCategoryOrder(
                        params[0] as List<PixCategory>,
                        params[1] as PixList,
                        true
                    )
                }
            }
        }
    }
}