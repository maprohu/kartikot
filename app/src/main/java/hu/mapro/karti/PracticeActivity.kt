package hu.mapro.karti

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout.HORIZONTAL

import org.jetbrains.anko.*

class PracticeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = ViewModelProviders.of(this).get(PracticeModel::class.java)

        verticalLayout{
            toolbar {
                title = "Practice Karti"
            }

            verticalLayout {
                textView {
                }.lparams(width = matchParent, height = 0) { weight = 1f }
                textView {
                }.lparams(width = matchParent, height = 0) { weight = 1f }
            }.lparams(width = matchParent, height = 0) { weight = 2f }

            verticalLayout {
                linearLayout {
                    orientation = HORIZONTAL
                    button("A").lparams(0, matchParent) { weight = 1f }
                    button("B").lparams(0, matchParent) { weight = 1f }
                }.lparams(width = matchParent, height = 0) { weight = 1f }
                linearLayout {
                    orientation = HORIZONTAL
                    button("C").lparams(0, matchParent) { weight = 1f }
                    button("D").lparams(0, matchParent) { weight = 1f }
                }.lparams(width = matchParent, height = 0) { weight = 1f }
            }.lparams(width = matchParent, height = 0) { weight = 1f }


            lparams(width = matchParent, height = matchParent)
        }
    }
}

class PracticeModel(application: Application) : AndroidViewModel(application) {

}

sealed class PracticeState
