package hu.mapro.karti.data

import android.arch.persistence.room.*

/**
 * Created by maprohu on 11/25/2017.
 */
@Entity
class Recording(
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
        var data: ByteArray,
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0
)

@Dao
interface RecordingDao {
        @Insert
        fun insert(item: Recording): Long

        @Query("select * from recording where id = :id")
        fun get(id: Long): Recording

        @Delete
        fun delete(item: Recording)

        @Update
        fun update(item: Recording)
}