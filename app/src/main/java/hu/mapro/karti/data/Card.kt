package hu.mapro.karti.data

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListProvider
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
data class Card(
        var questionText :String? = null,
        var questionRecordingId: Long? = null,
        var answerText: String? = null,
        var answerRecordingId: Long? = null,
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0
) {
}

@Dao
interface CardDao {
    @Insert
    fun insert(item: Card): Long

    @Update
    fun update(item: Card)

    @Query("select count(*) from card")
    fun count(): LiveData<Long>

    @Query("select * from card order by id desc")
    fun list(): LivePagedListProvider<Int, Card>

    @Query("select * from card where id = :id")
    fun get(id: Long): Card

    @Delete
    fun delete(item: Card)

}