package com.cineflow.tube.data.repository

import android.content.Context
import android.net.Uri
import com.cineflow.tube.data.model.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SubscribedChannel(
    val channelId: String,
    val channelName: String,
    val channelUrl: String,
    val avatarUrl: String? = null
)

data class ImportResult(
    val success: Boolean,
    val importedCount: Int,
    val message: String
)

/**
 * YouTube & NewPipe Subscription Import/Export Manager:
 * - Parses Google Takeout subscriptions.csv (Channel Id, Channel URL, Channel title)
 * - Parses NewPipe JSON backup files (app_version, subscriptions array)
 * - Persists subscriptions locally in private app storage (zero tracking, zero account login)
 * - Exports local database backup to JSON
 */
class YouTubeImportManager(private val context: Context) {

    private val storageFile = File(context.filesDir, "cineflow_subscriptions.json")

    private val _subscriptions = MutableStateFlow<List<SubscribedChannel>>(emptyList())
    val subscriptions: StateFlow<List<SubscribedChannel>> = _subscriptions.asStateFlow()

    init {
        loadStoredSubscriptions()
    }

    private fun loadStoredSubscriptions() {
        if (!storageFile.exists()) {
            _subscriptions.value = defaultChannels()
            return
        }
        try {
            val jsonStr = storageFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SubscribedChannel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubscribedChannel(
                        channelId = obj.getString("channelId"),
                        channelName = obj.getString("channelName"),
                        channelUrl = obj.optString("channelUrl", "https://youtube.com/channel/${obj.getString("channelId")}"),
                        avatarUrl = obj.optString("avatarUrl").ifEmpty { null }
                    )
                )
            }
            _subscriptions.value = if (list.isNotEmpty()) list else defaultChannels()
        } catch (_: Exception) {
            _subscriptions.value = defaultChannels()
        }
    }

    private fun saveSubscriptions(list: List<SubscribedChannel>) {
        try {
            val array = JSONArray()
            for (ch in list) {
                val obj = JSONObject()
                obj.put("channelId", ch.channelId)
                obj.put("channelName", ch.channelName)
                obj.put("channelUrl", ch.channelUrl)
                obj.put("avatarUrl", ch.avatarUrl ?: "")
                array.put(obj)
            }
            storageFile.writeText(array.toString(2))
            _subscriptions.value = list
        } catch (_: Exception) { }
    }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult(false, 0, "Could not open selected file")

            val content = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val trimmed = content.trim()

            // 1. Check if it's a NewPipe JSON backup
            if (trimmed.startsWith("{") && trimmed.contains("\"subscriptions\"")) {
                return@withContext importNewPipeJson(trimmed)
            }

            // 2. Check if it's a Google Takeout CSV (Channel Id,Channel URL,Channel title)
            if (trimmed.contains("Channel Id") || trimmed.contains("channel_id") || trimmed.lines().any { it.contains("UC") && it.contains(",") }) {
                return@withContext importGoogleTakeoutCsv(trimmed)
            }

            // 3. Fallback: line-by-line CSV parser
            return@withContext importGenericCsv(trimmed)
        } catch (e: Exception) {
            ImportResult(false, 0, "Import failed: ${e.message}")
        }
    }

    private fun importGoogleTakeoutCsv(csvText: String): ImportResult {
        val lines = csvText.lines().filter { it.isNotBlank() }
        val imported = mutableListOf<SubscribedChannel>()

        var idIndex = 0
        var urlIndex = 1
        var titleIndex = 2

        val header = lines.firstOrNull() ?: ""
        val headerCols = parseCsvLine(header)
        for (i in headerCols.indices) {
            val col = headerCols[i].trim().lowercase()
            if (col.contains("channel id") || col == "id") idIndex = i
            if (col.contains("url")) urlIndex = i
            if (col.contains("title") || col.contains("name")) titleIndex = i
        }

        for (i in 1 until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.size > idIndex && cols[idIndex].isNotBlank()) {
                val channelId = cols[idIndex].trim()
                if (channelId.startsWith("UC")) {
                    val name = if (cols.size > titleIndex) cols[titleIndex].trim() else "Channel $channelId"
                    val url = if (cols.size > urlIndex) cols[urlIndex].trim() else "https://youtube.com/channel/$channelId"
                    imported.add(SubscribedChannel(channelId = channelId, channelName = name, channelUrl = url))
                }
            }
        }

        if (imported.isEmpty()) {
            return ImportResult(false, 0, "No valid channel IDs found in CSV")
        }

        val merged = (imported + _subscriptions.value).distinctBy { it.channelId }
        saveSubscriptions(merged)
        return ImportResult(true, imported.size, "Imported ${imported.size} YouTube subscriptions successfully!")
    }

    private fun importNewPipeJson(jsonText: String): ImportResult {
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("subscriptions") ?: return ImportResult(false, 0, "No subscriptions array found in NewPipe JSON")
        val imported = mutableListOf<SubscribedChannel>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val url = obj.optString("url", "")
            val name = obj.optString("name", "Channel")
            val avatar = obj.optString("avatarUrl", null)
            val channelId = when {
                url.contains("/channel/") -> url.substringAfter("/channel/").substringBefore("?")
                url.contains("/c/") -> url.substringAfter("/c/").substringBefore("?")
                else -> "UC_${obj.hashCode()}"
            }
            imported.add(SubscribedChannel(channelId = channelId, channelName = name, channelUrl = url, avatarUrl = avatar))
        }

        val merged = (imported + _subscriptions.value).distinctBy { it.channelId }
        saveSubscriptions(merged)
        return ImportResult(true, imported.size, "Imported ${imported.size} subscriptions from NewPipe backup!")
    }

    private fun importGenericCsv(text: String): ImportResult {
        val lines = text.lines().filter { it.isNotBlank() }
        val imported = mutableListOf<SubscribedChannel>()
        for (line in lines) {
            val parts = parseCsvLine(line)
            val channelIdPart = parts.find { it.trim().startsWith("UC") && it.trim().length >= 20 }
            if (channelIdPart != null) {
                val namePart = parts.find { it != channelIdPart && !it.startsWith("http") } ?: "Subscribed Channel"
                imported.add(SubscribedChannel(channelId = channelIdPart.trim(), channelName = namePart.trim(), channelUrl = "https://youtube.com/channel/${channelIdPart.trim()}"))
            }
        }
        if (imported.isNotEmpty()) {
            val merged = (imported + _subscriptions.value).distinctBy { it.channelId }
            saveSubscriptions(merged)
            return ImportResult(true, imported.size, "Imported ${imported.size} channels successfully!")
        }
        return ImportResult(false, 0, "Unrecognized format. Please provide subscriptions.csv from Google Takeout or a NewPipe JSON backup.")
    }

    fun exportToJson(): String {
        val root = JSONObject()
        root.put("app", "CineFlow Tube")
        root.put("version", "1.0.0")
        root.put("timestamp", System.currentTimeMillis())
        val array = JSONArray()
        for (sub in _subscriptions.value) {
            val obj = JSONObject()
            obj.put("channelId", sub.channelId)
            obj.put("channelName", sub.channelName)
            obj.put("channelUrl", sub.channelUrl)
            obj.put("avatarUrl", sub.avatarUrl ?: "")
            array.put(obj)
        }
        root.put("subscriptions", array)
        return root.toString(2)
    }

    fun removeSubscription(channelId: String) {
        val updated = _subscriptions.value.filter { it.channelId != channelId }
        saveSubscriptions(updated)
    }

    fun addSubscription(channel: SubscribedChannel) {
        val updated = listOf(channel) + _subscriptions.value.filter { it.channelId != channel.channelId }
        saveSubscriptions(updated)
    }

    private fun parseCsvLine(line: String): List<String> {
        val list = mutableListOf<String>()
        var inQuote = false
        val sb = StringBuilder()
        for (c in line.toCharArray()) {
            if (c == '"') {
                inQuote = !inQuote
            } else if (c == ',' && !inQuote) {
                list.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        list.add(sb.toString())
        return list.map { it.trim().trim('"') }
    }

    private fun defaultChannels(): List<SubscribedChannel> = listOf(
        SubscribedChannel("UCX6OQ3DkcsbYNE6H8uQQuVA", "MrBeast", "https://youtube.com/channel/UCX6OQ3DkcsbYNE6H8uQQuVA", "https://yt3.googleusercontent.com/ytc/AIdro_nOCfE"),
        SubscribedChannel("UC-lHJZR3Gqxm24_Vd_AJ5Yw", "PewDiePie", "https://youtube.com/channel/UC-lHJZR3Gqxm24_Vd_AJ5Yw", "https://yt3.googleusercontent.com/5o-5fONdA"),
        SubscribedChannel("UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt – In a Nutshell", "https://youtube.com/channel/UCsXVk37bltHxD1rDPwtNM8Q", "https://yt3.googleusercontent.com/ytc/AIdro_kL28c"),
        SubscribedChannel("UCBJycsmduvYEL83R_U4JriQ", "MKBHD", "https://youtube.com/channel/UCBJycsmduvYEL83R_U4JriQ", "https://yt3.googleusercontent.com/lkH37D712-cb"),
        SubscribedChannel("UC2C_jShtL725hvbm1arSV9w", "CGP Grey", "https://youtube.com/channel/UC2C_jShtL725hvbm1arSV9w", "https://yt3.googleusercontent.com/ytc/AIdro_m"),
        SubscribedChannel("UCtinbF-Q-fVthA0qrFQTgXQ", "Veritasium", "https://youtube.com/channel/UCtinbF-Q-fVthA0qrFQTgXQ", "https://yt3.googleusercontent.com/ytc/AIdro_p")
    )
}
