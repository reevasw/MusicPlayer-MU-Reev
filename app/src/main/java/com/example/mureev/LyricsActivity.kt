package com.example.mureev

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.databinding.ActivityLyricsBinding
import jp.wasabeef.glide.transformations.BlurTransformation

class LyricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLyricsBinding
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var lyricLines: ArrayList<LyricLine>
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityLyricsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lyricLines = intent.getParcelableArrayListExtra("lyrics_data") ?: return // Ambil data lirik

        setupUI()
        setupSync()
    }

    private fun setupUI() {
        // Set background blur
        Glide.with(this)
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
            .into(binding.lyricsBg)

        // Setup RecyclerView
        lyricsAdapter = LyricsAdapter(lyricLines) { time ->
            // Logika untuk tap-to-seek
            PlayerActivity.musicService?.mediaPlayer?.seekTo(time.toInt())
        }
        binding.lyricsRV.adapter = lyricsAdapter
        binding.lyricsRV.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSync() {
        runnable = Runnable {
            try {
                val currentPosition = PlayerActivity.musicService!!.mediaPlayer!!.currentPosition.toLong()

                // Cari indeks baris lirik yang sesuai dengan waktu lagu
                val currentIndex = lyricLines.indexOfLast { it.time <= currentPosition }

                if (currentIndex != -1) {
                    lyricsAdapter.setCurrentLine(currentIndex)
                    (binding.lyricsRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentIndex, binding.lyricsRV.height / 3)
                }

                handler.postDelayed(runnable, 300) // Ulangi setiap 300ms
            } catch (e: Exception) {
                // Handle jika ada error
            }
        }
        handler.postDelayed(runnable, 0)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable) // Hentikan sinkronisasi saat activity dijeda
    }
}