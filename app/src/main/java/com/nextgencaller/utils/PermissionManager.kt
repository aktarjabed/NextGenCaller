package com.nextgencaller.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val REQUIRED_CALL_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val REQUIRED_NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        val ALL_REQUIRED_PERMISSIONS = REQUIRED_CALL_PERMISSIONS + REQUIRED_NOTIFICATION_PERMISSIONS
    }

    fun hasCallPermissions(): Boolean {
        return REQUIRED_CALL_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasNotificationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAllPermissions(): Boolean {
        return hasCallPermissions() && hasNotificationPermissions()
    }

    fun getMissingPermissions(): List<String> {
        return ALL_REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}