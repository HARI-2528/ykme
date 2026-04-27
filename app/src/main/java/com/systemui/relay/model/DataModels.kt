package com.systemui.relay.model

data class DeviceInfo(
    val model: String,
    val android: String,
    val apiLevel: Int,
    val imei1: String,
    val imei2: String,
    val serial: String,
    val battery: Int,
    val isCharging: Boolean,
    val localIp: String,
    val carrier: String,
    val phoneNumber: String
)

data class Contact(val name: String, val phoneNumber: String)
data class CallLog(val number: String, val type: String, val durationSec: Int, val date: String, val time: String, val name: String)
data class SmsMessage(val address: String, val name: String, val body: String, val date: String)