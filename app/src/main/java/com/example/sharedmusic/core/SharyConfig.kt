package com.example.sharedmusic.core

/**
 * Постоянные значения продукта SHARY.
 * Все изменяемые настройки живут в UserSettings, здесь только правила и лимиты.
 */
object SharyConfig {

    /**
     * URL Realtime Database проекта shary-f5c42.
     * Адрес указан явно, потому что в google-services.json нет поля firebase_url.
     * Если база создана в другом регионе, значение нужно заменить здесь.
     */
    const val DATABASE_URL: String =
        "https://shary-f5c42-default-rtdb.europe-west1.firebasedatabase.app"

    /** Комната рассчитана на 2–5 человек */
    const val MIN_ROOM_CAPACITY = 2
    const val MAX_ROOM_CAPACITY = 5
    const val DEFAULT_ROOM_CAPACITY = 3

    /** Максимальная длина сообщения чата */
    const val MAX_CHAT_LENGTH = 500

    /** Предельная длина названия трека в базе */
    const val MAX_TITLE_LENGTH = 200

    /** Период опроса позиции плеера в UI */
    const val PLAYBACK_TICK_MS = 200L

    /** Период отправки контрольной позиции хоста в комнату */
    const val HEARTBEAT_PERIOD_MS = 2_000L

    /** Период проверки рассинхронизации гостем */
    const val SYNC_CHECK_PERIOD_MS = 1_500L

    /** Размер обложки, сохраняемой рядом с треком, в пикселях */
    const val ARTWORK_SIZE_PX = 512
}
