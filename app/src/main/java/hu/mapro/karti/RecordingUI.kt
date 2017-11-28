package hu.mapro.karti

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.view.Gravity.END
import android.view.ViewManager
import android.widget.EditText
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.textChangedListener

fun EditText.textOrNull() : String? {
    val txt = this.text.toString()
    return if (txt.isBlank()) null
    else txt
}

enum class Ids{
    QuestionText,
    AnswerText
}

val Side.textId: Int
        get() = when (this) {
            Question -> Ids.QuestionText
            Answer -> Ids.AnswerText
        }.ordinal

fun ViewManager.sideEditor(aca: AppCompatActivity, model: EditViewModel, side: Side) {
    val sideModel = side.model(model)

    verticalLayout {
        editText {
            id = side.textId
            if (model.first) setText(sideModel.text)

            textChangedListener {
                afterTextChanged {
                    sideModel.text = textOrNull()
                }
            }
        }.lparams(
                width = matchParent,
                height = 0
        ) {
            weight = 1f
        }
        recordingControls(aca, model, side)

        lparams(
                width = matchParent,
                height = 0
        ) {
            weight = 1f
        }
    }

}

fun ViewManager.recordingControls(aca: AppCompatActivity, model: EditViewModel, side: Side) {
    val sideModel = side.model(model)

    toolbar {
        linearLayout {
            button("Play") {
                model.audioState.observe(
                        aca,
                        Observer {
                            text = when (it) {
                                PlayingState(side) -> "Playing"
                                else -> "Play"
                            }
                            isEnabled =
                                    sideModel.recording.value != null &&
                                            it !is RecordingState
                        }
                )
                sideModel.recording.observe(
                        aca,
                        Observer {
                            isEnabled =
                                    it != null &&
                                            model.audioState.value !is RecordingState

                        }
                )
                onClick {
                    model.proc.offer(PlayEvent(side))
                }
            }.lparams(
                    width = 0,
                    height = wrapContent
            ) {
                weight = 1f
            }
            button("Record") {
                model.audioState.observe(
                        aca,
                        Observer {
                            text = when (it) {
                                RecordingState(side) -> "Recording"
                                else -> "Record"
                            }
                            isEnabled =
                                    when (it) {
                                        RecordingState(side.other()) -> false
                                        is PlayingState -> false
                                        else -> true
                                    }
                        }
                )
                onClick {
                    model.proc.offer(RecordEvent(side))
                }
            }.lparams(
                    width = 0,
                    height = wrapContent
            ) {
                weight = 1f
            }
        }.lparams(
                width = matchParent,
                height = wrapContent
        )

        val paste = menu.add("Paste")
        paste.isEnabled = false
        aca.karti.clipboard.observe(
                aca,
                Observer {
                    paste.isEnabled = it != null
                }
        )
        paste.setOnMenuItemClickListener {
            sideModel.recording.value = aca.karti.clipboard.value
            true
        }
    }

}


fun Activity.editorPage(aca: AppCompatActivity, model: EditViewModel) {
    verticalLayout {
        toolbar {
            title = "Karti"
            backgroundColor = Color.CYAN
            elevation = dip(4).toFloat()
            linearLayout {
                if (model.cardId != null) {
                    button("Delete") {
                        onClick {
                            model.proc.offer(DeleteEvent(ctx))
                        }
                    }
                }
                button("Cancel") {
                    onClick {
                        model.proc.offer(CancelEvent)
                    }
                }
                button("Save") {
                    model.audioState.observe(
                            aca,
                            Observer {
                                isEnabled = when (it) {
                                    is RecordingState -> false
                                    else -> true
                                }
                            }
                    )
                    onClick {
                        model.proc.offer(SaveEvent)
                    }
                }
            }.lparams(
                    gravity = END
            )
        }

        sideEditor(aca, model, Question)
        sideEditor(aca, model, Answer)

    }

}


sealed class Side {
    abstract fun other(): Side
}
object Question : Side() {
    override fun other(): Side = Answer
}
object Answer : Side() {
    override fun other(): Side = Question
}

sealed class EditorPageEvent
object CancelEvent : EditorPageEvent()
class DeleteEvent(val context: Context) : EditorPageEvent()
class RecordEvent(val side: Side) : EditorPageEvent()
class PlayEvent(val side: Side) : EditorPageEvent()
object PlayComplete : EditorPageEvent()
object SaveEvent : EditorPageEvent()
