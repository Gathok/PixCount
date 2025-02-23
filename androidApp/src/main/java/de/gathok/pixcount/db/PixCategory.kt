package de.gathok.pixcount.db

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PixCategory(): RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var name: String = ""
    var color: PixColor? = PixColor()

    constructor(name: String, color: PixColor, id: ObjectId = ObjectId()) : this() {
        this.id = id
        this.name = name
        this.color = color
    }
}
