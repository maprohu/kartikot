package hu.mapro.karti

import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.File


class MainActivity : AppCompatActivity() {

    private val restart = Channel<Unit>(1)

    override fun onRestart() {
        super.onRestart()

        restart.offer(Unit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        hu.mapro.karti.requestPermissions(this)

        restart.offer(Unit)

        verticalLayout {
            toolbar {
                title = "Karti"

                val backup = menu.add("Backup")
                backup.setOnMenuItemClickListener {
                    application
                            .database
                            .close()

                    val dbFile = context.getDatabasePath(application.database.openHelper.databaseName)
                    val outPath = "${Environment.getExternalStorageDirectory()}/karti_backup.db"

                    dbFile.copyTo(File(outPath), true)

                    Toast.makeText(this@MainActivity, "Backed up to: ${outPath}", Toast.LENGTH_SHORT)

                    finishAffinity()

                    true
                }

                val headless = menu.add("Headless Practice")
                headless.setOnMenuItemClickListener {
                    startActivity(
                            Intent(
                                    this@MainActivity,
                                    HeadlessPracticeActivity::class.java
                            )
                    )

                    true
                }
            }
            button("Practice") {
                onClick {
                    startActivity(
                            Intent(
                                    this@MainActivity,
                                    PracticeActivity::class.java
                            )
                    )

                }
            }.lparams(
                    width = matchParent,
                    height = 0
            ) {
                weight = 1f
            }
            button("Browse") {
                database.cardDao().count().observe(
                        this@MainActivity,
                        Observer {
                            text = "Browse (${it})"
                        }
                )

                onClick {
                    startActivity(
                            Intent(
                                    this@MainActivity,
                                    BrowseActivity::class.java
                            )
                    )

                }
            }.lparams(
                    width = matchParent,
                    height = 0
            ) {
                weight = 1f

            }
            button("Create New") {
                onClick {
                    startActivity(
                            Intent(
                                    this@MainActivity,
                                    EditActivity::class.java
                            )
                    )
                }
            }.lparams(
                    width = matchParent,
                    height = 0
            ) {
                weight = 1f
            }

        }

    }

    override fun onDestroy() {
        restart.close()

        super.onDestroy()
    }
}
