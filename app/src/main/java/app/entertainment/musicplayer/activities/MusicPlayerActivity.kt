package app.entertainment.musicplayer.activities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.entertainment.musicplayer.R
import app.entertainment.musicplayer.databinding.ActivityMusicPlayerBinding
import app.entertainment.musicplayer.models.Song
import app.entertainment.musicplayer.notifications.MusicApplication
import app.entertainment.musicplayer.services.MusicService
import app.entertainment.musicplayer.utils.NEXT
import app.entertainment.musicplayer.utils.PLAY_PAUSE
import app.entertainment.musicplayer.utils.PREVIOUS
import app.entertainment.musicplayer.utils.STOP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit


class MusicPlayerActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener {

    private lateinit var currentSong: Song

    private lateinit var binding: ActivityMusicPlayerBinding
    private lateinit var musicServiceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start music service
        startMusicService()

        // Get the song index and songs list from the intent
        songIndex = intent.getIntExtra("index", 0)
        if (songsList == null)
            songsList = intent.getSerializableExtra("LIST") as ArrayList<Song>

        setResourcesWithMusic()

        // Set a listener for the SeekBar
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    musicService!!.mediaPlayer!!.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun setLayout() {
        // Set the current song and update UI elements

        currentSong = songsList!![songIndex]

        binding.songTitle.text = currentSong.title
        binding.tvTotalTime.text = convertToMMSS(currentSong.duration)

        binding.imgPausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_24)
    }

    /**
     * Initialize resources and event listeners
     */
    private fun setResourcesWithMusic() {
        setLayout()

        binding.imgPausePlay.setOnClickListener {
            pausePlaySong()
        }

        binding.imgNext.setOnClickListener {
            playNextSong()
        }

        binding.imgPrevious.setOnClickListener {
            playPreviousSong()
        }
    }

    /**
     * Start the music service and bind to it
     */
    private fun startMusicService() {
        Intent(this, MusicService::class.java).also {
            musicServiceIntent = it
            bindService(it, serviceConnection, BIND_AUTO_CREATE)
            startService(it)
        }
    }

    // Define a ServiceConnection object to interact with the MusicService
    private val serviceConnection = object : ServiceConnection {
        // This method is called when the MusicService is successfully connected
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Retrieve the MusicService instance via the IBinder interface
            val binder = service as MusicService.MyBinder
            musicService = binder.getCurrentService()

            // Initialize the MediaPlayer and UI elements
            initializeMediaPlayer()

            // Register the BroadcastReceiver
            registerBroadcastReceiver()

            // Change SeekBar progress and time in RealTime
            changeSeekBarProgress()
            // Show a notification for the currently playing song
            MusicApplication.showNotification(this@MusicPlayerActivity, musicService!!)
        }

        // This method is called when the connection to the MusicService is (unexpectedly) disconnected
        override fun onServiceDisconnected(name: ComponentName?) {
            // Handle the disconnection from the MusicService
            musicService = null

            // Unregister the BroadcastReceiver
            unregisterReceiver(notificationReceiver)
        }
    }

    /**
     * Initialize the MediaPlayer and start playing the selected song
     */
    private fun initializeMediaPlayer() {
        musicService!!.mediaPlayer!!.apply {
            reset()
            try {
                setDataSource(songsList!![songIndex].path)
                prepare()
                start()
                binding.seekBar.progress = 0
                binding.seekBar.max = musicService!!.mediaPlayer!!.duration
                setOnCompletionListener(this@MusicPlayerActivity)
            } catch (exception: IOException) {
                exception.printStackTrace()
            } catch (exception: IllegalStateException) {
                exception.printStackTrace()
                Toast.makeText(this@MusicPlayerActivity, "File might be corrupted!", Toast.LENGTH_LONG).show()
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    /**
     * Play the next song in the list
     */
    private fun playNextSong() {
        if (songIndex == songsList!!.size - 1)
        // If we are playing last song in list
            songIndex = 0
        else
            songIndex++

        setLayout()
        initializeMediaPlayer()

        Log.d("notification", "Playing Next song")

        // Update notification
        MusicApplication.showNotification(this@MusicPlayerActivity, musicService!!)
    }

    /**
     * Play the previous song in the list
     */
    private fun playPreviousSong() {
        if (songIndex == 0)
        // If we are playing 1st song in list
            songIndex = songsList!!.size - 1
        else
            songIndex--

        setLayout()
        initializeMediaPlayer()

        Log.d("notification", "Playing Previous song")

        // Update notification
        MusicApplication.showNotification(this@MusicPlayerActivity, musicService!!)
    }

    /**
     * Pause or play the current song and update the UI
     */
    private fun pausePlaySong() {
        if (musicService == null)
            startMusicService()

        if (musicService!!.mediaPlayer!!.isPlaying) {
            musicService!!.mediaPlayer!!.pause()
            Log.d("notification", "Pausing music")
            binding.imgPausePlay.setImageResource(R.drawable.ic_baseline_play_circle_24)
            MusicApplication.showNotification(
                this,
                musicService!!,
                R.drawable.ic_notification_play_circle_24
            )
        } else {
            musicService!!.mediaPlayer!!.start()
            Log.d("notification", "Starting music")
            binding.imgPausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_24)
            MusicApplication.showNotification(this, musicService!!)
        }
    }

    /**
     * Updates SeekBar progress and current time
     */
    private fun changeSeekBarProgress() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                binding.seekBar.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.tvCurrentTime.text =
                    convertToMMSS(musicService!!.mediaPlayer!!.currentPosition.toLong())

                delay(100)
            }
        }
    }

    private val notificationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let { action ->
                when (action) {
                    PLAY_PAUSE -> pausePlaySong()

                    PREVIOUS -> playPreviousSong()

                    NEXT -> playNextSong()

                    STOP -> {
                        // Stop the foreground service and release resources
                        musicService!!.mediaPlayer!!.pause()

                        MusicApplication.dismissNotification()

                        // Change music player icon to play
                        binding.imgPausePlay.setImageResource(R.drawable.ic_baseline_play_circle_24)

                        // Stop the service and unbind it
                        musicService!!.stopService(musicServiceIntent)
                        unbindService(serviceConnection)
                        return
                    }
                }
            } ?: Toast.makeText(context, "Action is NULL", Toast.LENGTH_LONG).show()
        }
    }

    private fun registerBroadcastReceiver() {
        IntentFilter().apply {
            // Add intent actions for Action Buttons
            arrayOf(PLAY_PAUSE, PREVIOUS, NEXT, STOP).forEach(this::addAction)

            // Register the BroadcastReceiver
            registerReceiver(notificationReceiver, this)
        }
    }

    /**
     * Handle song completion, automatically play the next song in the playlist
     */
    override fun onCompletion(mp: MediaPlayer?) {
        // don't play next song automatically, if current song is last
        if (songIndex == songsList!!.lastIndex) return

        binding.imgNext.performClick()
    }

    companion object {
        var songsList: ArrayList<Song>? = null
            private set
        var songIndex: Int = -1
            private set
        private var musicService: MusicService? = null

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
    }
}