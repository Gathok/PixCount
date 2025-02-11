package de.gathok.pixcount.db

import androidx.compose.ui.graphics.Color
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PixColor(): RealmObject {

    @PrimaryKey var id: ObjectId = ObjectId()
    var name: String = ""
    private var red: Float = 0f
    private var green: Float = 0f
    private var blue: Float = 0f
    private var alpha: Float = 0f
    var isPlaceholder: Boolean = true

    constructor(name: String, color: Color, id: ObjectId = ObjectId(), isPlaceholder: Boolean = false) : this() {
        this.id = id
        this.name = name
        this.red = color.red
        this.green = color.green
        this.blue = color.blue
        this.alpha = color.alpha
        this.isPlaceholder = isPlaceholder
    }

    constructor(name: String, red: Float, green: Float, blue: Float, alpha: Float = 1f, id: ObjectId = ObjectId(), isPlaceholder: Boolean = false) : this() {
        this.id = id
        this.name = name
        this.red = red
        this.green = green
        this.blue = blue
        this.alpha = alpha
        this.isPlaceholder = isPlaceholder
    }

    fun toColor(): Color {
        return Color(red, green, blue, alpha)
    }

    private fun valueToHex(value: Float): String {
        val hex = (value * 255).toInt().toString(16)
        return if (hex.length == 1) "0$hex" else hex
    }

    fun toHex(): String {
        return "#${valueToHex(red)}${valueToHex(green)}${valueToHex(blue)}"
    }

    fun getRgbValues(): List<Float> {
        return listOf(red, green, blue)
    }

    fun setRgb(newRgb: List<Float>) {
        this.red = newRgb[0]
        this.green = newRgb[1]
        this.blue = newRgb[2]
    }
}