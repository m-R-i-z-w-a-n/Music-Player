package app.entertainment.musicplayer.models

import java.io.Serializable

data class Song(var path: String, var title: String, var duration: Long) : Serializable
