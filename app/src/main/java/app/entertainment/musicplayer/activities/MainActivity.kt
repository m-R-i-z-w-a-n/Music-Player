package app.entertainment.musicplayer.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.entertainment.musicplayer.adapters.SongAdapter
import app.entertainment.musicplayer.databinding.ActivityMainBinding
import app.entertainment.musicplayer.models.Song
import java.io.File


class MainActivity : AppCompatActivity() {

    private val songsList = ArrayList<Song>()
    private lateinit var layoutManager: LinearLayoutManager
    private var backPressCount: Int = 0

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        layoutManager = LinearLayoutManager(this)

        // Check for necessary permissions
        if (isPermissionGranted())
            queryAudioFiles()
        else
            requestStoragePermission()
    }

    private fun queryAudioFiles() {
        // Define the columns to retrieve from the MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // Determine the appropriate URI based on Android version
        val songLibraryUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        // Query the MediaStore for audio files
        val cursor = contentResolver.query(
            songLibraryUri,
            projection,
            selection,
            null,
            sortOrder
        ) as Cursor

        cursor.use {
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val filePathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val filePath = it.getString(filePathColumn) ?: continue

                // Skip audio files from specific directories or non-MP3 files
                if (filePath.contains("whatsapp") || !filePath.endsWith(".mp3"))
                    continue

                val song = Song(
                    filePath,
                    it.getString(titleColumn),
                    it.getLong(durationColumn)
                )

                // Check if the file associated with the song exists and has a non-zero length.
                // This ensures that only valid and non-empty song files are added to the list.
                val file = File(song.path)
                if (file.exists() && file.length() > 0)
                    songsList.add(song)
            }
        }

        // Show the list of songs or a "no songs found" message
        if (songsList.isEmpty())
            binding.tvNoSongsFound.visibility = View.VISIBLE
        else
            showSongsList(songsList)
    }

    private fun showSongsList(songsList: ArrayList<Song>) {
        // Configure the RecyclerView to display the list of songs
        binding.recyclerViewSongs.apply {
            layoutManager = this@MainActivity.layoutManager
            setHasFixedSize(true)
            adapter = SongAdapter(this@MainActivity, lifecycleScope).also {
                it.updateList(songsList)
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                AUDIO_FILES_PERMISSION_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                AUDIO_FILES_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == AUDIO_FILES_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                queryAudioFiles()
            }
            else {
                Toast.makeText(this, "Storage permission is necessary to read files!", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressCount != 0) {
            finishAffinity()
            return
        }

        Toast.makeText(this, "Press back button once again to exit the app", Toast.LENGTH_SHORT)
            .show()
        backPressCount++
    }

    override fun onResume() {
        super.onResume()
//
//        binding.recyclerViewSongs.adapter = SongAdapter(this).also {
//            it.updateList(songsList)
//        }

        // Scroll to current playing song
        val songIndex = MusicPlayerActivity.songIndex
        if (songIndex != RecyclerView.NO_POSITION)
            layoutManager.scrollToPosition(songIndex)
    }

    private companion object {
        private const val AUDIO_FILES_PERMISSION_CODE = 999
    }
}