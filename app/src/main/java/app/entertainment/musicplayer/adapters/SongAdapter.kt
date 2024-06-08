package app.entertainment.musicplayer.adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import app.entertainment.musicplayer.R
import app.entertainment.musicplayer.activities.MusicPlayerActivity
import app.entertainment.musicplayer.databinding.ItemSongBinding
import app.entertainment.musicplayer.models.Song
import app.entertainment.musicplayer.utils.getImageArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongAdapter(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val mDiffer by lazy { AsyncListDiffer(this, DIFF_CALLBACK) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            ItemSongBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun getItemCount(): Int = mDiffer.currentList.size

    // Bind data to each item in the RecyclerView
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = mDiffer.currentList[position]
        holder.binding.apply {
            tvSongTitle.text = song.title

            // Highlight the currently playing song with a different text color
            if (MusicPlayerActivity.songIndex == position)
                tvSongTitle.setTextColor(Color.parseColor("#FF131A9E"))
            else
                tvSongTitle.setTextColor(Color.parseColor("#000000"))

            holder.itemView.setOnClickListener {
                Intent(context, MusicPlayerActivity::class.java).also { intent ->
                    intent.apply {
                        putExtra("LIST", ArrayList(mDiffer.currentList))
                        putExtra("index", position)
//                    putExtra("class", "SongAdapter")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val icon = getImageArt(song.path)?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_music_icon)
            withContext(Dispatchers.Main) {
                holder.binding.imgMusicIcon.setImageBitmap(icon)
            }
        }
    }

    fun updateList(songs: List<Song>) = mDiffer.submitList(songs)

    inner class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem.path == newItem.path

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem == newItem
        }
    }
}