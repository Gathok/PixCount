package de.gathok.pixcount.manageColors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.MyApp
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
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageColorsViewModel: ViewModel() {

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
            initialValue = realm.query<PixList>().find().toList()
        )

    private val _colorList = realm
        .query<PixColor>()
        .asFlow()
        .map { results ->
            results.list.toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow(ManageColorsState())

    val state = combine(
        _state, _allPixLists.onStart { emit(emptyList()) }, _colorList.onStart { emit(emptyList()) }
    ) { state, allPixLists, colorList ->
        state.copy(
            colorList = colorList,
            allCategories = allPixLists.flatMap { it.categories },
            colorUses = _colorList.value.associateWith { color ->
                _allPixLists.value.sumOf { pixList ->
                    pixList.categories.count { it.color == color }
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageColorsState())

    fun loadDefaultColors() {
        val invalideNames = _colorList.value.map { it.name }
        listOf(
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
        ).forEach { color ->
            if (color.name !in invalideNames) {
                viewModelScope.launch {
                    realm.write {
                        copyToRealm(color)
                    }
                }
            }
        }
    }

    fun deleteUnusedColors() {
        viewModelScope.launch {
            realm.write {
                _colorList.value.forEach { color ->
                    if (_state.value.colorUses[color] == 0 && !color.isPlaceholder) {
                        val managedColor = findLatest(color)
                            ?: throw IllegalArgumentException("color is invalid or outdated")
                        delete(managedColor)
                    }
                }
            }
        }
    }

    // Color DB operations
    fun addColor(color: PixColor) {
        viewModelScope.launch {
            realm.write {
                copyToRealm(color)
            }
        }
    }

    fun updateColor(colorToEdit: PixColor, newName: String?, newRgb: List<Float>?) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(colorToEdit)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                if (newName != null) {
                    managedColor.name = newName
                }
                if (newRgb != null) {
                    managedColor.setRgb(newRgb)
                }
                copyToRealm(managedColor)
            }
        }
    }

    fun deleteColor(color: PixColor) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(color)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                for (pixList in _allPixLists.value) {
                    pixList.categories.forEach { category ->
                        if (category.color == managedColor) {
                            val managedCategory = findLatest(category)
                                ?: throw IllegalArgumentException("category is invalid or outdated")
                            managedCategory.color = PixColor()
                        }
                    }
                }
                delete(managedColor)
            }
        }
    }
}