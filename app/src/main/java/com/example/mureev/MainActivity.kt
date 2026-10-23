package com.example.mureev

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mureev.adapter.MusicAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.example.mureev.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var musicAdapter: MusicAdapter
    private lateinit var voiceSearchLauncher: ActivityResultLauncher<Intent>
    private var searchView: SearchView? = null
    private lateinit var voiceCommandProcessor: VoiceCommandProcessor

    private val handler = Handler(Looper.getMainLooper())
    private val hideBubbleRunnable = Runnable {
        binding.fastScrollBubble.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(200)
            .withEndAction { binding.fastScrollBubble.visibility = View.GONE }
            .start()

        binding.fastScrollerLayout.animate().alpha(0.3f).setDuration(300).start()
        binding.fastScrollerLayout.setBackgroundResource(0)
    }
    private var lastSectionIndex = -1

    companion object{
        lateinit var MusicListMA : ArrayList<Music>
        lateinit var musicListSearch : ArrayList<Music>
        var search: Boolean = false
        var themeIndex: Int = 0
        // 0 = Light, 1 = Dark
        val currentTheme = arrayOf(R.style.LightTheme, R.style.DarkTheme)
        val currentThemeNav = arrayOf(R.style.LightThemeNav, R.style.DarkThemeNav)
        val currentGradient = arrayOf(R.drawable.gradient_light, R.drawable.gradient_dark)
        var sortOrder: Int = 0
        var playCountMap: MutableMap<String, Int> = mutableMapOf()
        var playTimeMap: MutableMap<String, Long> = mutableMapOf()
        val sortingList = arrayOf(MediaStore.Audio.Media.DATE_ADDED + " DESC", MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.SIZE + " DESC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeEditor = getSharedPreferences("THEMES", MODE_PRIVATE)
        themeIndex = themeEditor.getInt("themeIndex", 0)
        
        // Validasi agar tidak crash jika index melebihi jumlah tema yang tersedia
        if (themeIndex >= currentThemeNav.size) {
            themeIndex = 0
            themeEditor.edit().putInt("themeIndex", 0).apply()
        }

        setTheme(currentThemeNav[themeIndex])
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        voiceCommandProcessor = VoiceCommandProcessor(this)

        voiceSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val values = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val voiceQuery = values?.get(0)
                if (!voiceQuery.isNullOrEmpty()) {
                    val wasHandled = voiceCommandProcessor.processCommand(voiceQuery) { query ->
                        searchView?.isIconified = false
                        searchView?.setQuery(query, true)
                    }
                    if (!wasHandled) {
                        searchView?.isIconified = false
                        searchView?.setQuery(voiceQuery, true)
                    }
                }
            }
        }

        toggle = ActionBarDrawerToggle(this, binding.root,R.string.open, R.string.close)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(themeIndex == 4 &&  resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO)
            Toast.makeText(this, "Tema Ireng Berfungsi Paling Baik dalam Mode Gelap!!", Toast.LENGTH_LONG).show()

        if(requestRuntimePermission()){
            initializeLayout()

            FavouriteActivity.favouriteSongs = ArrayList()
            val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
            val jsonString = editor.getString("FavouriteSongs", null)
            val typeToken = object : TypeToken<ArrayList<Music>>(){}.type
            if(jsonString != null){
                val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
                FavouriteActivity.favouriteSongs.addAll(data)
            }
            PlaylistActivity.musicPlaylist = MusicPlaylist()
            val jsonStringPlaylist = editor.getString("MusicPlaylist", null)
            if(jsonStringPlaylist != null){
                val dataPlaylist: MusicPlaylist = GsonBuilder().create().fromJson(jsonStringPlaylist, MusicPlaylist::class.java)
                PlaylistActivity.musicPlaylist = dataPlaylist
            }

            val playCountPrefs = getSharedPreferences("PLAY_COUNTS", MODE_PRIVATE)
            val jsonPlayCount = playCountPrefs.getString("PlayCountsMap", null)
            if(jsonPlayCount != null) {
                val data: MutableMap<String, Int> = GsonBuilder().create().fromJson(jsonPlayCount, object : TypeToken<MutableMap<String, Int>>(){}.type)
                playCountMap = data
            }

            val playTimePrefs = getSharedPreferences("PLAY_TIMES", MODE_PRIVATE)
            val jsonPlayTime = playTimePrefs.getString("PlayTimeMap", null)
            if(jsonPlayTime != null) {
                val data: MutableMap<String, Long> = GsonBuilder().create().fromJson(jsonPlayTime, object : TypeToken<MutableMap<String, Long>>(){}.type)
                playTimeMap = data
            }
        }

        binding.favouriteBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, FavouriteActivity::class.java))
        }
        binding.playlistBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, PlaylistActivity::class.java))
        }
        binding.MostPlayedBtn.setOnClickListener {
            val options = arrayOf("Lagu", "Artis", "Album")
            MaterialAlertDialogBuilder(this)
                .setTitle("Tampilkan Terpopuler Berdasarkan")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> startActivity(Intent(this, MostPlayedActivity::class.java))
                        1 -> {
                            val intent = Intent(this, ArtistActivity::class.java)
                            intent.putExtra("most_played", true)
                            startActivity(intent)
                        }
                        2 -> {
                            val intent = Intent(this, AlbumActivity::class.java)
                            intent.putExtra("most_played", true)
                            startActivity(intent)
                        }
                    }
                }
                .show()
        }

        binding.navView.setNavigationItemSelectedListener{
            when(it.itemId)
            {
                R.id.navUser -> startActivity(Intent(this@MainActivity, UserActivity::class.java))
                R.id.navSettings -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                R.id.navStats -> startActivity(Intent(this, StatsActivity::class.java))
                R.id.navAbout -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                R.id.navLogout -> {
                    val builder = MaterialAlertDialogBuilder(this@MainActivity)
                    builder.setTitle("Logout")
                        .setMessage("Apakah kamu yakin ingin logout dari akun ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                clear()
                                apply()
                            }
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Tidak") { dialog, _ ->
                            dialog.dismiss()
                        }

                    val customDialog = builder.create()
                    customDialog.show()
                    setDialogBtnBackground(this@MainActivity, customDialog)
                }
            }
            true
        }
    }

    private fun requestRuntimePermission() :Boolean{
        val permissions = mutableListOf<String>()
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else{
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if(permissions.isNotEmpty()){
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 13)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 13){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted",Toast.LENGTH_SHORT).show()
                initializeLayout()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item))
            return true

        if (item.itemId == R.id.voiceSearch) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama lagu, artis, atau album...")
            try {
                voiceSearchLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Voice search tidak tersedia", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun initializeLayout(){
        search = false
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        sortOrder = sortEditor.getInt("sortOrder", 0)
        MusicListMA = getAllAudio()
        binding.scrollBar.setHasFixedSize(true)
        binding.scrollBar.setItemViewCacheSize(13)
        binding.scrollBar.layoutManager = LinearLayoutManager(this@MainActivity)
        musicAdapter = MusicAdapter(this@MainActivity, MusicListMA)
        binding.scrollBar.adapter = musicAdapter
        updateMusicSummary()
        setupFastScroller()

        binding.scrollBar.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (sortOrder == 1 || search) {
                    val firstVisiblePos = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (firstVisiblePos != RecyclerView.NO_POSITION) {
                        val sectionIndex = musicAdapter.getSectionForPosition(firstVisiblePos)
                        highlightLetter(sectionIndex)
                        // Bubble removal from general scroll as requested
                    }
                }

                // Disable SwipeRefresh when not at the top to prevent "limited" scrolling feel
                val canScrollUp = recyclerView.canScrollVertically(-1)
                binding.refreshLayout.isEnabled = !canScrollUp
            }
        })

        binding.refreshLayout.setOnRefreshListener {
            MusicListMA = getAllAudio()
            if (::musicAdapter.isInitialized) {
                musicAdapter.updateMusicList(MusicListMA)
                setupFastScroller()
            }
            updateMusicSummary()
            binding.refreshLayout.isRefreshing = false
        }
    }

    private fun showBubble(text: String) {
        if (binding.fastScrollBubble.visibility != View.VISIBLE) {
            binding.fastScrollBubble.visibility = View.VISIBLE
            binding.fastScrollBubble.alpha = 0f
            binding.fastScrollBubble.scaleX = 0.5f
            binding.fastScrollBubble.scaleY = 0.5f
            binding.fastScrollBubble.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start()
        }
        binding.fastScrollBubble.text = text
        handler.removeCallbacks(hideBubbleRunnable)
    }

    private fun highlightLetter(index: Int) {
        if (lastSectionIndex == index) return

        val accentColor = getThemeColor(com.google.android.material.R.attr.colorSecondary)

        // Reset previous
        if (lastSectionIndex != -1 && lastSectionIndex < binding.fastScroller.childCount) {
            val lastTv = binding.fastScroller.getChildAt(lastSectionIndex) as? TextView
            lastTv?.setTextColor(Color.WHITE)
            lastTv?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(100)?.start()
        }

        // Highlight current
        if (index >= 0 && index < binding.fastScroller.childCount) {
            val currentTv = binding.fastScroller.getChildAt(index) as? TextView
            currentTv?.setTextColor(accentColor)
            currentTv?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(100)?.start()
        }
        lastSectionIndex = index
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    @SuppressLint("Recycle", "Range")
    private fun getAllAudio(): ArrayList<Music> {
        val tempList = ArrayList<Music>()
        val filterPrefs = getSharedPreferences("FILTER", MODE_PRIVATE)
        val minDurationSec = filterPrefs.getInt("minDuration", 30)
        val minDurationMs = minDurationSec * 1000L

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.MIME_TYPE + " LIKE 'audio/%'"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
            sortingList[sortOrder], null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)) ?: "Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)) ?: "Unknown"
                    val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)) ?: "Unknown"
                    val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()

                    if (durationC >= minDurationMs) {
                        val music = Music(id = idC, title = titleC, album = albumC, artist = artistC, path = pathC, duration = durationC, artUri = artUriC)
                        if (File(music.path).exists()) tempList.add(music)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return tempList
    }

    private fun updateMusicSummary() {
        val totalLagu = MusicListMA.size
        val totalArtis = MusicListMA.map { it.artist }.distinct().size
        val totalAlbum = MusicListMA.map { it.album }.distinct().size

        val laguStr = "$totalLagu Lagu"
        val artisStr = "$totalArtis Artis"
        val albumStr = "$totalAlbum Album"
        val fullText = "$laguStr • $artisStr • $albumStr"
        val spannableString = SpannableString(fullText)

        val artistClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) { startActivity(Intent(this@MainActivity, ArtistActivity::class.java)) }
        }
        val albumClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) { startActivity(Intent(this@MainActivity, AlbumActivity::class.java)) }
        }

        val artisStart = fullText.indexOf(artisStr)
        val artisEnd = artisStart + artisStr.length
        spannableString.setSpan(artistClickableSpan, artisStart, artisEnd, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        val albumStart = fullText.indexOf(albumStr)
        val albumEnd = albumStart + albumStr.length
        spannableString.setSpan(albumClickableSpan, albumStart, albumEnd, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.totalSongs.text = spannableString
        binding.totalSongs.movementMethod = LinkMovementMethod.getInstance()
        binding.totalSongs.highlightColor = Color.TRANSPARENT
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!PlayerActivity.isPlaying && PlayerActivity.musicService != null) exitApplication()
    }

    override fun onResume() {
        super.onResume()
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        editor.putString("FavouriteSongs", GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs))
        editor.putString("MusicPlaylist", GsonBuilder().create().toJson(PlaylistActivity.musicPlaylist))
        editor.apply()

        val playCountEditor = getSharedPreferences("PLAY_COUNTS", MODE_PRIVATE).edit()
        playCountEditor.putString("PlayCountsMap", GsonBuilder().create().toJson(playCountMap))
        playCountEditor.apply()

        val playTimeEditor = getSharedPreferences("PLAY_TIMES", MODE_PRIVATE).edit()
        playTimeEditor.putString("PlayTimeMap", GsonBuilder().create().toJson(playTimeMap))
        playTimeEditor.apply()

        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        val filterPrefs = getSharedPreferences("FILTER", MODE_PRIVATE)
        val sortValue = sortEditor.getInt("sortOrder", 0)
        val currentMinDuration = filterPrefs.getInt("minDuration", 30)

        if(sortOrder != sortValue || lastMinDuration != currentMinDuration){
            sortOrder = sortValue
            lastMinDuration = currentMinDuration
            MusicListMA = getAllAudio()
            if (::musicAdapter.isInitialized) {
                musicAdapter.updateMusicList(MusicListMA)
                setupFastScroller()
            }
            updateMusicSummary()
        }

        if(PlayerActivity.musicService != null) binding.nowPlaying.visibility = View.VISIBLE
    }

    private var lastMinDuration: Int = -1

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_view_menu, menu)
        findViewById<LinearLayout>(R.id.linearLayoutNav)?.setBackgroundResource(currentGradient[themeIndex])

        searchView = menu?.findItem(R.id.searchView)?.actionView as SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                musicListSearch = ArrayList()
                if (newText != null) {
                    val userInput = newText.lowercase()
                    for (song in MusicListMA) {
                        if (song.title.lowercase().contains(userInput) || song.artist.lowercase().contains(userInput) || song.album.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    }
                    search = true
                    if (::musicAdapter.isInitialized) {
                        musicAdapter.updateMusicList(searchList = musicListSearch)
                        setupFastScroller()
                    }
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFastScroller() {
        binding.fastScroller.removeAllViews()
        if (sortOrder != 1 && !search) {
            binding.fastScrollerLayout.visibility = View.GONE
            return
        }
        binding.fastScrollerLayout.visibility = View.VISIBLE
        binding.fastScrollerLayout.alpha = 0.3f

        val sections = musicAdapter.getSections()
        if (sections != null) {
            for (i in sections.indices) {
                val letter = sections[i].toString()
                val textView = TextView(this)
                textView.text = letter
                textView.setTextColor(Color.WHITE)
                textView.textSize = 11f
                textView.gravity = Gravity.CENTER
                textView.setPadding(16, 4, 16, 4)
                binding.fastScroller.addView(textView)
            }
        }

        binding.fastScrollerLayout.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().alpha(1f).setDuration(150).start()
                    v.setBackgroundResource(R.drawable.rounded_bg_semi_transparent)
                    processFastScroll(event, v)
                }
                MotionEvent.ACTION_MOVE -> {
                    processFastScroll(event, v)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.postDelayed(hideBubbleRunnable, 800)
                }
            }
            true
        }
    }

    private fun processFastScroll(event: MotionEvent, v: View) {
        val y = event.y
        val childCount = binding.fastScroller.childCount
        if (childCount > 0) {
            var index = (y / v.height * childCount).toInt()
            index = index.coerceIn(0, childCount - 1)

            if (index != lastSectionIndex) {
                val pos = musicAdapter.getPositionForSection(index)
                (binding.scrollBar.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)

                val letter = (binding.fastScroller.getChildAt(index) as TextView).text.toString()
                showBubble(letter)
                highlightLetter(index)
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }
}