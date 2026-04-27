package com.systemui.package

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import java.net.NetworkInterface

@SuppressLint("MissingPermission", "HardwareIds")
fun getDeviceInfo(context: Context): Map<String, String> {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // ── IMEI ────────────────────────────────────────────────────────
    val imei1 = try {
        when {
            Build.VERSION.SDK_INT >= 29 -> "Restricted (API 29+)"   // needs privileged permission
            Build.VERSION.SDK_INT >= 26 -> tm.getImei(0) ?: "None"  // API 26-28
            else                        -> tm.deviceId ?: "None"     // API < 26
        }
    } catch (e: SecurityException) { "Permission denied" }

    val imei2 = try {
        when {
            Build.VERSION.SDK_INT >= 29 -> "Restricted (API 29+)"
            Build.VERSION.SDK_INT >= 26 -> tm.getImei(1) ?: "None"
            else                        -> "Not supported"
        }
    } catch (e: SecurityException) { "Permission denied" }

    // ── SERIAL ──────────────────────────────────────────────────────
    val serial = try {
        when {
            Build.VERSION.SDK_INT >= 29 -> "Restricted (API 29+)"   // needs privileged permission
            Build.VERSION.SDK_INT >= 26 -> Build.getSerial()         // API 26-28
            else                        -> @Suppress("DEPRECATION") Build.SERIAL // API < 26
        }
    } catch (e: SecurityException) { "Permission denied" }

    // ── BATTERY ─────────────────────────────────────────────────────
    val (batteryPct, isCharging) = if (Build.VERSION.SDK_INT >= 21) {
        // API 21+ — BatteryManager direct
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        Pair(
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            bm.isCharging
        )
    } else {
        // API < 21 — sticky broadcast
        val intent = context.registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct    = if (scale > 0) (level * 100 / scale) else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        Pair(pct, charging)
    }

    // ── LOCAL IP ────────────────────────────────────────────────────
    val localIp = try {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains('.') == true }
            ?.hostAddress ?: "N/A"
    } catch (e: Exception) { "N/A" }

    // ── CARRIER / PHONE NUMBER ──────────────────────────────────────
    val carrier = tm.networkOperatorName.ifEmpty { "N/A" }

    val phoneNumber = try {
        when {
            Build.VERSION.SDK_INT >= 33 -> {
                // API 33+ requires READ_PHONE_NUMBERS permission
                tm.line1Number ?: "None"
            }
            else -> {
                tm.line1Number?.takeIf { it.isNotEmpty() } ?: "None"
            }
        }
    } catch (e: SecurityException) { "Permission denied" }

    return mapOf(
        "model"       to "${Build.MANUFACTURER} ${Build.MODEL}",
        "android"     to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "imei1"       to imei1,
        "imei2"       to imei2,
        "serial"      to serial,
        "battery"     to "$batteryPct% (${if (isCharging) "Charging" else "Not charging"})",
        "localIp"     to localIp,
        "carrier"     to carrier,
        "phoneNumber" to phoneNumber,
    )
}
