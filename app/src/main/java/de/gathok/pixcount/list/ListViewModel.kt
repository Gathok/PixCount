package de.gathok.pixcount.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.MyApp
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
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

class ListViewModel: ViewModel() {

    private val realm = MyApp.realm

    private val _allPixLists = realm
        .query<PixList>()
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = realm.query<PixList>().find().toList()
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
        _state, _allPixLists.onStart { emit(emptyList()) }, _colorList.onStart { emit(emptyList()) }
    ) { state, allPixLists, colorList ->
        val currentPixList = allPixLists.find { it.id == state.curPixListId }
        state.copy(
            colorList = colorList,
            curPixList = currentPixList,
            curCategories = currentPixList?.categories?.toList() ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ListState()
    )

    fun setPixListId(pixListId: ObjectId?) {
        _state.value = _state.value.copy(
            curPixListId = pixListId,
        )
    }

    fun getInvalideNames(): List<String> {
        return _allPixLists.value.map { it.name }
    }

    fun updatePixListName(newName: String) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(state.value.curPixList ?: throw IllegalArgumentException("curPixList is invalid or outdated"))
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                managedPixList.name = newName

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // PixCategory functions -------------------------------------------------
    fun createPixCategory(name: String, color: PixColor, pixList: PixList) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(color)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                val newCategory = PixCategory(name = name, color = managedColor)
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.add(newCategory))
                    throw IllegalArgumentException("Category already exists")

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    fun updatePixCategory(category: PixCategory, newName: String?, newColor: PixColor?) {
        viewModelScope.launch {
            realm.write {
                val managedCategory = findLatest(category)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
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

    fun deletePixCategory(category: PixCategory, pixList: PixList) {
        viewModelScope.launch {
            pixList.deleteCategory(category)
            realm.write {
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // PixEntry functions ---------------------------------------------------
    fun setPixEntry(day: Int, month: Months, category: PixCategory?, pixList: PixList) {
        viewModelScope.launch {
            realm.write {
                val managedCategory =
                    if (category == null) PixCategory()
                    else findLatest(category)
                        ?: throw IllegalArgumentException("category is invalid or outdated")

                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                managedPixList.entries?.setEntry(day, month, managedCategory)

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // New: Update category order after a drag and drop operation.
    fun updateCategoryOrder(newOrder: List<PixCategory>, pixList: PixList) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
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
}