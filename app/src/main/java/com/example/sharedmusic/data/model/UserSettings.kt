package com.example.sharedmusic.data.model

/** Тема оформления */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: DARK
    }
}

/** Пользовательские настройки приложения */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val downloadOverWifiOnly: Boolean = true,
    val equalizerEnabled: Boolean = false,
    /** Усиление по полосам в дБ: низкие, средние, высокие, голос */
    val equalizerBands: List<Float> = listOf(0f, 0f, 0f, 0f)
)
