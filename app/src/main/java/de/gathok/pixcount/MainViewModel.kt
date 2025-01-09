package de.gathok.pixcount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.dbObjects.PixCategory
import de.gathok.pixcount.dbObjects.PixColor
import de.gathok.pixcount.dbObjects.PixList
import de.gathok.pixcount.util.Months
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mongodb.kbson.BsonObjectId

class MainViewModel: ViewModel() {

    private val realm = MyApp.realm

    private val _allPixLists = realm
        .query<PixList>()
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

//    private val _colorList = realm TODO: Implement custom color query
//        .query<PixColor>()
//        .asFlow()
//        .map { results ->
//            results.list.toList()
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(),
//            initialValue = emptyList()
//        )

    private val _colorList = listOf(
        PixColor(name = "Peach", red = 1.0f, green = 0.87f, blue = 0.77f, alpha = 1.0f),
        PixColor(name = "Lemon Yellow", red = 1.0f, green = 0.97f, blue = 0.69f, alpha = 1.0f),
        PixColor(name = "Mint Green", red = 0.74f, green = 0.98f, blue = 0.79f, alpha = 1.0f),
        PixColor(name = "Sky Blue", red = 0.68f, green = 0.85f, blue = 0.90f, alpha = 1.0f),
        PixColor(name = "Lavender", red = 0.82f, green = 0.75f, blue = 0.93f, alpha = 1.0f),
        PixColor(name = "Dusty Pink", red = 0.91f, green = 0.75f, blue = 0.80f, alpha = 1.0f),
        PixColor(name = "Pale Orange", red = 1.0f, green = 0.85f, blue = 0.72f, alpha = 1.0f),
        PixColor(name = "Baby Blue", red = 0.68f, green = 0.90f, blue = 1.0f, alpha = 1.0f),
        PixColor(name = "Blush Pink", red = 1.0f, green = 0.82f, blue = 0.86f, alpha = 1.0f),
        PixColor(name = "Lilac", red = 0.91f, green = 0.78f, blue = 0.94f, alpha = 1.0f)
    )

    private val _state = MutableStateFlow(MainState())
    val state = combine(_state, _allPixLists) { state, allPixLists ->
        state.copy(
            allPixLists = allPixLists,
            curPixList = allPixLists.find { it.id == state.curPixList?.id },
            curCategories = allPixLists.find { it.id == state.curPixList?.id }.let { it?.categories?.toList() ?: emptyList() },
            colorList = _colorList // TODO: Add .value when custom color query is implemented
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = MainState()
    )

    // PixList functions -----------------------------------------------------
    fun createPixList(name: String): PixList {
        val pixList = PixList()
        viewModelScope.launch {
            realm.write {
                pixList.name = name

                copyToRealm(pixList)
            }
        }
        return pixList
    }

    fun setCurPixList(pixList: PixList) {
        _state.value = _state.value.copy(curPixList = pixList)
    }

    fun deletePixList(pixList: PixList) {
        viewModelScope.launch {
            realm.write {
                val managedPixList = findLatest(pixList) ?: throw IllegalArgumentException("pixList is invalid or outdated")
                delete(managedPixList)
            }
        }
    }

    // PixCategory functions -------------------------------------------------
    fun createPixCategory(name: String, color: PixColor, pixListId: BsonObjectId) {
        var pixList: PixList? = null
        for (pList in _allPixLists.value) {
            if (pList.id == pixListId) {
                pixList = pList
                break
            }
        }
        if (pixList == null) {
            throw IllegalArgumentException("pixListId is invalid")
        }
        viewModelScope.launch {
            realm.write {
                val pixCategory = PixCategory(name, color)
                val managedPixList = findLatest(pixList) ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.add(pixCategory)) {
                    throw IllegalArgumentException("Category already exists")
                }
                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    fun deleteCategory(category: PixCategory, pixListId: BsonObjectId) {
        var pixList: PixList? = null
        for (pList in _allPixLists.value) {
            if (pList.id == pixListId) {
                pixList = pList
                break
            }
        }
        if (pixList == null) {
            throw IllegalArgumentException("pixListId is invalid")
        }
        viewModelScope.launch {
            realm.write {
                val managedCategory = findLatest(category)
                    ?: throw IllegalArgumentException("category is invalid or outdated")
                val managedPixList = findLatest(pixList)
                    ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.remove(managedCategory)) {
                    throw IllegalArgumentException("Category does not exist")
                }

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    // PixEntry functions ---------------------------------------------------
    fun createPixEntry(day: Int, month: Months, category: PixCategory, pixListId: BsonObjectId) {
        var pixList: PixList? = null
        for (pList in _allPixLists.value) {
            if (pList.id == pixListId) {
                pixList = pList
                break
            }
        }
        if (pixList == null) {
            throw IllegalArgumentException("pixListId is invalid")
        }
        viewModelScope.launch {
            realm.write {
                val managedCategory = findLatest(category) ?: throw IllegalArgumentException("category is invalid or outdated")
                val managedPixList = findLatest(pixList) ?: throw IllegalArgumentException("pixList is invalid or outdated")
                managedPixList.entries?.setEntry(day, month, managedCategory)

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

}