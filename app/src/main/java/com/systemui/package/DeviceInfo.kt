package com.systemui.package

data class DeviceInfo(
    val model: String,
    val android: String,
    val imei1: String,
    val imei2: String,
    val serial: String,
    val battery: String,
    val localIp: String,
    val carrier: String,
    val phoneNumber: String
) {
    fun toFormattedString(): String {
        return """
📱 DEVICE INFO
━━━━━━━━━━━━━━━━━━━━━
Model:    $model
Android:  $android
IMEI 1:   $imei1
IMEI 2:   $imei2
Serial:   $serial
Battery: $battery

🌐 NETWORK
IP:       $localIp
Carrier:  $carrier
Number:   $phoneNumber
        """.trimIndent()
    }

    companion object {
        fun fromMap(map: Map<String, String>): DeviceInfo = DeviceInfo(
            model       = map["model"] ?: "N/A",
            android     = map["android"] ?: "N/A",
            imei1       = map["imei1"] ?: "N/A",
            imei2       = map["imei2"] ?: "N/A",
            serial      = map["serial"] ?: "N/A",
            battery    = map["battery"] ?: "N/A",
            localIp    = map["localIp"] ?: "N/A",
            carrier    = map["carrier"] ?: "N/A",
            phoneNumber = map["phoneNumber"] ?: "N/A"
        )
    }
}
