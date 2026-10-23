package com.example.mureev

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mureev.adapter.StatsAdapter
import com.example.mureev.databinding.ActivityStatsBinding
import java.util.concurrent.TimeUnit

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private var isArtistsExpanded = false
    private var isSongsExpanded = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Statistics"

        setupGeneralStats()
        setupTopArtists()
        setupTopSongs()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleItemClick(item: StatsItem) {
        if (item.type == "song") {
            val originalIndex = MainActivity.MusicListMA.indexOfFirst { it.id == item.id }
            if (originalIndex != -1) {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("class", "StatsAdapter")
                intent.putExtra("index", originalIndex)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("filter_type", "artist")
            intent.putExtra("filter_name", item.id)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupGeneralStats() {
        val totalSongs = MainActivity.MusicListMA.size
        binding.statsTotalSongs.text = totalSongs.toString()

        val totalArtists = MainActivity.MusicListMA.map { it.artist }.distinct().size
        binding.statsTotalArtists.text = totalArtists.toString()

        val totalAlbums = MainActivity.MusicListMA.map { it.album }.distinct().size
        binding.statsTotalAlbums.text = totalAlbums.toString()

        var totalDurationMs: Long = 0
        MainActivity.playCountMap.forEach { (songId, playCount) ->
            val song = MainActivity.MusicListMA.find { it.id == songId }
            song?.let {
                totalDurationMs += it.duration * playCount
            }
        }
        val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs) % 60
        binding.statsTotalTime.text = "${hours}j ${minutes}m"
    }

    private fun setupTopArtists() {
        val artistPlayCounts = mutableMapOf<String, Int>()
        MainActivity.playCountMap.forEach { (songId, playCount) ->
            val song = MainActivity.MusicListMA.find { it.id == songId }
            song?.let {
                artistPlayCounts[it.artist] = artistPlayCounts.getOrDefault(it.artist, 0) + playCount
            }
        }

        val allArtists = artistPlayCounts.toList().sortedByDescending { it.second }
        val displayArtists = if (isArtistsExpanded) allArtists else allArtists.take(5)

        val topArtistsList = displayArtists.mapIndexed { index, pair ->
            val artistName = pair.first
            val artUri = MainActivity.MusicListMA.firstOrNull { it.artist == artistName }?.artUri ?: ""
            StatsItem(
                id = artistName,
                type = "artist",
                rank = index + 1,
                artUri = artUri,
                title = artistName,
                subtitle = "${pair.second} kali diputar"
            )
        }

        binding.topArtistsRV.layoutManager = LinearLayoutManager(this)
        binding.topArtistsRV.adapter = StatsAdapter(this, topArtistsList) { item -> handleItemClick(item) }

        binding.showMoreArtists.text = if (isArtistsExpanded) "Sembunyikan" else "Lihat Semua"
        binding.showMoreArtists.visibility = if (allArtists.size > 5) android.view.View.VISIBLE else android.view.View.GONE
        binding.showMoreArtists.setOnClickListener {
            isArtistsExpanded = !isArtistsExpanded
            setupTopArtists()
        }
    }

    private fun setupTopSongs() {
        val allSongsPair = MainActivity.playCountMap.toList().sortedByDescending { it.second }
        val displaySongs = if (isSongsExpanded) allSongsPair else allSongsPair.take(5)

        val topSongsList = displaySongs.mapIndexed { index, pair ->
            val song = MainActivity.MusicListMA.find { it.id == pair.first }
            StatsItem(
                id = song?.id ?: "",
                type = "song",
                rank = index + 1,
                artUri = song?.artUri ?: "",
                title = song?.title ?: "Lagu Tidak Ditemukan",
                subtitle = "${song?.artist ?: ""} • ${pair.second} putar"
            )
        }

        binding.topSongsRV.layoutManager = LinearLayoutManager(this)
        binding.topSongsRV.adapter = StatsAdapter(this, topSongsList) { item -> handleItemClick(item) }

        binding.showMoreSongs.text = if (isSongsExpanded) "Sembunyikan" else "Lihat Semua"
        binding.showMoreSongs.visibility = if (allSongsPair.size > 5) android.view.View.VISIBLE else android.view.View.GONE
        binding.showMoreSongs.setOnClickListener {
            isSongsExpanded = !isSongsExpanded
            setupTopSongs()
        }
    }
}
