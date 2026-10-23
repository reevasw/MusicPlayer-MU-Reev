package com.example.mureev

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.adapter.MusicAdapter
import com.example.mureev.databinding.ActivityDetailBinding
import java.util.concurrent.TimeUnit

class DetailActivity : AppCompatActivity() {

    lateinit var binding: ActivityDetailBinding
    private lateinit var musicAdapter: MusicAdapter

    companion object {
        var songList: ArrayList<Music> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filterType = intent.getStringExtra("filter_type")
        val filterName = intent.getStringExtra("filter_name")

        if (filterType == null || filterName == null) {
            finish()
            return
        }

        // Filter lagu dan siapkan data
        songList = if (filterType == "artist") {
            ArrayList(MainActivity.MusicListMA.filter { it.artist == filterName })
        } else {
            ArrayList(MainActivity.MusicListMA.filter { it.album == filterName })
        }

        // Set UI
        binding.collapsingToolbar.title = filterName
        binding.detailArtistName.text = if(filterType == "artist") filterName else songList.firstOrNull()?.artist
        binding.detailStats.text = getStatsText()

        // Set Cover Art
        val artUri = songList.firstOrNull()?.artUri ?: ""
        Glide.with(this)
            .load(artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen))
            .into(binding.detailImageView)

        // Setup RecyclerView
        binding.detailSongsRV.layoutManager = LinearLayoutManager(this)
        musicAdapter = MusicAdapter(this, songList, fromDetail = true) // Tambahkan flag baru
        binding.detailSongsRV.adapter = musicAdapter

        binding.detailPlayAllBtn.setOnClickListener {
            // Buat intent untuk membuka PlayerActivity
            val intent = Intent(this, PlayerActivity::class.java)

            // Kirim referensi bahwa daftar lagu berasal dari halaman ini
            intent.putExtra("class", "DetailAdapter")

            // Atur agar pemutaran dimulai dari lagu pertama (indeks 0)
            intent.putExtra("index", 0)

            // Mulai PlayerActivity
            startActivity(intent)
        }
    }

    private fun getStatsText(): String {
        val totalTracks = songList.size
        val totalDurationMs = songList.sumOf { it.duration }
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationMs) % 60
        val durationStr = String.format("%02d:%02d", minutes, seconds)
        return "Tracks: $totalTracks / Duration: $durationStr"
    }
}