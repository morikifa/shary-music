package com.example.sharedmusic.data.model

/** Профиль пользователя: имя, аватар и короткий идентификатор */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val avatarPath: String? = null,
    val createdAt: Long = 0L
) {
    /** Короткий идентификатор для показа в профиле: первые 8 символов uid */
    val shortId: String
        get() = if (uid.length > 8) uid.substring(0, 8).uppercase() else uid.uppercase()

    /** Завершён ли онбординг: имя задано */
    val isConfigured: Boolean
        get() = displayName.isNotBlank()
}
