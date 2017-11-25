package hu.mapro.karti

import android.Manifest
import android.app.Activity
import android.support.v4.app.ActivityCompat

/**
 * Created by maprohu on 11/25/2017.
 */

private val REQUEST_RECORD_AUDIO_PERMISSION = 200

fun requestPermissions(context: Activity) = {
    ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
    )
}