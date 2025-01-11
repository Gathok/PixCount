package de.gathok.pixcount.db

import io.realm.kotlin.ext.realmSetOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmSet
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PixList() : RealmObject {
    @PrimaryKey var id: ObjectId = ObjectId()
    var name: String = ""
    var entries: PixListValues? = PixListValues()
    var categories: RealmSet<PixCategory> = realmSetOf()

    constructor(name: String, entries: PixListValues = PixListValues(), categories: RealmSet<PixCategory> = realmSetOf()) : this() {
        this.name = name
        this.entries = entries
        this.categories = categories
    }
}