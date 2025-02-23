package de.gathok.pixcount.manageColors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.gathok.pixcount.MyApp
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.manageColors.util.ManageColorsUndoActions
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
import org.mongodb.kbson.ObjectId

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
            colorUses = _colorList.value.associate { color ->
                color.id to
                _allPixLists.value.sumOf { pixList ->
                    pixList.categories.count { it.color?.id == color.id }
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageColorsState())

    fun loadDefaultColors() {
        val invalideNames = _colorList.value.map { it.name }
        val loadedColors: MutableList<PixColor> = mutableListOf()
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
                loadedColors += color
                viewModelScope.launch {
                    realm.write {
                        copyToRealm(color)
                    }
                }
            }
        }
        addToStack(
            ManageColorsUndoActions.UNDO_LOAD_DEFAULT_COLORS,
            listOf(loadedColors)
        )
    }

    fun deleteUnusedColors() {
        val deletedColors: MutableList<PixColor> = mutableListOf()
        viewModelScope.launch {
            realm.write {
                _colorList.value.forEach { color ->
                    if (state.value.colorUses[color.id] == 0 && !color.isPlaceholder) {
                        val managedColor = findLatest(color)
                            ?: throw IllegalArgumentException("color is invalid or outdated")
                        deletedColors += managedColor
                        delete(managedColor)
                    }
                }
            }
        }
        addToStack(
             ManageColorsUndoActions.UNDO_DELETE_UNUSED_COLORS,
            listOf(deletedColors)
        )
    }

    // Color DB operations
    fun addColor(name: String, red: Float, green: Float, blue: Float, alpha: Float = 1f) {
        viewModelScope.launch {
            realm.write {
                val addedColor = copyToRealm(
                    PixColor(name = name, red = red, green = green, blue = blue, alpha = alpha)
                )
                addToStack(
                    ManageColorsUndoActions.UNDO_ADD_COLOR,
                    listOf(addedColor)
                )
            }
        }
    }

    fun updateColor(colorToEdit: PixColor, newName: String?, newRgb: List<Float>?) {
        viewModelScope.launch {
            realm.write {
                val managedColor = findLatest(colorToEdit)
                    ?: throw IllegalArgumentException("color is invalid or outdated")
                addToStack(
                    ManageColorsUndoActions.UPDATE_COLOR,
                    listOf(managedColor, managedColor.name, managedColor.getRgbValues())
                )
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
                val placeholderColor = PixColor()
                val managedColor = findLatest(color)
                    ?: throw IllegalArgumentException("color is invalid or outdated")

                val removedColorUses: MutableList<ObjectId> = mutableListOf()
                for (pixList in _allPixLists.value) {
                    pixList.categories.forEach { category ->
                        if (category.color?.id == managedColor.id) {
                            val managedCategory = findLatest(category)
                                ?: throw IllegalArgumentException("category is invalid or outdated")
                            removedColorUses += managedCategory.id
                            managedCategory.color = placeholderColor
                        }
                    }
                }
                delete(managedColor)

                addToStack(
                    ManageColorsUndoActions.UNDO_DELETE_COLOR,
                    listOf(
                        color.id,
                        color.name,
                        color.getRgbValues(),
                        removedColorUses
                    )
                )
            }
        }

    }
    
    // Undo operations ---------------------------------------------------------
    private fun addToStack(action: ManageColorsUndoActions, params: List<Any>) {
        _state.value.undoStack.add(action to params)
    }

    private fun undoLoadDefaultColors(colors: List<PixColor>) {
        colors.forEach { color ->
            deleteColor(color)
        }
    }

    private fun undoDeleteUnusedColors(colors: List<PixColor>) {
        colors.forEach { color ->
            addColor(
                color.name,
                color.getRgbValues()[0],
                color.getRgbValues()[1],
                color.getRgbValues()[2]
            )
        }
    }

    private fun undoDeleteColor(id: ObjectId, name: String, rgbValues: List<Float>, categoryIds: List<ObjectId>) {
        viewModelScope.launch {
            realm.write {
                val managedColor = copyToRealm(PixColor(
                    id = id, name = name, red = rgbValues[0], green = rgbValues[1], blue = rgbValues[2]
                ))
                val managedCategories = categoryIds.map {
                    findLatest(realm.query<PixCategory>().find().find { it.id in categoryIds }
                            ?: throw IllegalArgumentException("category id is invalid or outdated"))
                        ?: throw IllegalArgumentException("category is invalid or outdated")}

                managedCategories.forEach { category ->
                    category.color = managedColor
                    copyToRealm(category)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun undo() {
        if (_state.value.undoStack.isEmpty()) return
        _state.value.undoStack.pop().let { (action, params) ->
            when (action) {
                ManageColorsUndoActions.UNDO_LOAD_DEFAULT_COLORS ->
                    undoLoadDefaultColors(params[0] as List<PixColor>)

                ManageColorsUndoActions.UNDO_DELETE_UNUSED_COLORS ->
                    undoDeleteUnusedColors(params[0] as List<PixColor>)

                ManageColorsUndoActions.UNDO_ADD_COLOR ->
                    deleteColor(params[0] as PixColor)

                ManageColorsUndoActions.UPDATE_COLOR -> {
                    updateColor(
                        params[0] as PixColor,
                        params[1] as String,
                        params[2] as List<Float>
                    )
                }
                ManageColorsUndoActions.UNDO_DELETE_COLOR -> {
                    undoDeleteColor(
                        params[0] as ObjectId,
                        params[1] as String,
                        params[2] as List<Float>,
                        params[3] as List<ObjectId>,
                    )
                }
            }
        }
    }
}