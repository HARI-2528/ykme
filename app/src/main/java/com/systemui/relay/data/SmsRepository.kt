package com.systemui.relay.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.systemui.relay.model.SmsMessage
import com.systemui.relay.utils.DateUtils

object SmsRepository {
    fun getSms(ctx: Context, page: Int, limit: Int = 10): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val cr: ContentResolver = ctx.contentResolver

        val cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC")

        cursor?.use {
            val addrIdx = it.getColumnIndex("address")
            val persIdx = it.getColumnIndex("person")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")

            val all = mutableListOf<SmsMessage>()
            while (it.moveToNext()) {
                val addr = it.getString(addrIdx) ?: ""
                if (addr.isNotBlank()) {
                    all.add(SmsMessage(
                        addr,
                        resolveContactName(ctx, it.getString(persIdx), addr),
                        it.getString(bodyIdx) ?: "",
                        DateUtils.formatForDisplay(DateUtils.epochToISTDateTime(it.getLong(dateIdx)))
                    ))
                }
            }

            val start = (page - 1) * limit
            val end = minOf(start + limit, all.size)
            if (start < all.size) messages.addAll(all.subList(start, end))
        }
        return messages
    }

    private fun resolveContactName(ctx: Context, personId: String?, address: String): String {
        if (personId != null) {
            val cr = ctx.contentResolver
            cr.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, personId),
                arrayOf("display_name"), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex("display_name")
                    if (idx >= 0) {
                        val name = c.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return address
    }
}