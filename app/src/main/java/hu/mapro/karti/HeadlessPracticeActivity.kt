package hu.mapro.karti

import android.app.ActivityManager
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.Gravity.CENTER
import android.view.KeyEvent
import android.widget.EditText
import hu.mapro.karti.data.Card
import hu.mapro.karti.data.Practice
import hu.mapro.karti.data.PracticeResult
import org.jetbrains.anko.*


class HeadlessPracticeActivity : AppCompatActivity() {

    lateinit var model : HeadlessModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProviders.of(this).get(HeadlessModel::class.java)

        model.bright.observe(
                this,
                Observer {
                    if (it == true) {
                        val lp = window.attributes
                        lp.buttonBrightness = 1f
                        lp.screenBrightness = 1f
                        window.attributes = lp
                    } else {
                        val lp = window.attributes
                        lp.buttonBrightness = 0f
                        lp.screenBrightness = 0f
                        window.attributes = lp
                    }
                }
        )

        model.end.observe(
                this,
                Observer {
                    when (it) {
                        Leave.Edit -> {
                            val i = Intent(ctx, EditActivity::class.java)
                            i.putExtra(EditActivity.CARD_ID, model.item.value!!.card.id)
                            ctx.startActivity(i)
                            finish()
                        }
                        Leave.Close -> {
                            finish()
                        }

                    }
                }
        )


        verticalLayout {
            textView {
                background = GradientDrawable().also {
                    it.shape = GradientDrawable.RECTANGLE
                    it.setColor(Color.TRANSPARENT)
                    it.setStroke(dip(1), Color.BLACK)
                }
                gravity = CENTER
                textSize = dip(8).toFloat()

                model.item.observe(
                        this@HeadlessPracticeActivity,
                        Observer {
                            text = it?.card?.questionText ?: ""
                        }
                )

            }.lparams(width = matchParent, height = 0) { weight = 1f }
            textView {
                background = GradientDrawable().also {
                    it.shape = GradientDrawable.RECTANGLE
                    it.setColor(Color.TRANSPARENT)
                    it.setStroke(dip(1), Color.BLACK)
                }
                gravity = CENTER
                textSize = dip(8).toFloat()

                fun set(flipped: Boolean, answer: String?) {
                    if (flipped) {
                        text = answer ?: ""
                    } else {
                        text = "<card turned down>"
                    }
                }

                model.item.observe(
                        this@HeadlessPracticeActivity,
                        Observer {
                            set(model.flipped.value ?: false, it?.card?.answerText)
                        }
                )
                model.flipped.observe(
                        this@HeadlessPracticeActivity,
                        Observer {
                            set(it ?: false, model.item.value?.card?.answerText)
                        }
                )

            }.lparams(width = matchParent, height = 0) { weight = 1f }
        }
    }


    fun pressed(b: Buttons) {
        model.beep(ToneGenerator.TONE_CDMA_PIP)
    }

    fun held(b: Buttons) {
        model.click(b)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val b = Buttons.map[keyCode]

        if (b != null) {
            if (event.repeatCount == 0) pressed(b)
            if (event.repeatCount == 1) held(b)

            return true
        }


        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN ->
                return super.onKeyDown(keyCode, event)
        }

        return true
    }

    override fun onResume() {
        super.onResume()

        model.beep(ToneGenerator.TONE_SUP_DIAL)
    }

    override fun onPause() {
        super.onPause()
        (applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).moveTaskToFront(taskId, 0)
    }
}

enum class Buttons(
        val code: Int

) {
    A(KeyEvent.KEYCODE_BUTTON_A),
    B(KeyEvent.KEYCODE_BUTTON_B),
    X(KeyEvent.KEYCODE_BUTTON_X),
    Y(KeyEvent.KEYCODE_BUTTON_Y),
    Select(KeyEvent.KEYCODE_BUTTON_SELECT),
    Start(KeyEvent.KEYCODE_BUTTON_START),
    Left(KeyEvent.KEYCODE_DPAD_LEFT),
    Right(KeyEvent.KEYCODE_DPAD_RIGHT),
    Up(KeyEvent.KEYCODE_DPAD_UP),
    Down(KeyEvent.KEYCODE_DPAD_DOWN),
    R1(KeyEvent.KEYCODE_BUTTON_R1),
    L1(KeyEvent.KEYCODE_BUTTON_L1),

    ;



    companion object {
        val map = values().associateBy { it.code }
    }



}

class HeadlessModel(val app: Application) : AndroidViewModel(app) {
    val item = MutableLiveData<PracticeItem>()
    val flipped = MutableLiveData<Boolean>()
    val bright = MutableLiveData<Boolean>()
    val end = MutableLiveData<Leave>()
    val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun click(b: Buttons) {
        when {
            b == Buttons.B -> {
                beep(0)
                bright.value = false
            }
            b == Buttons.X -> {
                beep(0)
                bright.value = true
            }
            b == Buttons.Y -> {
                beep(0)
                end.value = Leave.Edit
            }
            b == Buttons.A -> {
                beep(0)
                end.value = Leave.Close
            }
            item.value == null -> {
                beep(ToneGenerator.TONE_SUP_BUSY, 2000)
            }
            else -> {

                if (b == Buttons.Start) {
                    play(item.value!!.card.questionRecordingId!!)
                    beep(0)
                } else if (b == Buttons.Select) {
                    flipped.value = true
                    beep(0)
                    play(item.value!!.card.answerRecordingId!!)
                } else if (b == Buttons.L1) {
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                    beep(0)
                } else if (b == Buttons.R1) {
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                    beep(0)
                } else if (flipped.value == true) {
                    when (b) {
                        Buttons.Down -> feedback(PracticeResult.Easy)
                        Buttons.Left -> feedback(PracticeResult.Good)
                        Buttons.Up -> feedback(PracticeResult.Hard)
                        Buttons.Right -> feedback(PracticeResult.Again)
                        else -> beep(1)
                    }
                } else {
                    beep(1)
                }
            }

        }

    }

    fun feedback(result: PracticeResult) {
        beep(0)
        val pr = item.value!!.practice
        pr.statusEnum.update(pr, result)
        app.database.practiceDao().insert(pr)
        load()
    }

    fun load() {
        item.value = null
        flipped.value = false

        val due = app.database.practiceDao().nextDueHeadless(System.currentTimeMillis()).firstOrNull()

        if (due != null) {
            val card = app.database.cardDao().get(due.id)

            item.value =
                    PracticeItem(
                            card,
                            due
                    )
            play(card.questionRecordingId!!)

            return
        }

        val unseen = app.database.cardDao().nextNew().firstOrNull()

        if (unseen != null) {
            val pr = Practice(unseen.id)

            item.value =
                    PracticeItem(
                            unseen,
                            pr
                    )

            play(unseen.questionRecordingId!!)

            return
        }

    }

    fun play(id: Long) {
        currentPlaying?.stop()
        currentPlaying = Playing(id)
    }

    var currentPlaying: Playing? = null

    inner class Playing(id: Long) {
        val player = MediaPlayer()

        init {
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
        }

        fun stop() {
            player.stop()
            player.release()
        }

    }


    val tg = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)

    fun beep(id: Int, duration: Int = 100) {
        tg.startTone(id, duration)
    }

    override fun onCleared() {
        tg.release()
        currentPlaying?.stop()
        super.onCleared()
    }

    init {
        bright.value = false
        load()
    }
}

enum class Leave {
    Close,
    Edit
}