package com.example.mureev

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mureev.databinding.LyricsItemViewBinding

class LyricsAdapter(
    private val lyrics: List<LyricLine>,
    private val onLineClickListener: (Long) -> Unit // Listener untuk tap-to-seek
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    private var currentLineIndex = -1

    class ViewHolder(val binding: LyricsItemViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LyricsItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = lyrics.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = lyrics[position]
        holder.binding.lyricLineText.text = line.text

        // Logika Efek Fokus
        when {
            position == currentLineIndex -> {
                // Baris aktif: terang, besar
                holder.binding.lyricLineText.isSelected = true
                holder.binding.lyricLineText.alpha = 1.0f
                holder.binding.lyricLineText.textSize = 22f
            }
            position == currentLineIndex - 1 || position == currentLineIndex + 1 -> {
                // Baris tetangga: sedikit redup, ukuran normal
                holder.binding.lyricLineText.isSelected = false
                holder.binding.lyricLineText.alpha = 0.6f
                holder.binding.lyricLineText.textSize = 20f
            }
            else -> {
                // Baris lain: sangat redup, lebih kecil
                holder.binding.lyricLineText.isSelected = false
                holder.binding.lyricLineText.alpha = 0.4f
                holder.binding.lyricLineText.textSize = 18f
            }
        }

        holder.itemView.setOnClickListener {
            onLineClickListener(line.time)
        }
    }

    fun setCurrentLine(index: Int) {
        if (currentLineIndex != index) {
            val oldIndex = currentLineIndex
            currentLineIndex = index
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (currentLineIndex != -1) notifyItemChanged(currentLineIndex)

            // Refresh juga tetangga baris yang aktif untuk efek fokus
            if(oldIndex-1 >= 0) notifyItemChanged(oldIndex-1)
            if(oldIndex+1 < lyrics.size) notifyItemChanged(oldIndex+1)
            if(currentLineIndex-1 >= 0) notifyItemChanged(currentLineIndex-1)
            if(currentLineIndex+1 < lyrics.size) notifyItemChanged(currentLineIndex+1)
        }
    }

    fun getCurrentLineIndex(): Int {
        return currentLineIndex
    }
}
