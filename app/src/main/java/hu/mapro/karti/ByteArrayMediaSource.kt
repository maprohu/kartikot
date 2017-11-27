package hu.mapro.karti

import android.media.MediaDataSource

/**
 * Created by maprohu on 11/25/2017.
 */
class ByteArrayMediaSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
        val count =
                if (position + size > data.size)
                    (data.size - position).toInt()
                else size

        if (count < 0) {
            return 0
        }

        System.arraycopy(
                data,
                position.toInt(),
                buffer!!,
                offset,
                count
        )

        return count
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {
    }
}