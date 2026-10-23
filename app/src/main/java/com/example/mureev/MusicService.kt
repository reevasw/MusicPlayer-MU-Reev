package com.example.mureev

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlin.math.sqrt

class MusicService : Service(), AudioManager.OnAudioFocusChangeListener, SensorEventListener {
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable
    lateinit var audioManager: AudioManager
    private var currentVolume: Float = 1.0f

    // Sensor variables for Shake to Change
    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    // BroadcastReceiver for Headset Unplugged (Noisy Audio)
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (PlayerActivity.isPlaying) pauseMusic()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupSensor()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Register receiver for unplugged headset
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.registerListener(this, sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "My Music")
        return myBinder
    }

    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(playPauseBtn: Int) {
        val intent = Intent(baseContext, MainActivity::class.java)

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentIntent = PendingIntent.getActivity(this, 0, intent, flag)

        val prevIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext, 0, prevIntent, flag)

        val playIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext, 0, playIntent, flag)

        val nextIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext, 0, nextIntent, flag)

        val exitIntent =
            Intent(baseContext, NotificationReceiver::class.java).setAction(ApplicationClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(baseContext, 0, exitIntent, flag)

        val imgArt = getImgArt(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_slash_screen)
        }

        val notification =
            androidx.core.app.NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(PlayerActivity.musicListPA[PlayerActivity.songPosition].title)
                .setContentText(PlayerActivity.musicListPA[PlayerActivity.songPosition].artist)
                .setSmallIcon(R.drawable.music_icon).setLargeIcon(image)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.previous_icon, "Previous", prevPendingIntent)
                .addAction(playPauseBtn, "Play", playPendingIntent)
                .addAction(R.drawable.next_icon, "Next", nextPendingIntent)
                .addAction(R.drawable.exit_icon, "Exit", exitPendingIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            mediaSession.setMetadata(
                MediaMetadataCompat.Builder().putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong()
                ).build()
            )

            mediaSession.setPlaybackState(getPlayBackState())
            mediaSession.setCallback(object : MediaSessionCompat.Callback() {

                //called when play button is pressed
                override fun onPlay() {
                    super.onPlay()
                    handlePlayPause()
                }

                //called when pause button is pressed
                override fun onPause() {
                    super.onPause()
                    handlePlayPause()
                }

                //called when next button is pressed
                override fun onSkipToNext() {
                    super.onSkipToNext()
                    prevNextSong(increment = true, context = baseContext)
                }

                //called when previous button is pressed
                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    prevNextSong(increment = false, context = baseContext)
                }

                //called when headphones buttons are pressed
                //currently only pause or play music on button click
                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    handlePlayPause()
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                //called when seekbar is changed
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer?.seekTo(pos.toInt())

                    mediaSession.setPlaybackState(getPlayBackState())
                }
            })
        }

        startForeground(13, notification)
    }

    fun createMediaPlayer() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(PlayerActivity.musicListPA[PlayerActivity.songPosition].path)
            mediaPlayer?.prepare()

            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
            showNotification(R.drawable.pause_icon)
            PlayerActivity.binding.tvSeekBarStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerActivity.binding.tvSeekBarEnd.text =
                formatDuration(mediaPlayer!!.duration.toLong())
            PlayerActivity.binding.seekBarPA.progress = 0
            PlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration
            PlayerActivity.nowPlayingId = PlayerActivity.musicListPA[PlayerActivity.songPosition].id

            // Tambahkan listener untuk mendeteksi kapan lagu selesai
            mediaPlayer?.setOnCompletionListener {
                updateTotalTime(mediaPlayer!!.duration.toLong())
                PlayerActivity.binding.nextBtnPA.performClick() // Lanjut ke lagu berikutnya
            }

            // Update statistik jumlah putar
            updateStats()

        } catch (e: Exception) {
            return
        }
    }

    private fun updateStats() {
        val song = PlayerActivity.musicListPA[PlayerActivity.songPosition]

        // Update Count
        var count = MainActivity.playCountMap.getOrDefault(song.id, 0)
        count++
        MainActivity.playCountMap[song.id] = count

        // Update Last Played Time
        MainActivity.playTimeMap[song.id] = System.currentTimeMillis()

        // Update History List (Maksimal 50 lagu terakhir)
        updateHistory(song)
    }

    private fun updateHistory(song: Music) {
        val historyPrefs = getSharedPreferences("HISTORY", MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = historyPrefs.getString("HistoryList", null)
        val type = object : com.google.gson.reflect.TypeToken<ArrayList<Music>>() {}.type
        val historyList: ArrayList<Music> = if (json != null) gson.fromJson(json, type) else ArrayList()

        // Hapus jika lagu sudah ada di history (untuk memindahkannya ke paling atas)
        historyList.removeAll { it.id == song.id }

        // Masukkan ke posisi pertama
        historyList.add(0, song)

        // Batasi maksimal 50 lagu
        if (historyList.size > 50) historyList.removeAt(50)

        historyPrefs.edit().putString("HistoryList", gson.toJson(historyList)).apply()
    }

    private fun updateTotalTime(duration: Long) {
        val statsPrefs = getSharedPreferences("STATS", MODE_PRIVATE)
        var totalTime = statsPrefs.getLong("TotalListeningTime", 0L)
        totalTime += duration
        statsPrefs.edit().putLong("TotalListeningTime", totalTime).apply()
    }

    // Volume Control Functions
    fun setVolume(volume: Int) {
        currentVolume = volume / 100f // Ubah ke rentang 0.0 - 1.0
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }

    fun getVolume(): Int {
        return (currentVolume * 100).toInt() // Kembalikan dalam rentang 0-100
    }


    fun seekBarSetup() {
        runnable = Runnable {
            PlayerActivity.binding.tvSeekBarStart.text =
                formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }

    fun getPlayBackState(): PlaybackStateCompat {
        val playbackSpeed = if (PlayerActivity.isPlaying) 1F else 0F

        return PlaybackStateCompat.Builder().setState(
            if (mediaPlayer?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
            mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .build()
    }

    fun handlePlayPause() {
        if (PlayerActivity.isPlaying) pauseMusic()
        else playMusic()

        //update playback state for notification
        mediaSession.setPlaybackState(getPlayBackState())
    }

    private fun prevNextSong(increment: Boolean, context: Context){

        setSongPosition(increment = increment)

        PlayerActivity.musicService?.createMediaPlayer()

        // Menggunakan Glide tanpa cache untuk gambar album
        Glide.with(context)
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .centerCrop()
            )
            .into(PlayerActivity.binding.songImgPA)

        PlayerActivity.binding.songNamePA.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

        Glide.with(context)
            .load(PlayerActivity.musicListPA[PlayerActivity.songPosition].artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.music_player_icon_slash_screen)
                    .centerCrop()
            )
            .into(NowPlaying.binding.songImgNP)

        NowPlaying.binding.songNameNP.text = PlayerActivity.musicListPA[PlayerActivity.songPosition].title

        playMusic()

        PlayerActivity.fIndex = favouriteChecker(PlayerActivity.musicListPA[PlayerActivity.songPosition].id)
        if(PlayerActivity.isFavourite) PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else PlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

        // Update playback state untuk notifikasi
        mediaSession.setPlaybackState(getPlayBackState())
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Restore volume or resume playback
                mediaPlayer?.setVolume(currentVolume, currentVolume)
                if (PlayerActivity.isPlaying) mediaPlayer?.start()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an undefined amount of time: stop playback and release media player
                if (PlayerActivity.isPlaying) pauseMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop playback. We don't release the media player because playback is likely to resume
                if (PlayerActivity.isPlaying) mediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing at an attenuated level
                if (PlayerActivity.isPlaying) mediaPlayer?.setVolume(0.2f, 0.2f)
            }
        }
    }

    fun playMusic(){
        // Request audio focus before starting playback
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //play music
            try {
                PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
                NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
            } catch (e: Exception) {}

            PlayerActivity.isPlaying = true
            mediaPlayer?.start()
            showNotification(R.drawable.pause_icon)
        }
    }

    fun pauseMusic(){
        //pause music
        try {
            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.play_icon)
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.play_icon)
        } catch (e: Exception) {}

        PlayerActivity.isPlaying = false
        mediaPlayer!!.pause()
        showNotification(R.drawable.play_icon)

        // Abandon audio focus when pausing
        audioManager.abandonAudioFocus(this)
    }

    // Sensor Event Listeners
    override fun onSensorChanged(event: SensorEvent?) {
        val x = event!!.values[0]
        val y = event.values[1]
        val z = event.values[2]
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta: Float = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta

        if (acceleration > 12) {
            val shakePrefs = getSharedPreferences("SHAKE", MODE_PRIVATE)
            val isShakeEnabled = shakePrefs.getBoolean("shakeToChange", false)
            if (isShakeEnabled) {
                prevNextSong(increment = true, context = baseContext)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    //for making persistent
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        try {
            unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {}
        audioManager.abandonAudioFocus(this)
    }
}
