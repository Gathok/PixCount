package de.gathok.pixcount

import android.app.Application
import de.gathok.pixcount.dbObjects.PixCategory
import de.gathok.pixcount.dbObjects.PixColor
import de.gathok.pixcount.dbObjects.PixList
import de.gathok.pixcount.dbObjects.PixListValues
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class MyApp: Application() {

    companion object {
        lateinit var realm: Realm
    }

    override fun onCreate() {
        super.onCreate()
        realm = Realm.open(
            configuration = RealmConfiguration.create(
                schema = setOf(
                    PixColor::class,
                    PixCategory::class,
                    PixListValues::class,
                    PixList::class,
                )
            )
        )
    }
}