package hu.mapro.karti

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.content.ContextWrapper
import hu.mapro.karti.data.AppDatabase

/**
 * Created by maprohu on 11/25/2017.
 */
class KartiApplication : Application() {
    val database: AppDatabase by lazy {
        Room
                .databaseBuilder(this, AppDatabase::class.java, "karti")
                .allowMainThreadQueries()
                .build()
    }

}

val Context.database
    get() =
        generateSequence(applicationContext) {
            (it as? ContextWrapper)?.baseContext
        }.filterIsInstance<KartiApplication>().first().database