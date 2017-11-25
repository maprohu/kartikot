package hu.mapro.karti

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Created by maprohu on 11/24/2017.
 */
class ImportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hu.mapro.karti.requestPermissions(this)

        val c = editorPage()

        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        c.answer.recording.recording =
                contentResolver.openInputStream(uri).use { it.readBytes() }

        c.setup(this)
    }

}