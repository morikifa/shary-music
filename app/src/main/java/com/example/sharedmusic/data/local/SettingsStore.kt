package com.example.sharedmusic.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sharedmusic.data.model.ThemeMode
import com.example.sharedmusic.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "shary_settings")

/**
 * Настройки пользователя в DataStore: тема, экономия трафика, эквалайзер.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val wifiOnly = booleanPreferencesKey("download_wifi_only")
        val equalizerEnabled = booleanPreferencesKey("equalizer_enabled")
        val bandLow = floatPreferencesKey("eq_band_low")
        val bandMid = floatPreferencesKey("eq_band_mid")
        val bandHigh = floatPreferencesKey("eq_band_high")
        val bandVocal = floatPreferencesKey("eq_band_vocal")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { preferences ->
        UserSettings(
            themeMode = ThemeMode.fromName(preferences[Keys.themeMode]),
            downloadOverWifiOnly = preferences[Keys.wifiOnly] ?: true,
            equalizerEnabled = preferences[Keys.equalizerEnabled] ?: false,
            equalizerBands = listOf(
                preferences[Keys.bandLow] ?: 0f,
                preferences[Keys.bandMid] ?: 0f,
                preferences[Keys.bandHigh] ?: 0f,
                preferences[Keys.bandVocal] ?: 0f
            )
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.wifiOnly] = enabled }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.equalizerEnabled] = enabled }
    }

    /** Сохраняет усиление одной полосы эквалайзера в дБ */
    suspend fun setEqualizerBand(index: Int, decibels: Float) {
        context.settingsDataStore.edit { preferences ->
            when (index) {
                0 -> preferences[Keys.bandLow] = decibels
                1 -> preferences[Keys.bandMid] = decibels
                2 -> preferences[Keys.bandHigh] = decibels
                3 -> preferences[Keys.bandVocal] = decibels
            }
        }
    }
}
