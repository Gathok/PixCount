package de.gathok.pixcount.db

import io.realm.kotlin.types.RealmObject

class PixCategory(): RealmObject {
    var name: String = ""
    var color: PixColor? = PixColor()

    constructor(name: String, color: PixColor) : this() {
        this.name = name
        this.color = color
    }
}