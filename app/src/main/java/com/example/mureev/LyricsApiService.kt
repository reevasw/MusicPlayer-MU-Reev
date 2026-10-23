package com.example.mureev

import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class LyricsApiService {
    private val client = OkHttpClient()
    private val TAG = "LyricsApiService"

    interface LyricsCallback {
        fun onSuccess(lyrics: String)
        fun onFailure(error: String)
    }

    fun fetchLyrics(artist: String, title: String, callback: LyricsCallback) {
        // 1. Bersihkan metadata dan tangani "Unknown"
        val cleanArtist = if (artist.lowercase().contains("unknown")) "" else cleanMetadata(artist)
        val cleanTitle = cleanMetadata(title)

        Log.d(TAG, "Fetching lyrics for: Artist='$cleanArtist', Title='$cleanTitle'")

        // 2. Jika artis tidak diketahui, langsung gunakan search
        if (cleanArtist.isBlank()) {
            searchLyricsFallback("", cleanTitle, callback)
            return
        }

        // 3. Gunakan URL Encoding
        val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")

        // Endpoint 'get' (Exact Match)
        val url = "https://lrclib.net/api/get?artist=$encodedArtist&track=$encodedTitle"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MUReev Music Player (https://github.com/mureev)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "GET Failure: ${e.message}")
                callback.onFailure(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(TAG, "GET Response Code: ${response.code}")
                
                if (response.isSuccessful && body != null) {
                    processJsonResponse(body, callback)
                } else {
                    // Jika gagal (404 atau lainnya), coba search
                    searchLyricsFallback(cleanArtist, cleanTitle, callback)
                }
            }
        })
    }

    private fun searchLyricsFallback(artist: String, title: String, callback: LyricsCallback) {
        val queryText = if (artist.isNotBlank()) "$artist $title" else title
        val encodedQuery = URLEncoder.encode(queryText, "UTF-8")
        val url = "https://lrclib.net/api/search?q=$encodedQuery"

        Log.d(TAG, "Searching fallback with query: $queryText")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MUReev Music Player")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "SEARCH Failure: ${e.message}")
                callback.onFailure("Search failed")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(TAG, "SEARCH Response Code: ${response.code}")
                
                if (response.isSuccessful && body != null) {
                    try {
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() > 0) {
                            // Loop untuk mencari yang punya lirik (plain atau synced)
                            var found = false
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                if (hasLyrics(item)) {
                                    processJsonResponse(item.toString(), callback)
                                    found = true
                                    break
                                }
                            }
                            if (!found) callback.onFailure("No lyrics in search results")
                        } else {
                            callback.onFailure("No results found")
                        }
                    } catch (e: Exception) {
                        callback.onFailure("Parsing error")
                    }
                } else {
                    callback.onFailure("Search API error")
                }
            }
        })
    }

    private fun hasLyrics(json: JSONObject): Boolean {
        return (json.has("syncedLyrics") && !json.isNull("syncedLyrics") && json.getString("syncedLyrics").isNotBlank()) ||
               (json.has("plainLyrics") && !json.isNull("plainLyrics") && json.getString("plainLyrics").isNotBlank())
    }

    private fun processJsonResponse(body: String, callback: LyricsCallback) {
        try {
            val json = JSONObject(body)
            val lyrics = when {
                json.has("syncedLyrics") && !json.isNull("syncedLyrics") && json.getString("syncedLyrics").isNotBlank() -> 
                    json.getString("syncedLyrics")
                json.has("plainLyrics") && !json.isNull("plainLyrics") && json.getString("plainLyrics").isNotBlank() -> 
                    json.getString("plainLyrics")
                else -> null
            }

            if (lyrics != null) {
                callback.onSuccess(lyrics)
            } else {
                callback.onFailure("Empty lyrics content")
            }
        } catch (e: Exception) {
            callback.onFailure("Parsing error")
        }
    }

    private fun cleanMetadata(text: String): String {
        return text.replace(Regex("(?i)\\(official.*?\\)|\\[official.*?\\]"), "")
            .replace(Regex("(?i)\\(lyrics.*?\\)|\\[lyrics.*?\\]"), "")
            .replace(Regex("(?i)\\(video.*?\\)|\\[video.*?\\]"), "")
            .replace(Regex("(?i)\\(audio.*?\\)|\\[audio.*?\\]"), "")
            .replace(Regex("(?i)\\(remastered.*?\\)|\\[remastered.*?\\]"), "")
            .replace(Regex("(?i)\\(live.*?\\)|\\[live.*?\\]"), "")
            .replace(Regex("(?i)feat\\.?.*|ft\\.?.*"), "")
            .replace(Regex("(?i) - Topic$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
