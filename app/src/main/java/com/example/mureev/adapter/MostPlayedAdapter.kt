package com.example.mureev

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.databinding.MostPlayedViewBinding

class MostPlayedAdapter(private val context: Context, private var musicList: ArrayList<Music>)
    : RecyclerView.Adapter<MostPlayedAdapter.MyHolder>() {

    // Holder ini menggunakan binding dari most_played_view.xml
    class MyHolder(binding: MostPlayedViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val artist = binding.songArtisMV
        val image = binding.imageMV
        val rank = binding.rankTV
        val playCount = binding.playCountMV
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        // Meng-inflate layout most_played_view.xml
        return MyHolder(MostPlayedViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val currentSong = musicList[position]
        val playCount = MainActivity.playCountMap.getOrDefault(currentSong.id, 0)

        // Mengisi data ke dalam view sesuai ID di most_played_view.xml
        holder.rank.text = (position + 1).toString()
        holder.title.text = currentSong.title
        holder.artist.text = currentSong.artist
        holder.playCount.text = "$playCount x diputar"

        Glide.with(context)
            .load(currentSong.artUri)
            .apply(RequestOptions().placeholder(R.mipmap.music_player_icon).centerCrop())
            .into(holder.image)

        holder.root.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("index", position)
            intent.putExtra("class", "MostPlayedAdapter")
            ContextCompat.startActivity(context, intent, null)
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }
}