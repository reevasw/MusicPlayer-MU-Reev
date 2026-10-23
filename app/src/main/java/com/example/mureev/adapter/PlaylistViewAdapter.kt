package com.example.mureev

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.mureev.databinding.PlaylistViewBinding
import com.example.mureev.databinding.AddPlaylistDialogBinding

class PlaylistViewAdapter(private val context: Context, private var playlistList: ArrayList<Playlist>) : RecyclerView.Adapter<PlaylistViewAdapter.MyHolder>() {

    class MyHolder(binding: PlaylistViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.playlistImg
        val name = binding.playlistName
        val songCount = binding.playlistSongCount
        val root = binding.root
        val delete = binding.playlistDeleteBtn
        val edit = binding.playlistEditBtn
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(PlaylistViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val currentPlaylist = playlistList[position]

        if(MainActivity.themeIndex == 4){
            holder.root.strokeColor = ContextCompat.getColor(context, R.color.white)
            holder.root.strokeWidth = 1
        }
        holder.name.text = currentPlaylist.name
        holder.name.isSelected = true
        val songCounterText = "${currentPlaylist.playlist.size} lagu"
        holder.songCount.text = songCounterText
        
        holder.delete.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(currentPlaylist.name)
                .setMessage("Apakah kamu ingin menghapus playlist?")
                .setPositiveButton("Ya"){ dialog, _ ->
                    PlaylistActivity.musicPlaylist.ref.removeAt(position)
                    refreshPlaylist()
                    dialog.dismiss()
                }
                .setNegativeButton("Tidak"){dialog, _ ->
                    dialog.dismiss()
                }
            val customDialog = builder.create()
            customDialog.show()
            setDialogBtnBackground(context, customDialog)
        }

        holder.edit.setOnClickListener {
            customEditDialog(position)
        }

        holder.root.setOnClickListener {
            val intent = Intent(context, PlaylistDetails::class.java)
            intent.putExtra("index", position)
            ContextCompat.startActivity(context, intent, null)
        }

        if(currentPlaylist.playlist.size > 0){
            Glide.with(context)
                .load(currentPlaylist.playlist[0].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(holder.image)
        } else {
            Glide.with(context)
                .load(R.drawable.music_player_icon_slash_screen)
                .into(holder.image)
        }
    }

    private fun customEditDialog(position: Int) {
        val customDialog = LayoutInflater.from(context).inflate(R.layout.add_playlist_dialog, null)
        val binder = AddPlaylistDialogBinding.bind(customDialog)
        val currentPlaylist = PlaylistActivity.musicPlaylist.ref[position]
        
        binder.playlistName.setText(currentPlaylist.name)
        binder.yourName.setText(currentPlaylist.createdBy)
        
        val builder = MaterialAlertDialogBuilder(context)
        val dialog = builder.setView(customDialog)
            .setTitle("Edit Playlist")
            .setPositiveButton("UPDATE"){ dialog, _ ->
                val playlistName = binder.playlistName.text
                val createdBy = binder.yourName.text
                if(playlistName != null && createdBy != null)
                    if(playlistName.isNotEmpty() && createdBy.isNotEmpty())
                    {
                        PlaylistActivity.musicPlaylist.ref[position].name = playlistName.toString()
                        PlaylistActivity.musicPlaylist.ref[position].createdBy = createdBy.toString()
                        refreshPlaylist()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL") { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.show()
        setDialogBtnBackground(context, dialog)
    }

    override fun getItemCount(): Int {
        return playlistList.size
    }

    fun refreshPlaylist(){
        playlistList = ArrayList()
        playlistList.addAll(PlaylistActivity.musicPlaylist.ref)
        notifyDataSetChanged()
    }
}