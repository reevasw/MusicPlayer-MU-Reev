package com.example.mureev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mureev.databinding.ActivityArtistBinding
import java.util.Locale

class ArtistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtistBinding
    private lateinit var adapter: ArtistAlbumAdapter
    private lateinit var originalList: ArrayList<String>
    private var playCounts: Map<String, Int>? = null
    private lateinit var voiceSearchLauncher: ActivityResultLauncher<Intent>
    private var searchView: SearchView? = null

    private lateinit var voiceCommandProcessor: VoiceCommandProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivityArtistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        voiceCommandProcessor = VoiceCommandProcessor(this)

        // Initialize Voice Search Launcher
        voiceSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val values = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val voiceQuery = values?.get(0)
                if (!voiceQuery.isNullOrEmpty()) {
                    voiceCommandProcessor.processCommand(voiceQuery) { query ->
                        searchView?.isIconified = false
                        searchView?.setQuery(query, true)
                    }
                }
            }
        }

        val showMostPlayed = intent.getBooleanExtra("most_played", false)
        var artistList: List<String>

        if (showMostPlayed) {
            supportActionBar?.title = "Artis Terpopuler"
            val artistPlayCounts = mutableMapOf<String, Int>()
            MainActivity.playCountMap.forEach { (songId, playCount) ->
                val song = MainActivity.MusicListMA.find { it.id == songId }
                song?.let {
                    artistPlayCounts[it.artist] = artistPlayCounts.getOrDefault(it.artist, 0) + playCount
                }
            }
            artistList = artistPlayCounts.keys.sortedByDescending { artistPlayCounts[it] ?: 0 }
            this.playCounts = artistPlayCounts
        } else {
            supportActionBar?.title = "Daftar Artis"
            artistList = MainActivity.MusicListMA.map { it.artist }.distinct().sorted()
        }

        originalList = ArrayList(artistList)
        binding.artistRV.setHasFixedSize(true)
        binding.artistRV.layoutManager = GridLayoutManager(this, 2)
        adapter = ArtistAlbumAdapter(this, ArrayList(artistList), "artist")
        binding.artistRV.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_view_menu, menu)
        val searchItem = menu?.findItem(R.id.searchView)
        searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = "Cari Artist..."

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = originalList.filter {
                    it.contains(newText ?: "", ignoreCase = true)
                }
                adapter.updateList(ArrayList(filteredList))
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.voiceSearch -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama artis...")
                try {
                    voiceSearchLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Voice search tidak tersedia", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
