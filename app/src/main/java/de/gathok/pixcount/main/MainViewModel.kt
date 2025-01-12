package de.gathok.pixcount.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.MyApp
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _colorList = realm
        .query<PixColor>()
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow(MainState())
    val state = combine(_state, _allPixLists, _colorList) { state, allPixLists, colorList ->
        state.copy(
            allPixLists = allPixLists,
            curPixList = allPixLists.find { it.id == state.curPixList?.id },
            colorList = colorList
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
}