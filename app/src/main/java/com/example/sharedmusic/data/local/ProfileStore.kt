package com.example.sharedmusic.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sharedmusic.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "shary_profile")

/**
 * Локальный профиль: имя и аватар хранятся на устройстве,
 * uid приходит из анонимной авторизации Firebase.
 */
class ProfileStore(private val context: Context) {

    private object Keys {
        val uid = stringPreferencesKey("uid")
        val displayName = stringPreferencesKey("display_name")
        val avatarPath = stringPreferencesKey("avatar_path")
        val createdAt = stringPreferencesKey("created_at")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { preferences ->
        UserProfile(
            uid = preferences[Keys.uid].orEmpty(),
            displayName = preferences[Keys.displayName].orEmpty(),
            avatarPath = preferences[Keys.avatarPath]?.takeIf { it.isNotBlank() },
            createdAt = preferences[Keys.createdAt]?.toLongOrNull() ?: 0L
        )
    }

    suspend fun saveUid(uid: String) {
        context.profileDataStore.edit { it[Keys.uid] = uid }
    }

    suspend fun saveDisplayName(name: String) {
        context.profileDataStore.edit { preferences ->
            preferences[Keys.displayName] = name.trim()
            if (preferences[Keys.createdAt].isNullOrBlank()) {
                preferences[Keys.createdAt] = System.currentTimeMillis().toString()
            }
        }
    }

    suspend fun saveAvatar(path: String?) {
        context.profileDataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(Keys.avatarPath)
            } else {
                preferences[Keys.avatarPath] = path
            }
        }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }
}
