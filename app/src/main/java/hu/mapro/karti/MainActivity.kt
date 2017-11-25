package hu.mapro.karti

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.jetbrains.anko.*



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hu.mapro.karti.requestPermissions(this)

        val c = editorPage()

        c.setup(this)
    }
}
