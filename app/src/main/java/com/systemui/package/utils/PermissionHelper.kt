package com.systemui.package.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun checkAllPermissions(context: Context): Boolean = getMissingPermissions(context).isEmpty()

    fun getMissingPermissions(context: Context): List<String> {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.READ_CALL_LOG)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.READ_SMS)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.READ_PHONE_STATE)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.READ_PHONE_NUMBERS)
        return perms
    }
}