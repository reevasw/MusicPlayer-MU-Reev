package com.example.mureev

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.mureev.databinding.ActivityPlaylistBinding
import com.example.mureev.databinding.AddPlaylistDialogBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter: PlaylistViewAdapter

    companion object{
        var musicPlaylist: MusicPlaylist = MusicPlaylist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Playlist"

        // Inisialisasi Smart Playlists sebelum menampilkan
        setupSmartPlaylists()

        binding.playlistRV.setHasFixedSize(true)
        binding.playlistRV.setItemViewCacheSize(13)
        binding.playlistRV.layoutManager = GridLayoutManager(this, 2)
        adapter = PlaylistViewAdapter(this, playlistList = musicPlaylist.ref)
        binding.playlistRV.adapter = adapter
        
        binding.addPlaylistBtn.setOnClickListener { customAlertDialog() }

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (musicPlaylist.ref.isEmpty()) {
            binding.instructionPA.visibility = View.VISIBLE
        } else {
            binding.instructionPA.visibility = View.GONE
        }
    }

    private fun setupSmartPlaylists() {
        // 1. Top 20 Played
        val topSongs = MainActivity.MusicListMA
            .filter { MainActivity.playCountMap.getOrDefault(it.id, 0) > 0 }
            .sortedByDescending { MainActivity.playCountMap.getOrDefault(it.id, 0) }
            .take(20)

        updateOrCreateSmartPlaylist("Top 20 Played", topSongs)

        // 3. Forgotten Gems (Lagu yang belum pernah diputar, diambil acak)
        val forgottenGems = MainActivity.MusicListMA
            .filter { MainActivity.playCountMap.getOrDefault(it.id, 0) == 0 }
            .shuffled()
            .take(15)

        updateOrCreateSmartPlaylist("Forgotten Gems", forgottenGems)

        // Pastikan Recently Played yang lama dihapus dari daftar jika ada
        val recentIndex = musicPlaylist.ref.indexOfFirst { it.name == "Recently Played" }
        if (recentIndex != -1) {
            musicPlaylist.ref.removeAt(recentIndex)
        }
    }

    private fun updateOrCreateSmartPlaylist(name: String, songs: List<Music>) {
        val existingIndex = musicPlaylist.ref.indexOfFirst { it.name == name }

        if (songs.isNotEmpty()) {
            if (existingIndex != -1) {
                // Update lagu di playlist yang sudah ada
                musicPlaylist.ref[existingIndex].playlist = ArrayList(songs)
            } else {
                // Buat playlist baru jika belum ada
                val smartPlaylist = Playlist()
                smartPlaylist.name = name
                smartPlaylist.playlist = ArrayList(songs)
                smartPlaylist.createdBy = "System"
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                smartPlaylist.createdOn = sdf.format(Calendar.getInstance().time)

                // Masukkan ke posisi paling atas agar mudah terlihat
                musicPlaylist.ref.add(0, smartPlaylist)
            }
        }
    }

    private fun customAlertDialog(){
        val customDialog = LayoutInflater.from(this).inflate(R.layout.add_playlist_dialog, binding.root, false)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val builder = MaterialAlertDialogBuilder(this)
        val dialog = builder.setView(customDialog)
            .setTitle("Playlist Details")
            .setPositiveButton("ADD"){ dialog, _ ->
                val playlistName = binder.playlistName.text
                val createdBy = binder.yourName.text
                if(!playlistName.isNullOrEmpty() && !createdBy.isNullOrEmpty())
                {
                    addPlaylist(playlistName.toString(), createdBy.toString())
                }
                dialog.dismiss()
            }.create()
        dialog.show()
        setDialogBtnBackground(this, dialog)
    }

    private fun addPlaylist(name: String, createdBy: String){
        var playlistExists = false
        for(i in musicPlaylist.ref) {
            if (name == i.name){
                playlistExists = true
                break
            }
        }
        if(playlistExists) Toast.makeText(this, "Playlist Exist!!", Toast.LENGTH_SHORT).show()
        else {
            val tempPlaylist = Playlist()
            tempPlaylist.name = name
            tempPlaylist.playlist = ArrayList()
            tempPlaylist.createdBy = createdBy
            val calendar = Calendar.getInstance().time
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            tempPlaylist.createdOn = sdf.format(calendar)
            musicPlaylist.ref.add(tempPlaylist)
            adapter.refreshPlaylist()
            updateEmptyState()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        setupSmartPlaylists() 
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }
}
