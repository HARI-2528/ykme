package com.systemui.package.data

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import com.systemui.package.model.CallLog
import com.systemui.package.utils.DateUtils

object CallLogRepository {
    fun getCallLogs(context: Context, date: String): List<CallLog> {
        val logs = mutableListOf<CallLog>()
        val cr: ContentResolver = context.contentResolver

        val targetDate = when (date.lowercase()) {
            "today" -> DateUtils.todayIST()
            "yesterday" -> DateUtils.yesterdayIST()
            else -> return emptyList()
        }

        val cursor = cr.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE, CallLog.Calls.CACHED_NAME),
            "${CallLog.Calls.DATE} >= ?",
            arrayOf(targetDate.atStartOfDay(DateUtils.IST).toInstant().toEpochMilli().toString()),
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

            while (it.moveToNext()) {
                val callDate = DateUtils.epochToISTDateTime(it.getLong(dateIdx))
                if (callDate.toLocalDate() == targetDate) {
                    val typeStr = when (it.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        else -> "UNKNOWN"
                    }
                    logs.add(CallLog(
                        it.getString(numIdx) ?: "", typeStr, it.getInt(durIdx),
                        DateUtils.formatForDisplay(callDate), DateUtils.formatTimeOnly(callDate),
                        (it.getString(nameIdx) ?: it.getString(numIdx) ?: "")
                    ))
                }
            }
        }
        return logs
    }
}