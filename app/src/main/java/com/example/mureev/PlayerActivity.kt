package com.example.mureev

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.text.SpannableStringBuilder
import android.widget.Toast
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.mureev.databinding.ActivityPlayerBinding
import com.example.mureev.databinding.DetailsViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class PlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {


    companion object {
        lateinit var musicListPA : ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying:Boolean = false
        var musicService: MusicService? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityPlayerBinding
        var repeat: Boolean = false
        var shuffle: Boolean = false
        var min1: Boolean = false
        var min5: Boolean = false
        var min10: Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var min120: Boolean = false
        var min240: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        private var appVolume: Int = 100
        private var lyricsDialog: BottomSheetDialog? = null
        private var handler: Handler? = null
        private var syncRunnable: Runnable? = null
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.currentTheme[MainActivity.themeIndex])
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(intent.data?.scheme.contentEquals("content")){
            songPosition = 0
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.music_player_icon_slash_screen)
                        .centerCrop()
                )
                .into(binding.songImgPA)
            binding.songNamePA.text = musicListPA[songPosition].title
            binding.songAlbumPA.text = musicListPA[songPosition].title
            binding.songArtisPA .text = musicListPA[songPosition].title
        }
        else handleIntent(intent)

        setupGestureControls()

        // Floating Buttons for Previous, Pause & Next, Shuffle & Repeat-->
        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPA.setIconTint(ContextCompat.getColorStateList(this, R.color.white))
            } else {
                repeat = false
                binding.repeatBtnPA.setIconTint(ContextCompat.getColorStateList(this, R.color.set_white))
            }
        }
        binding.shuffleBtnPA.setOnClickListener {
            val intent = Intent(this@PlayerActivity, PlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "PlayerActivity")
            startActivity(intent)

            if (!shuffle) {
                shuffle = true
                binding.shuffleBtnPA.setIconTint(ContextCompat.getColorStateList(this, R.color.white))
            } else {
                shuffle = false
                binding.shuffleBtnPA.setIconTint(ContextCompat.getColorStateList(this, R.color.set_white))
            }
        }
        setButtonScaleEffect(findViewById(R.id.previousBtnPA))
        setButtonScaleEffect(findViewById(R.id.nextBtnPA))

        binding.backBtnPA.setOnClickListener { finish() }
        binding.playPauseBtnPA.setOnClickListener{ if(isPlaying) pauseMusic() else playMusic() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if(isPlaying) R.drawable.pause_icon else R.drawable.play_icon)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        //   volume, favourite, lyric, speed
        binding.volumeBtnPA.setOnClickListener {
            setButtonScaleEffect(findViewById(R.id.volumeBtnPA))
            showVolumeDialog()
        }

        binding.favouriteBtnPA.setOnClickListener {
            setButtonScaleEffect(findViewById(R.id.favouriteBtnPA))
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if(isFavourite){
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
                Toast.makeText(this, "Lagu dihapus dari favorit", Toast.LENGTH_SHORT).show()
            } else{
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
                Toast.makeText(this, "Lagu ditambah ke favorit", Toast.LENGTH_SHORT).show()
            }
            FavouriteActivity.favouritesChanged = true
        }
        binding.lyricBtnPA.setOnClickListener {
            setButtonScaleEffect(findViewById(R.id.lyricBtnPA))
            val currentSongPath = musicListPA[songPosition].path
            showLyrics(currentSongPath)
        }
        binding.speedBtnPA.setOnClickListener {
            setButtonScaleEffect(findViewById(R.id.speedBtnPA))
            showSpeedOptions()
        }

        binding.moreBtnPA.setOnClickListener {
            setButtonScaleEffect(it)
            showMoreOptions()
        }

        // Fitur home, equalizer, timer, share, queue
        binding.homeBtnPA.setOnClickListener {
            // Membuat Intent untuk berpindah ke MainActivity
            val intent = Intent(this@PlayerActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                Toast.makeText(this, "Equalizer akan terbuka di layar penuh.", Toast.LENGTH_SHORT).show()
                startActivityForResult(eqIntent, 13)
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur Equalizer Tidak Didukung!!", Toast.LENGTH_SHORT).show()
            }
        }
        binding.timerBtnPA.setOnClickListener {
            val timer = min5 || min10 || min15 || min30 || min60 || min120 || min240
            if(!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Hentikan Timer")
                    .setMessage("Apakah Anda ingin menghentikan timer?")
                    .setPositiveButton("Ya"){ _, _ ->
                        min1 = false
                        min5 = false
                        min10 = false
                        min15 = false
                        min30 = false
                        min60 = false
                        min120 = false
                        min240 = false
                        binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.white))
                    }
                    .setNegativeButton("Tidak"){dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                setDialogBtnBackground(this, customDialog)
            }
        }
        binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Berbagi File Musik!"))

        }
        binding.queueBtnPA.setOnClickListener {
            val queueDialog = QueueBottomSheet()
            queueDialog.show(supportFragmentManager, "QueueDialog")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureControls() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe Right - Previous Song
                            prevNextSong(increment = false)
                        } else {
                            // Swipe Left - Next Song
                            prevNextSong(increment = true)
                        }
                        return true
                    }
                }
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isPlaying) pauseMusic() else playMusic()
                return true
            }
        })

        binding.songImgPA.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        songPosition = intent?.getIntExtra("index", 0) ?: 0
        when(intent?.getStringExtra("class")){
            "MainActivity" -> {
                musicListPA = MainActivity.MusicListMA
                if (shuffle) musicListPA.shuffle()
                setLayout()
                if (musicService != null) createMediaPlayer()
            }
            "QueueDialogFragment" -> {
                musicListPA = MainActivity.MusicListMA
                setLayout()
                if (musicService != null) createMediaPlayer()
            }
            "NowPlaying"->{
                setLayout()
                musicService?.let {
                    binding.tvSeekBarStart.text = formatDuration(it.mediaPlayer!!.currentPosition.toLong())
                    binding.tvSeekBarEnd.text = formatDuration(it.mediaPlayer!!.duration.toLong())
                    binding.seekBarPA.progress = it.mediaPlayer!!.currentPosition
                    binding.seekBarPA.max = it.mediaPlayer!!.duration
                }
                if(isPlaying) binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"-> initServiceAndPlaylist(MainActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            "PlaylistDetailsAdapter"-> initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = false)
            "PlaylistDetailsShuffle"-> initServiceAndPlaylist(PlaylistActivity.musicPlaylist.ref[PlaylistDetails.currentPlaylistPos].playlist, shuffle = true)
            "PlayNext"->initServiceAndPlaylist(PlayNext.playNextList, shuffle = false, playNext = true)
            "FavouriteShuffle"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
            "MostPlayedAdapter"-> initServiceAndPlaylist(MostPlayedActivity.mostPlayedSongs, shuffle = false)
            "DetailAdapter" -> initServiceAndPlaylist(DetailActivity.songList, shuffle = false)
            "StatsAdapter" -> initServiceAndPlaylist(MainActivity.MusicListMA, shuffle = false)
        }
        if (musicService!= null && !isPlaying) playMusic()
    }

    // Fungsi showLyrics sekarang sudah benar memanggil API
    private fun showLyrics(audioFilePath: String) {
        try {
            val audioFile: AudioFile = AudioFileIO.read(File(audioFilePath))
            val lyricsText = audioFile.tag.getFirst(FieldKey.LYRICS)

            if (lyricsText.isNotEmpty()) {
                val parsedLyrics = parseLrc(lyricsText)
                if (parsedLyrics.isNotEmpty()) {
                    setupKaraokeDialog(parsedLyrics)
                    return
                }
            }

            // Jika tidak ada lirik di file, cari di internet
            fetchLyricsFromInternet()

        } catch (e: Exception) {
            fetchLyricsFromInternet()
        }
    }

    private fun fetchLyricsFromInternet() {
        val currentSong = musicListPA[songPosition]
        val apiService = LyricsApiService()

        Toast.makeText(this, "Mencari lirik online...", Toast.LENGTH_SHORT).show()

        apiService.fetchLyrics(currentSong.artist, currentSong.title, object : LyricsApiService.LyricsCallback {
            override fun onSuccess(lyrics: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val parsedLyrics = parseLrc(lyrics)
                    if (parsedLyrics.isNotEmpty()) {
                        setupKaraokeDialog(parsedLyrics)
                    } else {
                        // Jika ada lirik tapi format plain (bukan LRC), bungkus sebagai satu baris atau tampilkan di search
                        showLyricsSearchDialog(currentSong.artist, currentSong.title)
                    }
                }
            }

            override fun onFailure(error: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showLyricsSearchDialog(currentSong.artist, currentSong.title)
                }
            }
        })
    }

    // Fungsi setupKaraokeDialog sekarang berisi logika sinkronisasi yang lebih baik
    private fun setupKaraokeDialog(lyricLines: ArrayList<LyricLine>) {
        if (isFinishing || isDestroyed) return
        
        if (lyricsDialog != null && lyricsDialog!!.isShowing) {
            lyricsDialog!!.dismiss()
        }

        handler?.removeCallbacksAndMessages(null)

        val view = layoutInflater.inflate(R.layout.dialog_lyrics, null)
        val binding = com.example.mureev.databinding.DialogLyricsBinding.bind(view)

        lyricsDialog = BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        lyricsDialog?.setContentView(view)

        // Atur tinggi dialog saat ditampilkan
        lyricsDialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val desiredHeight = 400.toPx() // Atur tinggi dialog di sini (misal: 400dp)
                behavior.peekHeight = desiredHeight
            }
        }

        lyricsDialog?.setOnDismissListener {
            handler?.removeCallbacksAndMessages(null)
        }

        binding.lyricsSongTitle.text = musicListPA[songPosition].title
        val lyricsRV = binding.lyricsRV
        val layoutManager = LinearLayoutManager(this)
        val lyricsAdapter = LyricsAdapter(lyricLines) { time ->
            // Logika untuk tap-to-seek
            handler?.removeCallbacks(syncRunnable!!)
            musicService?.mediaPlayer?.seekTo(time.toInt())
            val tappedIndex = lyricLines.indexOfFirst { it.time == time }
            if (tappedIndex != -1) {
                (lyricsRV.adapter as LyricsAdapter).setCurrentLine(tappedIndex)
                layoutManager.scrollToPositionWithOffset(tappedIndex, lyricsRV.height / 2 - 50)
            }
            handler?.postDelayed(syncRunnable!!, 1000)
        }

        lyricsRV.layoutManager = layoutManager
        lyricsRV.adapter = lyricsAdapter

        handler = Handler(Looper.getMainLooper())
        syncRunnable = object : Runnable {
            override fun run() {
                try {
                    val currentPosition = musicService!!.mediaPlayer!!.currentPosition.toLong()
                    val currentIndex = lyricLines.indexOfLast { it.time <= currentPosition }

                    if (currentIndex != -1 && lyricsAdapter.getCurrentLineIndex() != currentIndex) {
                        lyricsAdapter.setCurrentLine(currentIndex)

                        val smoothScroller = object : LinearSmoothScroller(this@PlayerActivity) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_ANY
                            }

                            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                                val recyclerViewCenter = lyricsRV.height / 2
                                val itemCenter = view.top + (view.height / 2)
                                return recyclerViewCenter - itemCenter
                            }
                        }

                        smoothScroller.targetPosition = currentIndex
                        layoutManager.startSmoothScroll(smoothScroller)
                    }
                    handler?.postDelayed(this, 250)
                } catch (e: Exception) {
                    // Abaikan error jika activity sudah ditutup
                }
            }
        }

        lyricsDialog?.show()
        handler?.postDelayed(syncRunnable!!, 0)
    }

    private fun parseLrc(lrcText: String): ArrayList<LyricLine> {
        val lyricLines = ArrayList<LyricLine>()
        // Cek jika teks mengandung tag waktu [00:00.00]
        if (!lrcText.contains("[")) {
            // Jika plain text, buat setiap baris dengan waktu 0
            lrcText.lines().forEach { line ->
                if (line.isNotBlank()) lyricLines.add(LyricLine(0, line.trim()))
            }
            return lyricLines
        }

        val regex = """\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""".toRegex()
        lrcText.lines().forEach { line ->
            regex.find(line)?.let {
                val (min, sec, ms, text) = it.destructured
                val time = min.toLong() * 60000 + sec.toLong() * 1000 + ms.toLong()
                if (text.isNotBlank()) {
                    lyricLines.add(LyricLine(time, text.trim()))
                }
            }
        }
        lyricLines.sortBy { it.time }
        return lyricLines
    }

    private fun showLyricsSearchDialog(songArtists: String, songTitle: String) {
        if (isFinishing || isDestroyed) return
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Lirik Tidak Tersedia")
        builder.setMessage("Lirik otomatis tidak ditemukan. Ingin mencari lirik lagu \"$songTitle\" di Musixmatch?")

        builder.setPositiveButton("Ya") { _, _ ->
            val artist = songArtists.trim().replace(" ", "-")
            val title = songTitle.trim().replace(" ", "-")
            val url = "https://www.musixmatch.com/lyrics/$artist/$title"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }


    // Fungsi untuk menampilkan PopupMenu dengan opsi kecepatan
    private fun showSpeedOptions() {
        val popupMenu = PopupMenu(this, binding.speedBtnPA)
        popupMenu.menu.apply {
            add("0.5x")
            add("0.75x")
            add("Normal")
            add("1.25x")
            add("1.75x")
            add("2x")
        }
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "0.5x" -> setPlaybackSpeed(0.5f)
                "0.75x" -> setPlaybackSpeed(0.75f)
                "Normal" -> setPlaybackSpeed(1.0f)
                "1.25x" -> setPlaybackSpeed(1.25f)
                "1.75x" -> setPlaybackSpeed(1.75f)
                "2x" -> setPlaybackSpeed(2.0f)
            }
            true
        }
        popupMenu.show()
    }

    // Fungsi untuk mengatur kecepatan playback pada MediaPlayer
    private fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            musicService?.mediaPlayer?.let { mediaPlayer ->
                val playbackParams = mediaPlayer.playbackParams
                playbackParams.speed = speed
                mediaPlayer.playbackParams = playbackParams
                Toast.makeText(this, "Kecepatan pemutaran diatur ke ${speed}x", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Penyesuaian kecepatan pemutaran tidak didukung pada perangkat ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVolumeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_volume, null)
        val volumeSeekBar = dialogView.findViewById<SeekBar>(R.id.volumeSeekBar)
        val volumeLabel = dialogView.findViewById<TextView>(R.id.volumeLabel)

        // Set nilai awal SeekBar ke nilai volume aplikasi
        volumeSeekBar.progress = appVolume
        volumeLabel.text = "  Volume: $appVolume%"

        // Tambahkan listener ke SeekBar untuk mengatur volume aplikasi
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                appVolume = progress
                volumeLabel.text = "  Volume: $progress%"
                musicService?.setVolume(progress) //
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Tampilkan dialog untuk mengatur volume
        AlertDialog.Builder(this)
            .setTitle("Atur Volume")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMoreOptions() {
        val popupMenu = PopupMenu(this, binding.moreBtnPA)
        popupMenu.menuInflater.inflate(R.menu.player_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            val currentSong = musicListPA[songPosition]
            when (item.itemId) {
                R.id.metaData -> {
                    showSongDetails(currentSong)
                }
                R.id.goArtist -> {
                    val intent = Intent(this, DetailActivity::class.java)
                    intent.putExtra("filter_type", "artist")
                    intent.putExtra("filter_name", currentSong.artist)
                    startActivity(intent)
                }
                R.id.goAlbum -> {
                    val intent = Intent(this, DetailActivity::class.java)
                    intent.putExtra("filter_type", "album")
                    intent.putExtra("filter_name", currentSong.album)
                    startActivity(intent)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun showSongDetails(currentSong: Music) {
        val detailsDialog = LayoutInflater.from(this).inflate(R.layout.details_view, null)
        val binder = DetailsViewBinding.bind(detailsDialog)
        binder.detailsTV.setTextColor(Color.WHITE)
        binder.root.setBackgroundColor(Color.TRANSPARENT)
        val dDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Meta data")
            .setView(detailsDialog)
            .setPositiveButton("OK") { self, _ -> self.dismiss() }
            .setCancelable(true)
            .create()
        dDialog.show()
        dDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.CYAN)
        dDialog.window?.setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))

        val file = File(currentSong.path)
        val str = SpannableStringBuilder()

        try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            val header = audioFile.audioHeader

            val sampleRate = "${header.sampleRateAsNumber} Hz"
            val bitDepth = "${header.bitsPerSample}-bit"
            val bitrate = "${header.bitRate} Kbps"
            val channels = header.channels

            fun getSafeTag(key: FieldKey): String = try { tag.getFirst(key) } catch (e: Exception) { "" }

            str.bold { append("Title: ") }.append(getSafeTag(FieldKey.TITLE).takeIf { it.isNotEmpty() } ?: currentSong.title).append("\n")
               .bold { append("Artist: ") }.append(getSafeTag(FieldKey.ARTIST).takeIf { it.isNotEmpty() } ?: currentSong.artist).append("\n")
               .bold { append("Album artist: ") }.append(getSafeTag(FieldKey.ALBUM_ARTIST)).append("\n")
               .bold { append("Album: ") }.append(getSafeTag(FieldKey.ALBUM).takeIf { it.isNotEmpty() } ?: currentSong.album).append("\n")
               .bold { append("Track number: ") }.append(getSafeTag(FieldKey.TRACK)).append("\n")
               .bold { append("Disc number: ") }.append(getSafeTag(FieldKey.DISC_NO)).append("\n")
               .bold { append("Genre: ") }.append(getSafeTag(FieldKey.GENRE)).append("\n")
               .bold { append("Composer: ") }.append(getSafeTag(FieldKey.COMPOSER)).append("\n")
               .bold { append("Duration: ") }.append(formatDurationHMS(currentSong.duration)).append("\n")
               .bold { append("Bitrate: ") }.append(bitrate).append("\n")
               .bold { append("Sample rate: ") }.append(sampleRate).append("\n")
               .bold { append("Resolution: ") }.append(bitDepth).append("\n")
               .bold { append("Channels: ") }.append(channels).append("\n")
               .bold { append("Date Added: ") }.append(formatDate(file.lastModified())).append("\n")
               .bold { append("Jumlah diputar: ") }.append("${MainActivity.playCountMap.getOrDefault(currentSong.id, 0)} kali").append("\n")
            str.bold { append("File name: ") }.append(currentSong.path)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        binder.detailsTV.text = str
    }

    private fun formatDurationHMS(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    //Important Function
    private fun initializeLayout(){
        handleIntent(intent)
    }

    private fun setLayout() {
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .centerCrop()
            )
            .into(binding.songImgPA)

        if (lyricsDialog?.isShowing == true) {
            val currentPath = musicListPA[songPosition].path
            showLyrics(currentPath)
        }

        binding.songNamePA.text = musicListPA[songPosition].title
        binding.songNamePA.isSelected = true //aktifkan teks bergerak
        binding.songAlbumPA.text = musicListPA[songPosition].album
        binding.songAlbumPA.isSelected = true
        binding.songArtisPA.text = musicListPA[songPosition].artist
        binding.songArtisPA.isSelected = true

        try {
            val currentSong = musicListPA[songPosition]
            val file = java.io.File(currentSong.path)
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
            val header = audioFile.audioHeader

            val format = header.format.uppercase()
            val sampleRate = "${header.sampleRateAsNumber / 1000.0}kHz"
            val bitDepth = "${header.bitsPerSample}bit"
            val bitrate = "${header.bitRate}Kbps"

            binding.audioInfoTV.text = "$format: $sampleRate  $bitDepth  $bitrate"
            binding.audioInfoTV.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.audioInfoTV.visibility = View.GONE
            e.printStackTrace()
        }

        if (repeat) binding.repeatBtnPA.setIconTint(
            ContextCompat.getColorStateList(
                applicationContext,
                R.color.white
            )
        )
        if (shuffle) binding.shuffleBtnPA.setIconTint(
            ContextCompat.getColorStateList(
                applicationContext,
                R.color.white
            )
        )
        if (min1 || min10 || min15 || min30 || min60 || min120 || min240) binding.timerBtnPA.setColorFilter(
            ContextCompat.getColor(
                applicationContext,
                R.color.cool_blue
            )
        )
        if (isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

        val img = getImgArt(musicListPA[songPosition].path)
        val image = if (img != null) {
            BitmapFactory.decodeByteArray(img, 0, img.size)
        } else {
            BitmapFactory.decodeResource(
                resources,
                R.drawable.music_player_icon_slash_screen
            )
        }

        // Mengambil warna dominan menggunakan Palette
        Palette.from(image).generate { palette ->
            palette?.let {
                val color1 = it.getDominantColor(0xFFFFFF)
                var color2 = it.getVibrantColor(0xFFFFFF)

                if (color1 == color2) {
                    color2 = it.getDarkVibrantColor(0xFFFFFF)
                    if (color1 == color2) {
                        color2 =
                            it.getLightVibrantColor(0xFFFFFF)
                    }
                }

                if (color1 == color2) {
                    color2 = 0xCCCCCC
                }

                // Animasi Transisi Warna Latar Belakang & Status Bar
                val oldStatusBarColor = window.statusBarColor
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), oldStatusBarColor, color1)
                colorAnimation.duration = 1000 // 1 detik transisi
                colorAnimation.addUpdateListener { animator ->
                    val animatedColor = animator.animatedValue as Int
                    window.statusBarColor = animatedColor

                    // Kita juga menganimasikan gradien background secara perlahan
                    val gradient = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(animatedColor, color2)
                    )
                    binding.root.background = gradient
                }
                colorAnimation.start()
            }
        }
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun createMediaPlayer() {
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
            playMusic()

            val songId = musicListPA[songPosition].id
            var count = MainActivity.playCountMap.getOrDefault(songId, 0)
            count++
            MainActivity.playCountMap[songId] = count

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun playMusic(){
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon)
    }

    private fun pauseMusic(){
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon)
    }

    private fun prevNextSong(increment: Boolean) {
        if (shuffle) {
            songPosition = (0 until musicListPA.size).random()
        } else {
            setSongPosition(increment = increment)
        }
        setLayout()
        createMediaPlayer()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if(musicService == null){
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        createMediaPlayer()
        musicService!!.seekBarSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        NowPlaying.binding.songNameNP.isSelected = true

        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .centerCrop()
            )
            .into(NowPlaying.binding.songImgNP)

        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].title
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@PlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_1)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 1 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min1 = true
            Thread{Thread.sleep((1 * 60000).toLong())
                if(min1) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_5)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 5 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min5 = true
            Thread{Thread.sleep((5 * 60000).toLong())
                if(min5) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_10)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 10 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min10 = true
            Thread{Thread.sleep((10 * 60000).toLong())
                if(min10) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 15 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min15 = true
            Thread{Thread.sleep((15 * 60000).toLong())
                if(min15) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 30 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if(min30) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 60 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min60 = true
            Thread{Thread.sleep((60 * 60000).toLong())
                if(min60) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_120)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 120 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min120 = true
            Thread{Thread.sleep((120 * 60000).toLong())
                if(min120) exitApplication()}.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_240)?.setOnClickListener {
            Toast.makeText(baseContext,  "Musik akan berhenti setelah 240 menit", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_blue))
            min240 = true
            Thread{Thread.sleep((240 * 60000).toLong())
                if(min240) exitApplication()}.start()
            dialog.dismiss()
        }
    }

    private fun setButtonScaleEffect(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.4f).scaleY(1.4f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            false
        }
    }

    fun playSongFromQueue(position: Int) {
        if (songPosition != position) {
            songPosition = position
            createMediaPlayer()
            setLayout()
        }
    }

    private fun getMusicDetails(contentUri: Uri): Music {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION)
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            return Music(id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
                artUri = "Unknown", path = path.toString())
        }finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }
    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false){
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if(shuffle) musicListPA.shuffle()
        setLayout()
        if(!playNext) PlayNext.playNextList = ArrayList()
    }
    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()
}