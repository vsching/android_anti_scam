/**
 * Utility for providing a stable device identifier.
 * Reuses the same SharedPreferences file ("safeanot_device") as ShareEventRepositoryImpl.
 */
package com.safeanot.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "safeanot_device"
        private const val KEY_DEVICE_ID = "device_id"
    }

    open fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }
}
