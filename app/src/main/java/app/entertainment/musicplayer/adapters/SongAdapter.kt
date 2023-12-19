package app.entertainment.musicplayer.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.entertainment.musicplayer.activities.MusicPlayerActivity
import app.entertainment.musicplayer.R
import app.entertainment.musicplayer.models.Song

class SongAdapter(private val songsList: ArrayList<Song>, private val context: Context) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    // Create a view holder for each item in the RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(LayoutInflater.from(context).inflate(R.layout.item_song, parent, false))
    }

    // Return the total number of items in the data set
    override fun getItemCount(): Int {
        return songsList.size
    }

    // Bind data to each item in the RecyclerView
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songsList[position]
        holder.tvSongTitle.text = song.title

        // Highlight the currently playing song with a different text color
        if (MusicPlayerActivity.songIndex == position)
            holder.tvSongTitle.setTextColor(Color.parseColor("#FF131A9E"))
        else
            holder.tvSongTitle.setTextColor(Color.parseColor("#000000"))

        // Handle item click to open the MusicPlayerActivity for song playback
        holder.itemView.setOnClickListener {
            Intent(context, MusicPlayerActivity::class.java).also { intent ->
                intent.apply {
                    putExtra("LIST", songsList)
                    putExtra("index", position)
//                    putExtra("class", "SongAdapter")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    // View holder class to hold references to the UI components
    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSongTitle: TextView

        init {
            tvSongTitle = itemView.findViewById(R.id.tv_song_title)
        }
    }
}