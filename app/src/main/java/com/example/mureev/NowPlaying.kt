package com.example.mureev

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mureev.databinding.FragmentNowPlayingBinding
import androidx.palette.graphics.Palette
import org.jaudiotagger.audio.AudioFileIO
import java.io.File

class NowPlaying : Fragment() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireContext().theme.applyStyle(MainActivity.currentTheme[MainActivity.themeIndex], true)
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view)
        binding.root.visibility = View.INVISIBLE

        binding.playPauseBtnNP.setOnClickListener {
            if (PlayerActivity.isPlaying) pauseMusic() else playMusic()
        }

        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            PlayerActivity.musicService!!.createMediaPlayer()
            updateNowPlayingUI()
            PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
            playMusic()
        }

        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("index", PlayerActivity.songPosition)
            intent.putExtra("class", "NowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (PlayerActivity.musicService != null) {
            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            updateNowPlayingUI()
        }
    }

    private fun updateNowPlayingUI() {
        Glide.with(requireContext())
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(binding.songImgNP)

        binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title
        binding.songArtisNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].artist

        if (PlayerActivity.isPlaying) {
            binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
        } else {
            binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        }

        try {
            // Ambil path file lagu yang sedang diputar
            val path = PlayerActivity.musicListPA[PlayerActivity.songPosition].path
            val file = File(path)

            // Baca metadata audio menggunakan Jaudiotagger
            val audioFile = AudioFileIO.read(file)
            val header = audioFile.audioHeader

            // Format data teknis
            val format = header.format.uppercase()
            val sampleRate = "${header.sampleRateAsNumber / 1000.0}kHz"
            val bitDepth = "${header.bitsPerSample}bit"
            val bitrate = "${header.bitRate}Kbps"

            // Set teks dan tampilkan
            binding.audioInfoNP.text = "$format: $sampleRate  $bitDepth  $bitrate"
            binding.audioInfoNP.visibility = View.VISIBLE
        } catch (e: Exception) {
            // Jika gagal membaca info, sembunyikan TextView agar tidak kosong
            binding.audioInfoNP.visibility = View.GONE
            e.printStackTrace()
        }

        // Ambil gambar dari path dan ambil warna dominan
        val imgArt = getImgArt(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_slash_screen)
        }

        Palette.from(image).generate { palette ->
            palette?.let {
                val color1 = it.getDominantColor(0xFFFFFF)
                var color2 = it.getVibrantColor(0xFFFFFF)

                if (color1 == color2) {
                    color2 = it.getDarkVibrantColor(0xFFFFFF)
                    if (color1 == color2) {
                        color2 = it.getLightVibrantColor(0xFFFFFF)
                    }
                }

                if (color1 == color2) {
                    color2 = 0xCCCCCC // fallback
                }

                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(color1, color2)
                )
                binding.root.background = gradient
            }
        }
    }

    private fun getImgArt(uri: String): ByteArray? {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(uri)
            val art = retriever.embeddedPicture
            retriever.release()
            return art
        } catch (e: Exception) {
            return null
        }
    }

    private fun playMusic() {
        PlayerActivity.isPlaying = true
        PlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
        PlayerActivity.musicService!!.showNotification(R.drawable.pause_icon)
    }

    private fun pauseMusic() {
        PlayerActivity.isPlaying = false
        PlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        PlayerActivity.musicService!!.showNotification(R.drawable.play_icon)
    }
}
