package com.systemui.package.model

data class Contact(val name: String, val phoneNumber: String)

data class CallLog(
    val number: String, val type: String, val durationSec: Int,
    val date: String, val time: String, val name: String
)

data class SmsMessage(
    val address: String, val name: String, val body: String, val date: String
)