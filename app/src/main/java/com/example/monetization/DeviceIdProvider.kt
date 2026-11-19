package com.example.monetization

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdProvider {
    @SuppressLint("HardwareIds")
    fun getDeviceCode(context: Context): String {
        val rawAndroidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty().ifBlank { context.packageName }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest("dante_farm_v1:$rawAndroidId".toByteArray(Charsets.UTF_8))

        val hex = digest.take(4).joinToString("") { byte -> "%02X".format(byte) }
        return "DF-$hex"
    }
}
