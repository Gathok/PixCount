package de.gathok.pixcount.util

import kotlinx.serialization.Serializable
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.ObjectId

enum class Screen {
    LIST,
    MANAGE_COLORS;

    val getObject : Any
        get() = when (this) {
            LIST -> NavListScreen
            MANAGE_COLORS -> NavManageColorsScreen
        }
}

@Serializable
data class NavListScreen(
    val curPixListId: String? = null,
)

@Serializable
object NavManageColorsScreen