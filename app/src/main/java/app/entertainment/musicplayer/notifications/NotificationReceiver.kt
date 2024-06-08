package app.entertainment.musicplayer.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Intent(intent?.action).also {
            context?.sendBroadcast(it)
        }
    }
}