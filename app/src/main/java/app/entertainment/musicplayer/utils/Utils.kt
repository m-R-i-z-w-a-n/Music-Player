package app.entertainment.musicplayer.utils

import android.media.MediaMetadataRetriever

/**
 * Retrieve the album art of a song given its path
 *
 * @param path location of the song to get albumArt of
 *
 * @return returns target image art as ByteArray
 */
fun getImageArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}