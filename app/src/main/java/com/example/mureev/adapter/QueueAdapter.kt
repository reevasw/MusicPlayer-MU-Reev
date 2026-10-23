package com.example.mureev

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.databinding.QueueItemViewBinding

// 1. Tambahkan parameter baru di konstruktor: itemClickListener
class QueueAdapter(
    private val context: Context,
    private var queueList: ArrayList<Music>,
    private val itemClickListener: (position: Int) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    class ViewHolder(binding: QueueItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.queueItemTitle
        val artist = binding.queueItemArtist
        val image = binding.albumCover
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(QueueItemViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSong = queueList[position]
        holder.title.text = currentSong.title
        holder.artist.text = currentSong.artist

        Glide.with(context)
            .load(currentSong.artUri)
            .apply(RequestOptions().placeholder(R.mipmap.music_player_icon).centerCrop())
            .into(holder.image)

        // 2. Tambahkan OnClickListener pada setiap item
        holder.itemView.setOnClickListener {
            itemClickListener(position)
        }

        // Logika untuk menandai lagu yang sedang diputar (tetap sama)
        if (PlayerActivity.songPosition == position) {
            holder.title.setTextColor(context.getColor(R.color.yellow))
        } else {
            holder.title.setTextColor(context.getColor(R.color.white))
        }
    }

    override fun getItemCount(): Int = queueList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateQueue(newList: ArrayList<Music>) {
        queueList = newList
        notifyDataSetChanged()
    }
}
