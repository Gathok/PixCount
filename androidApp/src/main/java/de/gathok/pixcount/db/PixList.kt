package de.gathok.pixcount.db

import de.gathok.pixcount.MyApp
import de.gathok.pixcount.util.Months
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PixList : RealmObject {
    @PrimaryKey var id: ObjectId = ObjectId()
    var name: String = ""
    var entries: PixListValues? = PixListValues()
    var categories: RealmList<PixCategory> = realmListOf()

    suspend fun deleteCategory(category: PixCategory) : List<Pair<Months, Int>> {
        MyApp.realm.write {
            val managedPixList = findLatest(this@PixList)
                ?: throw IllegalArgumentException("pixList is invalid or outdated")
            val managedCategory = findLatest(category)
                ?: throw IllegalArgumentException("category is invalid or outdated")
            managedPixList.categories.remove(managedCategory)
        }
        return entries?.deleteCategory(category) ?: emptyList()
    }
}
