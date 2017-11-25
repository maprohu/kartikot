package hu.mapro.karti

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.view.Gravity.END
import android.view.ViewManager
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.File


fun ViewManager.sideEditor(): SideEditor {
    val c = SideEditor()

    verticalLayout {
        c.text = editText {
        }.lparams(
                width = matchParent,
                height = 0
        ) {
            weight = 1f
        }
        c.recording = recordingControls()

        lparams(
                width = matchParent,
                height = 0
        ) {
            weight = 1f
        }
    }

    return c
}

class SideEditor {
    lateinit var text: EditText
    lateinit var recording: RecordingControls

    fun setup(a: ActorJob<EditorPageEvent>, side: Side) {
        recording.setup(a, side)
    }
}

fun ViewManager.recordingControls(): RecordingControls {
    val c = RecordingControls()

    toolbar {
        linearLayout {
            c.playButton = button("Play").lparams(
                    width = 0,
                    height = wrapContent
            ) {
                weight = 1f
            }
            c.recordButton = button("Record").lparams(
                    width = 0,
                    height = wrapContent
            ) {
                weight = 1f
            }
        }.lparams(
                width = matchParent,
                height = wrapContent
        )

        menu.add("Copy")
    }

    return c
}

class RecordingControls {
    lateinit var playButton: Button
    lateinit var recordButton: Button

    var recording : ByteArray? = null

    fun setEnabled(value: Boolean) {
        playButton.isEnabled = (recording != null) && value
        recordButton.isEnabled = value
    }

    fun setup(a: ActorJob<EditorPageEvent>, side: Side) {
        playButton.onClick {
            a.offer(PlayEvent(side))
        }
        recordButton.onClick {
            a.offer(RecordEvent(side))
        }
        setEnabled(true)
    }
}


fun Activity.editorPage(): EditorPage {
    val c = EditorPage()

    verticalLayout {
        toolbar {
            title = "Karti"
            backgroundColor = Color.CYAN
            elevation = dip(4).toFloat()
            linearLayout {
                c.cancel = button("Cancel")
                c.save = button("Save")
            }.lparams(
                    gravity = END
            )
        }
        c.question = sideEditor()
        c.answer = sideEditor()

    }

    return c
}

class EditorPage {
    lateinit var question: SideEditor
    lateinit var answer: SideEditor
    lateinit var cancel: Button
    lateinit var save: Button

    suspend fun startRecording(activity: Activity, channel: Channel<EditorPageEvent>, side: Side) {
        save.isEnabled = false
        val c = side.controls(this@EditorPage).recording
        val oc = side.other().controls(this@EditorPage).recording
        oc.setEnabled(false)
        c.playButton.isEnabled = false
        c.recordButton.text = "Recording"

        val file = File.createTempFile(
                "recording",
                ".m4a",
                activity.externalCacheDir
        )
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioChannels(1)
        recorder.setAudioEncodingBitRate(16 * 44100)
        recorder.setAudioSamplingRate(44100)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()

        try {
            for (event in channel) {
                when (event) {
                    is RecordEvent -> {
                        recorder.stop()
                        c.recording = file.readBytes()
                        file.delete()

                        oc.setEnabled(true)
                        c.setEnabled(true)
                        c.recordButton.text = "Record"
                        save.isEnabled = true
                        return
                    }
                    is CancelEvent -> {
                        recorder.stop()
                        activity.finish()
                        return
                    }
                }
            }
        } finally {
            recorder.release()
        }
    }

    suspend fun startPlaying(activity: Activity, channel: Channel<EditorPageEvent>, s: Side) {
        data class State(
                val side: Side
        ) {
            val c = side.controls(this@EditorPage).recording
            val oc = side.other().controls(this@EditorPage).recording

            val player = MediaPlayer()

            init {
                c.recordButton.isEnabled = false
                oc.recordButton.isEnabled = false
                c.playButton.text = "Playing"

                player.setDataSource(
                        ByteArrayMediaSource(c.recording!!)
                )
                player.prepare()
                player.start()

                player.setOnCompletionListener { mp ->
                    channel.offer(PlayComplete)
                }
            }
        }

        var state = State(s)


        try {
            for (event in channel) {
                with(state) {
                    when (event) {
                        is PlayEvent -> {
                            c.playButton.text = "Play"
                            oc.setEnabled(true)
                            c.setEnabled(true)

                            if (event.side == side) {
                                return
                            } else {
                                player.stop()
                                player.release()
                                state = State(event.side)
                            }
                        }
                        is PlayComplete -> {
                            c.playButton.text = "Play"
                            oc.setEnabled(true)
                            c.setEnabled(true)
                            return
                        }
                        is CancelEvent -> {
                            activity.finish()
                            return
                        }
                    }
                }
            }
        } finally {
            state.player.stop()
            state.player.release()
        }
    }

    fun setup(activity: Activity) {
        val a = actor<EditorPageEvent>(UI, capacity = Channel.UNLIMITED) {
            for (event in channel) {
                when (event) {
                    is RecordEvent -> startRecording(activity, channel, event.side)
                    is PlayEvent -> startPlaying(activity, channel, event.side)
                    is CancelEvent -> {
                        activity.finish()
                    }
                }

                if (activity.isFinishing) {
                    return@actor
                }
            }

        }

        cancel.onClick {
            a.offer(CancelEvent)
        }

        question.setup(a, Question)
        answer.setup(a, Answer)
    }
}

sealed class Side {
    abstract fun controls(page: EditorPage): SideEditor
    abstract fun other(): Side
}
object Question : Side() {
    override fun other(): Side = Answer
    override fun controls(page: EditorPage): SideEditor = page.question
}
object Answer : Side() {
    override fun other(): Side = Question
    override fun controls(page: EditorPage): SideEditor = page.answer
}

sealed class EditorPageEvent
object CancelEvent : EditorPageEvent()
class RecordEvent(val side: Side) : EditorPageEvent()
class PlayEvent(val side: Side) : EditorPageEvent()
object PlayComplete : EditorPageEvent()
