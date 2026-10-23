package com.example.mureev

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mureev.databinding.ActivityMostPlayedBinding

class MostPlayedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMostPlayedBinding
    private lateinit var adapter: MostPlayedAdapter

    companion object {
        var mostPlayedSongs: ArrayList<Music> = ArrayList()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivityMostPlayedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Paling Sering Diputar"

        loadMostPlayedSongs()

        binding.mostPlayedRV.setHasFixedSize(true)
        binding.mostPlayedRV.setItemViewCacheSize(10)
        binding.mostPlayedRV.layoutManager = LinearLayoutManager(this)

        adapter = MostPlayedAdapter(this, mostPlayedSongs)
        binding.mostPlayedRV.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun loadMostPlayedSongs() {
        mostPlayedSongs = ArrayList()
        val allSongs = ArrayList(MainActivity.MusicListMA)

        // Mengurutkan lagu berdasarkan jumlah putar
        allSongs.sortByDescending { MainActivity.playCountMap[it.id] ?: 0 }

        // Mengambil semua lagu yang pernah diputar (play count > 0)
        mostPlayedSongs.addAll(allSongs.filter { (MainActivity.playCountMap[it.id] ?: 0) > 0 })

        if (mostPlayedSongs.isEmpty()) {
            binding.linearLayout.visibility = View.GONE
            binding.emptyStateMP.visibility = View.VISIBLE
        } else {
            binding.linearLayout.visibility = View.VISIBLE
            binding.emptyStateMP.visibility = View.GONE
            binding.totalSongsMostPlayed.text = "Total Lagu : ${mostPlayedSongs.size}"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}