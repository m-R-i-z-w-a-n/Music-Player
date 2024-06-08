package app.entertainment.musicplayer.utils

import android.media.MediaMetadataRetriever
import java.util.concurrent.TimeUnit

/**
 * Retrieve the album art of a song given its path
 *
 * @param path Location of the song to get albumArt of
 *
 * @return Returns target image art as ByteArray
 */
fun getImageArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}

/**
 * Converts a duration in milliseconds to a string representation in the format MM:SS (minutes and seconds).
 *
 * @param duration The duration in milliseconds to be converted.
 * @return A string in the format MM:SS representing the converted duration.
 */
fun convertToMMSS(duration: Long): String {
    // Convert the duration to minutes and seconds
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1)

    // Format the minutes and seconds as MM:SS
    return String.format("%02d:%02d", minutes, seconds)
}
