/**
 * DataStore wrapper for user preferences (region and notification settings).
 * Provides reactive Flow-based access to persisted preferences.
 */
package com.safeanot.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

open class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val REGION = stringPreferencesKey("region")
        val SCAM_ALERTS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("scam_alerts_notifications_enabled")
    }

    /**
     * Observe the stored region string, or null when no preference has been set.
     */
    open val regionFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.REGION]
    }

    /**
     * Observe whether scam alert notifications are enabled. Defaults to true.
     */
    open val scamAlertsNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAM_ALERTS_NOTIFICATIONS_ENABLED] ?: true
    }

    open suspend fun setRegion(region: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REGION] = region
        }
    }

    open suspend fun setScamAlertsNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAM_ALERTS_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
