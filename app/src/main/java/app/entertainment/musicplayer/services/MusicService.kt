package app.entertainment.musicplayer.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder

class MusicService : Service() {
    // MediaPlayer instance to handle music playback
    var mediaPlayer: MediaPlayer? = null

    // MyBinder instance for binding the service to clients
    private var myBinder = MyBinder()

    override fun onCreate() {
        super.onCreate()

        // Initialize the MediaPlayer instance
        initMediaPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return myBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    /**
     * Initializes the MediaPlayer instance
     */
    private fun initMediaPlayer() {
        // Initialize the MediaPlayer if it's null
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer()

        // Set audio attributes for the MediaPlayer
        mediaPlayer!!.setAudioAttributes(
            with(AudioAttributes.Builder()) {
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                setUsage(AudioAttributes.USAGE_MEDIA)
                build()
            }
        )

        // Initialize SeekBar progress
//        MusicPlayerActivity.binding.seekBar.progress = 0
//        MusicPlayerActivity.binding.seekBar.max = mediaPlayer!!.duration
    }

    // Inner class for Binder to allow clients to interact with this service
    inner class MyBinder : Binder() {
        // Get access to the current MusicService instance
        fun getCurrentService(): MusicService {
            return this@MusicService
        }
    }
}