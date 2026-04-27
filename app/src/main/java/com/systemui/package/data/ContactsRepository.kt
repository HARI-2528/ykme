package com.systemui.package.data

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.systemui.package.model.Contact

object ContactsRepository {
    fun getAllContacts(ctx: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cr: ContentResolver = ctx.contentResolver

        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalized = mutableSetOf<String>()

            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: ""
                val number = it.getString(numIdx) ?: ""
                val norm = number.replace(Regex("[^0-9+]"), "")

                if (norm.isNotEmpty() && normalized.add(norm)) {
                    contacts.add(Contact(name.ifBlank { number }, number))
                }
            }
        }
        return contacts.sortedBy { it.name.lowercase() }
    }
}