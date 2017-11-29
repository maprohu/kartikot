package hu.mapro.karti.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

/**
 * Created by maprohu on 11/25/2017.
 */
@Database(
        entities = arrayOf(
                Recording::class,
                Card::class,
                Practice::class
        ),
        version = 1,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun cardDao(): CardDao
    abstract fun practiceDao(): PracticeDao
}

