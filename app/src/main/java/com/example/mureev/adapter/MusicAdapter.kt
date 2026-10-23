package com.example.mureev.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SectionIndexer
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.*
import com.example.mureev.databinding.DetailsViewBinding
import com.example.mureev.databinding.MoreFeaturesBinding
import com.example.mureev.databinding.MusicViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MusicAdapter(
    private val context: Context,
    private var musicList: ArrayList<Music>,
    private val playlistDetails: Boolean = false,
    private val selectionActivity: Boolean = false,
    private val fromDetail: Boolean = false
) : RecyclerView.Adapter<MusicAdapter.MyHolder>(), SectionIndexer {

    // Variable untuk SectionIndexer
    private var sections = ""
    private var sectionPositions = ArrayList<Int>()

    init {
        updateSections()
    }

    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val artist = binding.songArtisMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val root = binding.root
        val dragHandle = binding.dragHandle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.title.text = musicList[position].title
        val artistName = musicList[position].artist
        val albumName = musicList[position].album
        holder.artist.text = "$artistName / $albumName"
        holder.duration.text = formatDuration(musicList[position].duration)

        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        // Tampilkan drag handle hanya di detail playlist
        if (playlistDetails) {
            holder.dragHandle.visibility = android.view.View.VISIBLE
        } else {
            holder.dragHandle.visibility = android.view.View.GONE
        }

        // Logika untuk klik biasa (memutar lagu)
        when {
            playlistDetails -> {
                holder.root.setOnClickListener {
                    sendIntent(ref = "PlaylistDetailsAdapter", pos = position)
                }
            }
            selectionActivity -> {
                val isSelected = isSongInPlaylist(musicList[position])
                if (isSelected) {
                    holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_blue))
                } else {
                    holder.root.setBackgroundColor(Color.TRANSPARENT)
                }

                holder.root.setOnClickListener {
                    if (addSong(musicList[position])) {
                        holder.root.setBackgroundColor(ContextCompat.getColor(context, R.color.cool_blue))
                    } else {
                        holder.root.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }
            else -> {
                holder.root.setOnClickListener {
                    if(fromDetail){
                        sendIntent(ref = "DetailAdapter", pos = position)
                    } else {
                        when{
                            MainActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)
                            musicList[position].id == PlayerActivity.nowPlayingId ->
                                sendIntent(ref = "NowPlaying", pos = PlayerActivity.songPosition)
                            else->sendIntent(ref="MusicAdapter", pos = position)
                        }
                    }
                }
            }
        }

        // Logika untuk long-press (menampilkan fitur tambahan)
        holder.root.setOnLongClickListener {
            val customDialog = LayoutInflater.from(context).inflate(R.layout.more_features, holder.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context).setView(customDialog)
                .create()
            dialog.show()
            dialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))

            if (playlistDetails) {
                bindingMF.addToQueueBtn.text = "Hapus Lagu"
                bindingMF.addToQueueBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.delete_icon, 0, 0, 0)
                bindingMF.addToQueueBtn.setOnClickListener {
                    PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(position)
                    refreshPlaylist()
                    dialog.dismiss()
                    Toast.makeText(context, "Lagu dihapus dari playlist", Toast.LENGTH_SHORT).show()
                }
            } else {
                bindingMF.addToQueueBtn.setOnClickListener {
                    PlayerActivity.musicListPA.add(musicList[position])
                    Toast.makeText(context, "Ditambahkan ke antrean", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            bindingMF.favouriteHoldBtn.setOnClickListener {
                FavouriteActivity.favouriteSongs.add(musicList[position])
                FavouriteActivity.favouritesChanged = true
                Toast.makeText(context, "Lagu ditambah ke favorit", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            bindingMF.playNextBtn.setOnClickListener {
                PlayerActivity.musicListPA.add(PlayerActivity.songPosition + 1, musicList[position])
                Toast.makeText(context, "Ditambahkan berikutnya", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            bindingMF.infoBtn.setOnClickListener {
                dialog.dismiss()
                val detailsDialog = LayoutInflater.from(context).inflate(R.layout.details_view, bindingMF.root, false)
                val binder = DetailsViewBinding.bind(detailsDialog)
                binder.detailsTV.setTextColor(Color.WHITE)
                binder.root.setBackgroundColor(Color.TRANSPARENT)
                val dDialog = MaterialAlertDialogBuilder(context)
                    .setView(detailsDialog)
                    .setPositiveButton("OK") { self, _ -> self.dismiss() }
                    .setCancelable(false)
                    .create()
                dDialog.show()
                dDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                dDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))

                val currentSong = musicList[position]
                val file = File(currentSong.path)

                var techDetails = "N/A"

                try {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val header = audioFile.audioHeader
                    val sampleRate = "${header.sampleRateAsNumber / 1000.0} kHz"
                    val bitDepth = "${header.bitsPerSample} bit"
                    val bitrate = "${header.bitRate} Kbps"
                    techDetails = "$sampleRate • $bitDepth • $bitrate"
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val str = SpannableStringBuilder()
                    .bold { append("DETAIL LAGU\n\nNama: ") }.append(currentSong.title)
                    .bold { append("\n\nAlbum: ") }.append(currentSong.album)
                    .bold { append("\n\nArtis: ") }.append(currentSong.artist)
                    .bold { append("\n\nDurasi: ") }.append(DateUtils.formatElapsedTime(currentSong.duration / 1000))
                    .bold { append("\n\nUkuran File: ") }.append(formatSize(file.length()))
                    .bold { append("\n\nFormat: ") }.append(file.extension.uppercase())
                    .bold { append("\n\nKualitas Audio: ") }.append(techDetails)
                    .bold { append("\n\nTanggal Ditambahkan: ") }.append(formatDate(file.lastModified()))
                    .bold { append("\n\nJumlah Diputar: ") }.append("${MainActivity.playCountMap.getOrDefault(currentSong.id, 0)} kali")
                    .bold { append("\n\nLokasi: ") }.append(currentSong.path)

                binder.detailsTV.text = str
            }
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    // -- SectionIndexer Implementation --

    override fun getSections(): Array<Any> {
        val sectionsArray = ArrayList<String>()
        for (i in sections.indices) {
            sectionsArray.add(sections[i].toString())
        }
        return sectionsArray.toArray()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return if (sectionIndex in sectionPositions.indices) sectionPositions[sectionIndex] else 0
    }

    override fun getSectionForPosition(position: Int): Int {
        for (i in sectionPositions.size - 1 downTo 0) {
            if (position >= sectionPositions[i]) return i
        }
        return 0
    }

    private fun updateSections() {
        val sectionList = ArrayList<String>()
        sectionPositions = ArrayList()
        val tempSections = StringBuilder()

        // Sort musicList by title for correct sectioning
        // Only if we want sectioning to make sense (usually used when sorted A-Z)

        musicList.forEachIndexed { index, music ->
            val firstChar = if (music.title.isNotEmpty()) music.title[0].uppercaseChar().toString() else "#"
            if (!sectionList.contains(firstChar)) {
                sectionList.add(firstChar)
                tempSections.append(firstChar)
                sectionPositions.add(index)
            }
        }
        sections = tempSections.toString()
    }

    // -- Helper Functions --

    fun updateMusicList(searchList: ArrayList<Music>) {
        musicList = ArrayList()
        musicList.addAll(searchList)
        updateSections()
        notifyDataSetChanged()
    }

    private fun sendIntent(ref: String, pos: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    private fun isSongInPlaylist(song: Music): Boolean {
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEach { music ->
            if (song.id == music.id) return true
        }
        return false
    }

    private fun addSong(song: Music): Boolean {
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.forEachIndexed { index, music ->
            if (song.id == music.id) {
                PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.removeAt(index)
                return false
            }
        }
        PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist.add(song)
        return true
    }

    fun refreshPlaylist() {
        musicList = ArrayList()
        musicList.addAll(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist)
        updateSections()
        notifyDataSetChanged()
    }

    fun onMove(fromPosition: Int, toPosition: Int) {
        val movedItem = musicList.removeAt(fromPosition)
        musicList.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 0) return "N/A"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        return format.format(date)
    }
}
