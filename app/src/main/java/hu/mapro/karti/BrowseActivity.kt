package hu.mapro.karti

import android.app.Application
import android.arch.lifecycle.*
import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapter
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.VERTICAL
import android.view.Gravity.END
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import hu.mapro.karti.data.Card
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.sdk25.coroutines.onClick

class BrowseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = ViewModelProviders.of(this).get(BrowseModel::class.java)

        verticalLayout {
            toolbar {
                title = "Browse Karti"
                elevation = dip(4).toFloat()

                button("New") {
                    onClick {
                        startActivity(
                                Intent(
                                        this@BrowseActivity,
                                        EditActivity::class.java
                                )
                        )
                    }
                }.lparams(
                        gravity = END
                )
            }
            recyclerView {
                layoutManager = LinearLayoutManager(
                        act,
                        VERTICAL,
                        false
                )
                val a = CardAdapter(model, this@BrowseActivity)
                model.cardList.observe(
                        this@BrowseActivity,
                        Observer { a.setList(it) }
                )
                adapter = a
            }.lparams(
                    width = matchParent,
                    height = 0
            ) {
                weight = 1f
            }

        }

    }


}

object DIFF_CALLBACK : DiffCallback<Card>() {
    override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem.id == newItem.id
    }

}
class CardAdapter(val model: BrowseModel, val aca: AppCompatActivity) : PagedListAdapter<Card, CardViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardUI(model, aca).createView(
                AnkoContext.create(parent.context, parent)
        ).tag as CardViewHolder
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = getItem(position)
        if (card != null) holder.bind(card)
    }

}

class CardUI(val model: BrowseModel, val aca: AppCompatActivity): AnkoComponent<ViewGroup> {
    override fun createView(ui: AnkoContext<ViewGroup>): View {

        val controls = CardControls()

        val itemView = with(ui) {
            linearLayout {
                lparams(width = matchParent, height = wrapContent)
                orientation = LinearLayout.HORIZONTAL

                with(controls) {
                    questionText = textView {
                        model.currentPlaying.observe(
                                aca,
                                Observer {
                                    backgroundColor = recordingColor(controls.card, Question, it)
                                }
                        )

                        onClick {
                            model.startPlaying(card, Question)
                        }
                    }.lparams(width = 0, height = matchParent) {
                        weight = 5f
                        margin = dip(3)
                    }
                    answerText = textView {
                        model.currentPlaying.observe(
                                aca,
                                Observer {
                                    backgroundColor = recordingColor(controls.card, Answer, it)
                                }
                        )
                        onClick {
                            model.startPlaying(card, Answer)
                        }
                    }.lparams(width = 0, height = matchParent) {
                        weight = 5f
                        margin = dip(3)
                    }
                    id = button("0") {
                        onClick {
                            val i = Intent(ctx, EditActivity::class.java)
                            i.putExtra(EditActivity.CARD_ID, controls.card!!.id)
                            ctx.startActivity(i)
                        }
                    }.lparams(width = dip(48), height = wrapContent)
                }

            }
        }
        itemView.tag = CardViewHolder(itemView, controls, model.currentPlaying)
        return itemView
    }

}

class CardControls {
//    lateinit var id: TextView
    var card: Card? = null
    lateinit var id: Button
    lateinit var questionText: TextView
//    lateinit var questionButton: Button
    lateinit var answerText: TextView
//    lateinit var answerButton: Button
}

fun Side.recordingId(card: Card) : Long? {
    return when (this) {
        Question -> card.questionRecordingId
        Answer -> card.answerRecordingId
    }
}

fun recordingColor(card: Card?, side: Side, current: SideId?) : Int {
    return when {
        card == null -> Color.TRANSPARENT
        SideId(card.id, side) == current -> Color.GREEN
        side.recordingId(card) != null -> Color.YELLOW
        else -> Color.WHITE
    }
}


class CardViewHolder(
        itemView: View,
        val controls: CardControls,
        val current: LiveData<SideId>
) : RecyclerView.ViewHolder(itemView) {
    fun bind(card: Card) {
        controls.card = card
        with(controls) {
            id.text = card.id.toString()
            questionText.text = card.questionText ?: ""
            questionText.backgroundColor = recordingColor (card, Question, current.value)
//            questionButton.isEnabled = card.questionRecordingId != null
            answerText.text = card.answerText ?: ""
            answerText.backgroundColor = recordingColor(card, Answer, current.value)
//            answerButton.isEnabled = card.answerRecordingId != null

        }
    }

}


class BrowseModel(application: Application) : AndroidViewModel(application) {

    val cardList =
            application
                    .database
                    .cardDao()
                    .list()
                    .create(
                            0,
                            PagedList.Config.Builder()
                                    .setPageSize(50)
                                    .setPrefetchDistance(50)
                                    .build()
                    )

    val currentPlaying = MutableLiveData<SideId>()

    var playing: Playing? = null

    fun startPlaying(card: Card?, side: Side) {
        if (card != null) {
            val recordingId = side.recordingId(card)
            if (recordingId != null) {
                playing?.stop()
                playing = Playing(recordingId, SideId(card.id, side))
            }
        }
    }

    inner class Playing(id: Long, sideId: SideId) {
        val player = MediaPlayer()

        init {
            currentPlaying.value = sideId
            
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
                currentPlaying.value = null
            }

        }

        fun stop() {
            player.stop()
            player.release()
        }

    }

    override fun onCleared() {
        playing?.stop()
        super.onCleared()
    }
}

data class SideId(
        val cardId: Long,
        val side: Side
)

