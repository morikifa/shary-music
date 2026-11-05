package com.example.sharedmusic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Правила очереди, которые ломают воспроизведение при старте.
 *
 * Тест не проверяет ExoPlayer (для него нужен Android), а фиксирует два
 * свойства, от которых зависит первый запуск: очередь никогда не отдаёт
 * пустой идентификатор и повтор одного трека не уводит на соседний.
 */
class PlayerThreadingTest {

    private val queue = listOf("a", "b", "c")

    /** Следующий трек при выключенных режимах идёт по кругу */
    @Test
    fun nextWrapsAround() {
        assertEquals("c", PlaybackQueue.next(queue, "b", repeatOne = false, shuffle = false))
        assertEquals("a", PlaybackQueue.next(queue, "c", repeatOne = false, shuffle = false))
    }

    /** Повтор одного трека важнее перемешивания: тот же идентификатор */
    @Test
    fun repeatOneKeepsCurrentTrack() {
        assertEquals("b", PlaybackQueue.next(queue, "b", repeatOne = true, shuffle = true))
    }

    /** Перемешивание никогда не возвращает текущий трек, пока есть другие */
    @Test
    fun shuffleNeverReturnsCurrent() {
        repeat(20) {
            val next = PlaybackQueue.next(
                tracks = queue,
                currentId = "b",
                repeatOne = false,
                shuffle = true,
                randomIndex = { 0 }
            )
            assertTrue(next != "b")
        }
    }

    /** Пустая библиотека не даёт трека, а неизвестный идентификатор — первый */
    @Test
    fun emptyQueueAndUnknownId() {
        assertNull(PlaybackQueue.next(emptyList(), "a", repeatOne = false, shuffle = false))
        assertEquals("a", PlaybackQueue.next(queue, "zzz", repeatOne = false, shuffle = false))
    }

    /** Один трек в библиотеке: и вперёд, и назад он же */
    @Test
    fun singleTrackStaysTheSame() {
        assertEquals("only", PlaybackQueue.next(listOf("only"), "only", false, true))
        assertEquals("only", PlaybackQueue.previous(listOf("only"), "only", true))
    }
}
