package com.example.sharedmusic.domain

/** Правила воспроизведения: очередь, повтор одного трека и режим перемешивания */
object PlaybackQueue {

    /**
     * Следующий трек для воспроизведения.
     * @param repeatOne при повторе одного трека возвращается тот же трек
     * @param shuffle перемешивание: выбирается случайный трек, отличный от текущего
     */
    fun next(
        tracks: List<String>,
        currentId: String?,
        repeatOne: Boolean,
        shuffle: Boolean,
        randomIndex: (Int) -> Int = { kotlin.random.Random.nextInt(it) }
    ): String? {
        if (tracks.isEmpty()) return null
        if (currentId == null) return tracks.first()
        if (repeatOne) return currentId

        if (shuffle) {
            if (tracks.size == 1) return tracks.first()
            val others = tracks.filter { it != currentId }
            return others[randomIndex(others.size)]
        }

        val index = tracks.indexOf(currentId)
        if (index < 0) return tracks.first()

        return tracks[(index + 1) % tracks.size]
    }

    /** Предыдущий трек: при перемешивании выбирается случайный предыдущий */
    fun previous(
        tracks: List<String>,
        currentId: String?,
        shuffle: Boolean,
        randomIndex: (Int) -> Int = { kotlin.random.Random.nextInt(it) }
    ): String? {
        if (tracks.isEmpty()) return null
        if (currentId == null) return tracks.first()

        if (shuffle) {
            if (tracks.size == 1) return tracks.first()
            val others = tracks.filter { it != currentId }
            return others[randomIndex(others.size)]
        }

        val index = tracks.indexOf(currentId)
        if (index < 0) return tracks.first()

        return tracks[(index - 1 + tracks.size) % tracks.size]
    }
}
