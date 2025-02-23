package de.gathok.pixcount.util

import kotlinx.serialization.Serializable

enum class Screen {
    LIST,
    MANAGE_COLORS;
}

@Serializable
data class NavListScreen(
    val curPixListId: String? = null,
)

@Serializable
object NavManageColorsScreen

@Serializable
object LoadingScreen