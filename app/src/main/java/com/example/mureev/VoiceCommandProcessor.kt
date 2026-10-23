package com.example.mureev

import android.content.Context
import android.content.Intent
import android.widget.Toast

class VoiceCommandProcessor(private val context: Context) {

    private val playKeywords = listOf("putar", "mainkan", "setel", "play", "dengarkan", "bunyikan")
    private val searchKeywords = listOf("cari", "temukan", "lihat", "tampilkan", "pencarian", "search")
    private val stopWords = listOf("lagu", "musik", "artis", "album", "dari", "oleh")

    fun processCommand(input: String, onSearch: (String) -> Unit): Boolean {
        val query = input.lowercase().trim()
        var isPlayCommand = false
        var searchTarget = query

        // 1. Cek apakah ini perintah PUTAR
        for (key in playKeywords) {
            if (query.startsWith(key)) {
                isPlayCommand = true
                searchTarget = query.removePrefix(key).trim()
                break
            }
        }

        // 2. Cek apakah ini perintah CARI (jika bukan perintah putar)
        if (!isPlayCommand) {
            for (key in searchKeywords) {
                if (query.startsWith(key)) {
                    searchTarget = query.removePrefix(key).trim()
                    break
                }
            }
        }

        // 3. Bersihkan kata-kata sampah (lagu, musik, dll) dari awal teks
        for (word in stopWords) {
            if (searchTarget.startsWith("$word ")) {
                searchTarget = searchTarget.removePrefix("$word ").trim()
            }
        }

        if (searchTarget.isEmpty()) return false

        // 4. Eksekusi
        if (isPlayCommand) {
            val musicList = MainActivity.MusicListMA
            val index = musicList.indexOfFirst {
                it.title.contains(searchTarget, ignoreCase = true) ||
                        it.artist.contains(searchTarget, ignoreCase = true)
            }

            if (index != -1) {
                Toast.makeText(context, "Memutar: ${musicList[index].title}", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("index", index)
                intent.putExtra("class", "MusicAdapter")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(intent)
                return true
            }
        }

        // Default: Kirim hasil bersih ke kotak pencarian di UI
        onSearch(searchTarget)
        return true
    }
}