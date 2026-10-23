package com.example.mureev

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mureev.databinding.ArtistAlbumViewBinding

class ArtistAlbumAdapter(
    private val context: Context,
    private var list: ArrayList<String>,
    private val type: String,
    private val playCounts: Map<String, Int>? = null // Parameter untuk menampung data jumlah putar
) : RecyclerView.Adapter<ArtistAlbumAdapter.ViewHolder>() {

    class ViewHolder(binding: ArtistAlbumViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val name = binding.itemName
        val image = binding.itemImage
        val songCount = binding.itemSongCount
        val card = binding.itemCard
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ArtistAlbumViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemName = list[position]
        holder.name.text = itemName

        // Cek apakah adapter sedang dalam mode "Most Played" (jika playCounts tidak null)
        if (playCounts != null) {
            // Jika ya, tampilkan jumlah putar
            val count = playCounts[itemName] ?: 0
            holder.songCount.text = "$count x diputar"
        } else {
            // Jika tidak (mode normal), tampilkan jumlah lagu
            val count = if (type == "artist") {
                MainActivity.MusicListMA.count { it.artist == itemName }
            } else {
                MainActivity.MusicListMA.count { it.album == itemName }
            }
            holder.songCount.text = if (count > 1) "$count lagu" else "$count lagu"
        }

        // Logika untuk mencari cover art tetap sama
        val artUri = if (type == "artist") {
            MainActivity.MusicListMA.firstOrNull { it.artist == itemName }?.artUri
        } else {
            MainActivity.MusicListMA.firstOrNull { it.album == itemName }?.artUri
        } ?: ""

        // Set warna dan placeholder default
        val defaultColor = ContextCompat.getColor(context, R.color.radakireng)
        val placeholder = ContextCompat.getDrawable(context, R.drawable.music_player_icon_slash_screen)
        holder.card.setCardBackgroundColor(defaultColor)

        // Logika Glide untuk memuat gambar dan Palette untuk warna dinamis
        Glide.with(context)
            .asBitmap()
            .load(artUri)
            .apply(RequestOptions().placeholder(placeholder).centerCrop())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.image.setImageBitmap(resource)
                    Palette.from(resource).generate { palette ->
                        val swatch = palette?.darkVibrantSwatch ?: palette?.vibrantSwatch
                        swatch?.rgb?.let { color ->
                            holder.card.setCardBackgroundColor(color)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    holder.image.setImageDrawable(placeholder)
                }
            })

        // Logika klik untuk membuka halaman detail
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("filter_type", type)
            intent.putExtra("filter_name", itemName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateList(newList: ArrayList<String>) {
        list = newList
        notifyDataSetChanged()
    }
}