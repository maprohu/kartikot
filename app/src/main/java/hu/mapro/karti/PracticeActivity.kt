package hu.mapro.karti

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewManager
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.TEXT_ALIGNMENT_CENTER
import android.widget.Toast
import hu.mapro.karti.data.Card
import hu.mapro.karti.data.Practice
import hu.mapro.karti.data.PracticeResult

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

class PracticeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = ViewModelProviders.of(this).get(PracticeModel::class.java)

        verticalLayout{
            toolbar {
                title = "Practice Karti"

                val editItem = menu.add("Edit")

                model.state.observe(
                        this@PracticeActivity,
                        Observer {
                            editItem.isEnabled =
                                    it is PracticeLoaded
                        }
                )

                editItem.setOnMenuItemClickListener { item ->
                    finish()

                    val i = Intent(ctx, EditActivity::class.java)
                    i.putExtra(EditActivity.CARD_ID, model.state.value!!.item!!.card.id)
                    ctx.startActivity(i)

                    true
                }

            }

            verticalLayout {
                practiceSide(Question, model, this@PracticeActivity)
                practiceSide(Answer, model, this@PracticeActivity)
            }.lparams(width = matchParent, height = 0) { weight = 2f }

            verticalLayout {
                linearLayout {
                    orientation = HORIZONTAL
                    practiceFeedback(PracticeResult.Again, model, this@PracticeActivity)
                    practiceFeedback(PracticeResult.Hard, model, this@PracticeActivity)
                }.lparams(width = matchParent, height = 0) { weight = 1f }
                linearLayout {
                    orientation = HORIZONTAL
                    practiceFeedback(PracticeResult.Good, model, this@PracticeActivity)
                    practiceFeedback(PracticeResult.Easy, model, this@PracticeActivity)
                }.lparams(width = matchParent, height = 0) { weight = 1f }
            }.lparams(width = matchParent, height = 0) { weight = 1f }


            lparams(width = matchParent, height = matchParent)
        }

        model.state.observe(
                this,
                Observer {
                    if (it == PracticeFinished) {
                        Toast.makeText(
                                this,
                                "No more cards for now",
                                Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
        )

    }
}

fun practiceTextBackground(side: Side, state: PracticeState?, currentPlaying: Side?): Int {
    return when {
        side == currentPlaying -> Color.GREEN
        state?.item != null && side.recordingId(state.item!!.card) != null -> Color.YELLOW
        else -> Color.WHITE
    }
}

fun _LinearLayout.practiceSide(side: Side, model: PracticeModel, aca: AppCompatActivity) {
    textView {
        background = GradientDrawable().also {
            it.shape = GradientDrawable.RECTANGLE
            it.setColor(Color.TRANSPARENT)
            it.setStroke(dip(1), Color.BLACK)
        }
        gravity = CENTER
        textSize = dip(8).toFloat()

        model.state.observe(
                aca,
                Observer {
                    backgroundColor = practiceTextBackground(side, it, model.playing.value)

                    text = when (side) {
                        Question -> {
                            it?.item?.card?.questionText ?: ""
                        }
                        Answer -> {
                            when (it) {
                                is PracticeFeedback -> it.item.card.answerText
                                else -> "<card turned down>"
                            }
                        }
                    }

                }
        )

        model.playing.observe(
                aca,
                Observer {
                    backgroundColor = practiceTextBackground(side, model.state.value, it)
                }
        )

        onClick {
            model.click(side)
        }

    }.lparams(width = matchParent, height = 0) {
        weight = 1f
        margin = dip(4)
    }

}

fun _LinearLayout.practiceFeedback(result: PracticeResult, model: PracticeModel, aca: AppCompatActivity) {
    button(result.name) {
        model.state.observe(
                aca,
                Observer {
                    isEnabled = it is PracticeFeedback
                }
        )
        onClick {
            model.click(result)
        }

    }.lparams(0, matchParent) {
        weight = 1f
    }
}

class PracticeModel(val app: Application) : AndroidViewModel(app) {

    val state = MutableLiveData<PracticeState>()
    val playing = MutableLiveData<Side>()

    fun click(side: Side) {
        val stateValue = state.value
        val card = stateValue?.item?.card

        if (card != null) {
            val recordingId = side.recordingId(card)
            if (recordingId != null) {
                currentPlaying?.stop()
                currentPlaying = Playing(recordingId, side)
            }
        }

        if (side == Answer && stateValue is PracticeAsking) {
            state.value = PracticeFeedback(stateValue.item)
        }

    }

    fun click(result: PracticeResult) {
        val stateValue = state.value

        if (stateValue is PracticeFeedback) {
            val pr = stateValue.item.practice
            pr.statusEnum.update(pr, result)
            app.database.practiceDao().insert(pr)
            load()
        }
    }

    fun load() {
        state.value = PracticeLoading

        val due = app.database.practiceDao().nextDue(System.currentTimeMillis()).firstOrNull()

        if (due != null) {
            val card = app.database.cardDao().get(due.id)

            state.value = PracticeAsking(
                    PracticeItem(
                            card,
                            due
                    )
            )

            click(Question)

            return
        }

        val unseen = app.database.cardDao().nextNew().firstOrNull()

        if (unseen != null) {
            val pr = Practice(unseen.id)

            state.value = PracticeAsking(
                    PracticeItem(
                            unseen,
                            pr
                    )
            )

            click(Question)

            return
        }

        state.value = PracticeFinished
    }

    init {
        load()
    }

    var currentPlaying: Playing? = null

    inner class Playing(id: Long, side: Side) {
        val player = MediaPlayer()

        init {
            playing.value = side

            player.setDataSource(
                    ByteArrayMediaSource(
                            getApplication<Application>()
                                    .database
                                    .recordingDao()
                                    .get(id)
                                    .data
                    )
            )
            player.prepare()
            player.start()

            player.setOnCompletionListener {
                playing.value = null
            }

        }

        fun stop() {
            player.stop()
            player.release()
        }

    }

    override fun onCleared() {
        currentPlaying?.stop()
        super.onCleared()
    }

}

class PracticeItem(
        val card: Card,
        var practice: Practice
)

sealed class PracticeState {
    abstract val item: PracticeItem?
}
object PracticeLoading: PracticeState() {
    override val item: PracticeItem?
        get() = null
}
open class PracticeLoaded(override val item: PracticeItem): PracticeState()
class PracticeAsking(item: PracticeItem): PracticeLoaded(item)
class PracticeFeedback(item: PracticeItem): PracticeLoaded(item)
object PracticeFinished: PracticeState() {
    override val item: PracticeItem?
        get() = null
}


