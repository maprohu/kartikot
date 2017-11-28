package hu.mapro.karti

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import hu.mapro.karti.data.Card
import hu.mapro.karti.data.Practice
import hu.mapro.karti.data.Recording
import hu.mapro.karti.data.RecordingDao
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import java.io.File

/**
 * Created by maprohu on 11/24/2017.
 */
class EditActivity : AppCompatActivity() {

    companion object {
        val CARD_ID = "CARD_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hu.mapro.karti.requestPermissions(this)

        val model = ViewModelProviders.of(this).get(EditViewModel::class.java)

        val cardId = intent.getLongExtra(CARD_ID, -1L)
        val update = cardId != -1L

        if (model.first) {
            if (update) {
                model.cardId = cardId
                val card = application.database.cardDao().get(cardId)

                model.question.text = card.questionText
                model.answer.text = card.answerText

                if (card.questionRecordingId != null) {
                    model.question.recordingId = card.questionRecordingId
                    model.question.recording.value =
                            application
                                    .database
                                    .recordingDao()
                                    .get(card.questionRecordingId!!)
                                    .data
                }
                if (card.answerRecordingId != null) {
                    model.answer.recordingId = card.answerRecordingId
                    model.answer.recording.value =
                            application
                                    .database
                                    .recordingDao()
                                    .get(card.answerRecordingId!!)
                                    .data
                }

            }
            if (Intent.ACTION_SEND == intent.action) {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

                val bytes = contentResolver.openInputStream(uri).use { it.readBytes() }
                model.answer.recording.value = bytes
                karti.clipboard.value = bytes
            }

        }


        editorPage(this, model)

        model.first = false

        model.audioState.observe(
                this,
                Observer {
                    if (it == Finished) finish()
                }
        )
    }

}

fun Side.model(m: EditViewModel): SideModel {
    return when (this) {
        Question -> m.question
        Answer -> m.answer
    }
}

class EditViewModel(application: Application) : AndroidViewModel(application) {
    var first = true
    var cardId : Long? = null

    val question = SideModel()
    val answer = SideModel()
    val audioState = MutableLiveData<AudioState>()

    init {
        audioState.value = Idle
    }

    val Side.model: SideModel
        get() = this.model(this@EditViewModel)

    val app: Application
        get() = getApplication()

    suspend fun doDelete(context: Context) {
        AlertDialog.Builder(context)
                .setTitle("Delete")
                .setMessage("Do you want to delete this card?")
                .setPositiveButton(
                        "Yes",
                        { dialog, which ->
                            app.database.practiceDao().delete(
                                    Practice(id = cardId!!)
                            )
                            app.database.cardDao().delete(
                                    Card(id = cardId!!)
                            )
                            question.delete(app.database.recordingDao())
                            answer.delete(app.database.recordingDao())
                            proc.offer(CancelEvent)
                        }
                )
                .setNegativeButton(
                        "No",
                        { dialog, which ->  }
                )
                .show()

    }

    suspend fun doSave() {
        val db = app.database

        fun saveRecording(id: Long?, data: ByteArray?) : Long? {
            return if (id == null && data == null) {
                null
            } else if (id != null && data == null) {
                db.recordingDao().delete(
                        Recording(ByteArray(0), id)
                )
                null
            } else if (id == null && data != null) {
                db.recordingDao().insert(
                        Recording(data)
                )
            } else if (id != null && data != null) {
                db.recordingDao().update(
                        Recording(data, id)
                )
                id
            } else {
                null
            }
        }

        async(CommonPool) {
            val card =
                    Card(
                            questionText = question.text,
                            questionRecordingId = saveRecording(question.recordingId, question.recording.value),
                            answerText = answer.text,
                            answerRecordingId = saveRecording(answer.recordingId, answer.recording.value),
                            id = cardId ?: 0L
                    )

            if (cardId == null) {
                db.cardDao().insert(card)
            } else {
                db.cardDao().update(card)
            }
        }.await()

    }

    suspend fun startRecording(channel: Channel<EditorPageEvent>, side: Side): Boolean {
        audioState.value = RecordingState(side)

        val file = File.createTempFile(
                "recording",
                ".m4a",
                app.externalCacheDir
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
                        side.model.recording.value = file.readBytes()
                        file.delete()

                        audioState.value = Idle

                        return true
                    }
                    is CancelEvent -> {
                        recorder.stop()
                        return false
                    }
                    is DeleteEvent -> {
                        doDelete(event.context)
                    }
                }
            }

            return false
        } finally {
            recorder.release()
        }
    }

    suspend fun startPlaying(channel: Channel<EditorPageEvent>, s: Side): Boolean {
        data class State(
                val side: Side
        ) {
            val player = MediaPlayer()

            init {
                audioState.value = PlayingState(s)

                player.setDataSource(
                        ByteArrayMediaSource(side.model.recording.value!!)
                )
                player.prepare()
                player.start()

                player.setOnCompletionListener {
                    channel.offer(PlayComplete)
                }
            }

            fun stopPlayer() {
                player.stop()
                player.release()
            }
        }

        var state = State(s)

        try {
            for (event in channel) {
                when (event) {
                    is PlayEvent -> {
                        if (event.side == state.side) {
                            audioState.value = Idle
                            return true
                        } else {
                            state.stopPlayer()
                            state = State(event.side)
                        }
                    }
                    is PlayComplete -> {
                        audioState.value = Idle
                        return true
                    }
                    is CancelEvent -> {
                        return false
                    }
                    is SaveEvent -> {
                        doSave()
                        return false
                    }
                    is DeleteEvent -> {
                        doDelete(event.context)
                    }
                }
            }

            return false
        } finally {
            state.stopPlayer()
        }
    }

    val proc = actor<EditorPageEvent>(UI, capacity = Channel.UNLIMITED) {
        for (event in channel) {
            val cont : Boolean = when (event) {
                is RecordEvent -> startRecording(channel, event.side)
                is PlayEvent -> startPlaying(channel, event.side)
                is CancelEvent -> false
                is DeleteEvent -> {
                    doDelete(event.context)
                    true
                }
                is SaveEvent -> {
                    doSave()
                    false
                }
                else -> true
            }

            if (!cont) {
                audioState.value = Finished
                return@actor
            }
        }

    }

    override fun onCleared() {
        proc.close()
        super.onCleared()
    }
}

class SideModel {
    val recording = MutableLiveData<ByteArray>()
    var text : String? = null
    var recordingId : Long? = null

    fun delete(dao: RecordingDao) {
        if (recordingId != null) {
            dao.delete(Recording(ByteArray(0), recordingId!!))
        }
    }
}

sealed class AudioState
object Idle : AudioState()
data class PlayingState(val side: Side) : AudioState()
data class RecordingState(val side: Side) : AudioState()
object Finished : AudioState()