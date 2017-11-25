package hu.mapro.karti.data

import android.arch.persistence.room.*

/**
 * Created by maprohu on 11/25/2017.
 */
@Entity(
        foreignKeys = arrayOf(
            ForeignKey(
                    entity = Recording::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("questionRecordingId")
            ),
            ForeignKey(
                    entity = Recording::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("answerRecordingId")
            )
        )
)
class Card(
        var questionText :String?,
        var questionRecordingId: Long?,
        var answerText: String?,
        var answerRecordingId: Long?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Dao
interface CardDao {
    @Insert
    fun insert(item: Card): Long

    @Query("select count(*) from card")
    fun count(): Long
}