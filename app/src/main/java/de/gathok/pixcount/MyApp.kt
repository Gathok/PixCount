package de.gathok.pixcount

import android.app.Application
import de.gathok.pixcount.db.PixCategory
import de.gathok.pixcount.db.PixColor
import de.gathok.pixcount.db.PixList
import de.gathok.pixcount.db.PixListValues
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.dynamic.DynamicRealmObject
import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.mongodb.kbson.ObjectId

class MyApp : Application() {

    companion object {
        lateinit var realm: Realm
    }

    override fun onCreate() {
        super.onCreate()
        val config = RealmConfiguration.Builder(
            schema = setOf(
                PixColor::class,
                PixCategory::class,
                PixListValues::class,
                PixList::class
            )
        )
            .schemaVersion(3)
            .migration ({ migrationContext: AutomaticSchemaMigration.MigrationContext ->
                if (migrationContext.oldRealm.schemaVersion() < 2L) {
                    migrationContext.enumerate(
                        "PixCategory"
                    ) { oldObj, newObj ->
                        val oldColor = oldObj.getObject("color")
                        val newColor = if (oldColor != null) {
                            migrationContext.newRealm.findLatest(oldColor)
                                ?: throw IllegalArgumentException("Migration failed: PixCategory.color contains invalid or outdated color")
                        } else {
                            PixColor("null",0f,0f,0f,0f,true)
                        }
                        newObj?.set("id", ObjectId())
                        newObj?.set("name", oldObj.getValue("name", String::class))
                        newObj?.set("color", newColor)
                    }
                }
                if (migrationContext.oldRealm.schemaVersion() < 3L) {
                    migrationContext.enumerate(
                        "PixList"
                    ) { oldObj, newObj ->
                        val oldCategoriesSet =
                            oldObj.getValueSet("categories", DynamicRealmObject::class)
                        val newCategoriesList = newObj?.getValueList("categories", DynamicRealmObject::class)
                            ?: throw IllegalArgumentException("Migration failed: PixList.categories is null")

                        oldCategoriesSet.forEach { oldCategory ->
                            val newCategory = migrationContext.newRealm.findLatest(oldCategory)
                                ?: throw IllegalArgumentException("Migration failed: PixList.categories contains invalid or outdated category")
                            newCategoriesList.add(newCategory)
                        }
                    }
                }
            })
            .build()
        realm = Realm.open(config)
    }
}
