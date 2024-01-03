package app.entertainment.musicplayer.notifications

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import app.entertainment.musicplayer.R
import app.entertainment.musicplayer.activities.MusicPlayerActivity
import app.entertainment.musicplayer.services.MusicService
import app.entertainment.musicplayer.utils.CHANNEL_ID
import app.entertainment.musicplayer.utils.NEXT
import app.entertainment.musicplayer.utils.PLAY_PAUSE
import app.entertainment.musicplayer.utils.PREVIOUS
import app.entertainment.musicplayer.utils.STOP
import app.entertainment.musicplayer.utils.getImageArt

class MusicApplication : Application() {
    // This method is called when the application is created
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(this, "My Music")
        notificationManager = getNotificationManager(this)
    }

    companion object {
        private lateinit var mediaSession: MediaSessionCompat
        private lateinit var notificationManager: NotificationManager

        private const val NOTIFICATION_ID = 1

        /**
         * Shows a notification with playback controls
         */
        @SuppressLint("NotificationPermission", "MissingPermission")
        fun showNotification(
            context: Context,
            musicService: MusicService,
            playPauseButtonId: Int = R.drawable.ic_notification_pause_circle_24
        ) {
            val notificationBuilder = getNotificationBuilder(context, playPauseButtonId)
            val notification = notificationBuilder.build()

            musicService.startForeground(NOTIFICATION_ID, notification)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun dismissNotification() = notificationManager.cancel(NOTIFICATION_ID)

        /**
         * Creates a notification with playback controls
         */
        private fun getNotificationBuilder(
            context: Context,
            playPauseButtonId: Int
        ): NotificationCompat.Builder {
            // Create Intents and PendingIntents for notification actions
            val previousIntent = Intent(context, NotificationReceiver::class.java).setAction(PREVIOUS)
            val previousPendingIntent = PendingIntent.getBroadcast(context, 0, previousIntent, PendingIntent.FLAG_IMMUTABLE)

            val playPauseIntent = Intent(context, NotificationReceiver::class.java).setAction(PLAY_PAUSE)
            val playPausePendingIntent = PendingIntent.getBroadcast(context, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)

            val nextIntent = Intent(context, NotificationReceiver::class.java).setAction(NEXT)
            val nextPendingIntent = PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)

            val stopIntent = Intent(context, NotificationReceiver::class.java).setAction(STOP)
            val stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

            val activityIntent = Intent(context, MusicPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val activityPendingIntent = PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            return NotificationCompat.Builder(context, CHANNEL_ID).apply {
                // Set notification title, icons, and style
                setContentTitle(MusicPlayerActivity.songsList!![MusicPlayerActivity.songIndex].title)
                setSmallIcon(R.drawable.ic_music_icon)
                setLargeIcon(getIcon(context))
                setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
                priority = NotificationCompat.PRIORITY_DEFAULT
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Disable notification sound
                setSilent(true)

                // Set notification actions (play/pause, previous, next, stop)
                addAction(
                    R.drawable.ic_notification_skip_previous_24,
                    "Previous",
                    previousPendingIntent
                )

                addAction(
                    playPauseButtonId,
                    "Play/Pause",
                    playPausePendingIntent
                )

                addAction(
                    R.drawable.ic_notification_skip_next_24,
                    "Next",
                    nextPendingIntent
                )

                addAction(
                    R.drawable.ic_notification_stop,
                    "Stop",
                    stopPendingIntent
                )

//                setContentIntent(activityPendingIntent)
            }
        }

        private fun getNotificationManager(context: Context): NotificationManager {
            // Create a notification channel with a unique ID, name, and importance level
            val notificationManger =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Music",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManger.createNotificationChannel(notificationChannel)
            }
            return notificationManger
        }

        /**
         * Load album art or use a default image
         */
        private fun getIcon(context: Context): Bitmap {
            val imageArt =
                getImageArt(MusicPlayerActivity.songsList!![MusicPlayerActivity.songIndex].path)

            return imageArt?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_music_icon_big)

        }
    }
}