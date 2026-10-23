package com.example.mureev

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.adapter.MusicAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.example.mureev.databinding.ActivityPlaylistDetailsBinding
import java.util.concurrent.TimeUnit

class PlaylistDetails : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private lateinit var adapter: MusicAdapter

    companion object{
        var currentPlaylistPos: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BARU: Set up Toolbar kustom
        setSupportActionBar(binding.toolbarPD)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarPD.setNavigationOnClickListener {
            finish()
        }

        currentPlaylistPos = intent.extras?.get("index") as Int
        try {
            PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist =
                checkPlaylist(playlist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist)
        } catch(e: Exception){}

        // Setup RecyclerView
        binding.playlistDetailsRV.setItemViewCacheSize(10)
        binding.playlistDetailsRV.setHasFixedSize(true)
        binding.playlistDetailsRV.layoutManager = LinearLayoutManager(this)
        // PENTING: nonaktifkan scroll di RecyclerView karena NestedScrollView sudah menanganinya
        binding.playlistDetailsRV.isNestedScrollingEnabled = false

        adapter = MusicAdapter(this, PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist, playlistDetails = true)
        binding.playlistDetailsRV.adapter = adapter

        // Setup ItemTouchHelper untuk drag & drop (reorder lagu)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.onMove(fromPosition, toPosition)
                savePlaylist() // Simpan urutan baru
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Tidak ada aksi swipe
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.playlistDetailsRV)

        // Listener untuk tombol-tombol (logika tetap sama)
        binding.addBtnPD.setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
        }

        binding.removeAllPD.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Remove")
                .setMessage("Apakah kamu ingin menghapus semua lagu dari Playlist?")
                .setPositiveButton("Ya"){ dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref[currentPlaylistPos].playlist.clear()
                    adapter.refreshPlaylist()
                    savePlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak"){dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()
        }

        binding.shuffleBtnPD.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlaylistDetailsShuffle")
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        val currentPlaylist = PlaylistActivity.musicPlaylist.ref[currentPlaylistPos]

        // DIUBAH: Set judul di CollapsingToolbar, bukan TextView terpisah
        binding.collapsingToolbar.title = currentPlaylist.name

        // DIUBAH: Hitung total durasi dan format informasi detail
        val totalSongs = adapter.itemCount
        val totalDurationMs = currentPlaylist.playlist.sumOf { it.duration }

        binding.playlistDetailsInfo.text = "Dibuat oleh: ${currentPlaylist.createdBy}\n" +
                "$totalSongs lagu • ${formatDuration(totalDurationMs)}"

        if(totalSongs > 0) {
            Glide.with(this)
                .load(currentPlaylist.playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.playlistImgPD)
            binding.shuffleBtnPD.visibility = View.VISIBLE
        } else {
            binding.shuffleBtnPD.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
        savePlaylist()
    }

    private fun savePlaylist() {
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonStringPlaylist = GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist)
        editor.putString("MusicPlaylist", jsonStringPlaylist)
        editor.apply()
    }

    // FUNGSI BANTUAN: untuk format durasi dari milidetik ke jam/menit/detik
    private fun formatDuration(duration: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

        return if (hours > 0) {
            String.format("%d jam %02d mnt %02d dtk", hours, minutes, seconds)
        } else {
            String.format("%d mnt %02d dtk", minutes, seconds)
        }
    }
}