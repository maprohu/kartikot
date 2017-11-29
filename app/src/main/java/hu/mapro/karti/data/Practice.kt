package hu.mapro.karti.data

import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@Entity(
        foreignKeys = arrayOf(
                ForeignKey(
                        entity = Card::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("id")
                )
        ),
        indices = arrayOf(
                Index(
                        value = "next"
                )
        )

)
class Practice(
        @PrimaryKey
        var id: Long = 0,
        var status: Int = PracticeStatus.New.ordinal,
        var delay: Long = 0,
        var next: Long = 0
) {
    val statusEnum : PracticeStatus
        get() = PracticeStatus.values()[status]


    fun add(millis: Long) {
        val v = (millis * (1 + ThreadLocalRandom.current().nextDouble(0.1))).toLong()

        delay = v
        next = System.currentTimeMillis() + delay
    }

    fun setTo(sn: PracticeStatus) {
        status = sn.ordinal
    }

    fun again() {
        status = PracticeStatus.New.ordinal
        add(TimeUnit.MINUTES.toMillis(1))
    }

    fun hard() {
        add(delay)
    }

    fun factor(v: Double) {
        add((delay * v).toLong())
    }
}

enum class PracticeStatus {
    New {
        override fun update(practice: Practice, result: PracticeResult) {
            when (result) {
                PracticeResult.Again -> practice.again()
                PracticeResult.Hard -> practice.add(TimeUnit.MINUTES.toMillis(10))
                PracticeResult.Good -> {
                    practice.add(TimeUnit.MINUTES.toMillis(10))
                    practice.setTo(Learning)
                }
                PracticeResult.Easy -> {
                    practice.add(TimeUnit.DAYS.toMillis(1))
                    practice.setTo(Learned)
                }
            }
        }
    },
    Learning {
        override fun update(practice: Practice, result: PracticeResult) {
            when (result) {
                PracticeResult.Again -> practice.again()
                PracticeResult.Hard -> practice.hard()
                PracticeResult.Good -> {
                    practice.add(TimeUnit.DAYS.toMillis(1))
                    practice.setTo(Learned)
                }
                PracticeResult.Easy -> {
                    practice.add(TimeUnit.DAYS.toMillis(2))
                    practice.setTo(Learned)
                }
            }
        }
    },
    Learned {
        override fun update(practice: Practice, result: PracticeResult) {
            when (result) {
                PracticeResult.Again -> practice.again()
                PracticeResult.Hard -> practice.hard()
                PracticeResult.Good -> practice.factor(2.0)
                PracticeResult.Easy -> practice.factor(3.0)
            }
        }
    };

    abstract fun update(practice: Practice, result: PracticeResult)
}

enum class PracticeResult {
    Again,
    Hard,
    Good,
    Easy
}

@Dao
interface PracticeDao {
    @Insert(onConflict = REPLACE)
    fun insert(item: Practice): Long

    @Query("select * from practice where id = :id")
    fun get(id: Long): Practice

    @Delete
    fun delete(item: Practice)

    @Query("select * from practice where next <= :ts order by next limit 1")
    fun nextDue(ts: Long = System.currentTimeMillis()): List<Practice>

    @Query(
            """
                select
                    p.*
                from
                    practice p
                    inner join
                        card c
                    on
                        p.id = c.id
                where
                    p.next <= :ts
                    and
                    c.questionRecordingId is not null
                    and
                    c.answerRecordingId is not null
                order by
                    p.next
                limit
                    1
            """
    )
    fun nextDueHeadless(ts: Long = System.currentTimeMillis()): List<Practice>

}
