package app.entertainment.musicplayer.activities

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.entertainment.musicplayer.adapters.SongAdapter
import app.entertainment.musicplayer.databinding.ActivityMainBinding
import app.entertainment.musicplayer.models.Song
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
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
        checkPermissions()
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
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
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

        cursor.apply {
            val titleColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val filePathColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (moveToNext()) {
                val filePath = getString(filePathColumn)

                // Skip audio files from specific directories or non-MP3 files
                if (filePath.contains("whatsapp") || !filePath.endsWith(".mp3"))
                    continue

                // Create Song object and add it to the list
                val song = Song(
                    filePath,
                    getString(titleColumn),
                    getLong(durationColumn)
                )

                // Check if the file associated with the song exists and has a non-zero length.
                // This ensures that only valid and non-empty song files are added to the list.
                val file = File(song.path)
                if (file.exists() && file.length() > 0)
                    songsList.add(song)
            }
            // Close the cursor to release associated resources
            cursor.close()
        }

        // Show the list of songs or a "no songs found" message
        if (songsList.isEmpty())
            binding.tvNoSongsFound.visibility = View.VISIBLE
        else
            showSongsList()
    }

    private fun showSongsList() {
        // Configure the RecyclerView to display the list of songs
        binding.recyclerViewSongs.apply {
            layoutManager = this@MainActivity.layoutManager
            setHasFixedSize(true)
            adapter = SongAdapter(songsList, this@MainActivity).also {
                it.notifyDataSetChanged()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayListOf<String>()
        permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)

        // Check for additional permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)

        // Request the specified permissions using Dexter library
        permissions.forEach(::actAccordingToPermission)
    }

    /**
     * Performs actions based on permission's denial or acceptance
     */
    private fun actAccordingToPermission(permission: String) {
        Dexter.withContext(this@MainActivity)
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    // Permission granted, query and display audio files
                    queryAudioFiles()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    // Permission denied, show a toast message
                    Toast.makeText(
                        this@MainActivity,
                        "Storage permission is necessary to read files!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    permissionToken: PermissionToken?
                ) {
                    // Continue permission request if needed
                    permissionToken!!.continuePermissionRequest()
                }
            }).check()
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressCount != 0) {
            finish()
            return
        }

        Toast.makeText(this, "Press back button once again to exit the app", Toast.LENGTH_SHORT)
            .show()
        backPressCount++
    }

    override fun onResume() {
        super.onResume()

        binding.recyclerViewSongs.adapter = SongAdapter(songsList, this).also {
            it.notifyDataSetChanged()
        }

        // Scroll to current playing song
        val songIndex = MusicPlayerActivity.songIndex
        if (songIndex != RecyclerView.NO_POSITION)
            layoutManager.scrollToPosition(songIndex)
    }
}