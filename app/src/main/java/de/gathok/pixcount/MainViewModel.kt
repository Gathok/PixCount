package de.gathok.pixcount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.ui.theme.BabyBlue
import de.gathok.pixcount.ui.theme.BlushPink
import de.gathok.pixcount.ui.theme.DustyPink
import de.gathok.pixcount.ui.theme.Lavender
import de.gathok.pixcount.ui.theme.LemonYellow
import de.gathok.pixcount.ui.theme.MintGreen
import de.gathok.pixcount.ui.theme.PaleOrange
import de.gathok.pixcount.ui.theme.PastelLilac
import de.gathok.pixcount.ui.theme.Peach
import de.gathok.pixcount.ui.theme.SkyBlue
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
        PixColor(name = "Peach", Peach),
        PixColor(name = "Lemon Yellow", LemonYellow),
        PixColor(name = "Mint Green", MintGreen),
        PixColor(name = "Sky Blue", SkyBlue),
        PixColor(name = "Lavender", Lavender),
        PixColor(name = "Dusty Pink", DustyPink),
        PixColor(name = "Pale Orange", PaleOrange),
        PixColor(name = "Baby Blue", BabyBlue),
        PixColor(name = "Blush Pink", BlushPink),
        PixColor(name = "Lilac", PastelLilac),
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
        started = SharingStarted.WhileSubscribed(5000),
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
                val newCategory = PixCategory(name = name, color = color)
                val managedPixList = findLatest(pixList) ?: throw IllegalArgumentException("pixList is invalid or outdated")
                if (!managedPixList.categories.add(newCategory)) {
                    throw IllegalArgumentException("Category already exists")
                }
                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

    fun updatePixCategory(category: PixCategory, newName: String?, newColor: PixColor?, pixListId: BsonObjectId) {
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
                if (newName != null) {
                    managedCategory.name = newName
                }
                if (newColor != null) {
                    managedCategory.color = copyToRealm(newColor, UpdatePolicy.ALL)
                }

                copyToRealm(managedCategory, UpdatePolicy.ALL)
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
    fun createPixEntry(day: Int, month: Months, category: PixCategory?, pixListId: BsonObjectId) {
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
                val managedCategory = if (category == null) PixCategory()
                    else findLatest(category) ?: throw IllegalArgumentException("category is invalid or outdated")

                val managedPixList = findLatest(pixList) ?: throw IllegalArgumentException("pixList is invalid or outdated")
                managedPixList.entries?.setEntry(day, month, managedCategory)

                copyToRealm(managedPixList, UpdatePolicy.ALL)
            }
        }
    }

}