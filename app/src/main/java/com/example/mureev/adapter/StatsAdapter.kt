package com.example.mureev.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.R
import com.example.mureev.StatsItem
import com.example.mureev.databinding.StatsItemViewBinding

class StatsAdapter(
    private val context: Context,
    private val items: List<StatsItem>,
    private val onItemClick: (StatsItem) -> Unit
) : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    class ViewHolder(binding: StatsItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val rank = binding.statsRank
        val image = binding.statsImage
        val title = binding.statsTitle
        val subtitle = binding.statsSubtitle
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(StatsItemViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.rank.text = "${item.rank}."
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        Glide.with(context)
            .load(item.artUri)
            .apply(RequestOptions().placeholder(R.mipmap.music_player_icon).centerCrop())
            .into(holder.image)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
