package com.example.sharedmusic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoomCodeTest {

    @Test
    fun `код состоит из шести допустимых символов`() {
        val code = RoomCode.generate(Random(42))

        assertEquals(RoomCode.LENGTH, code.length)
        assertTrue(RoomCode.isValid(code))
    }

    @Test
    fun `в алфавите нет легко путаемых символов`() {
        listOf('0', 'O', '1', 'I').forEach { symbol ->
            assertFalse("Символ $symbol не должен встречаться", symbol in RoomCode.ALPHABET)
        }
    }

    @Test
    fun `нормализация приводит ввод к верхнему регистру и обрезает лишнее`() {
        val normalized = RoomCode.normalize("ab-7kx2mq")

        assertEquals("AB7KX2", normalized)
    }

    @Test
    fun `короткий код считается невалидным`() {
        assertFalse(RoomCode.isValid("AB7K"))
    }
}

class ChatRulesTest {

    @Test
    fun `пробельная строка считается пустой`() {
        assertTrue(ChatRules.validate("   \n  ") is ChatValidation.Empty)
    }

    @Test
    fun `управляющие символы удаляются`() {
        val result = ChatRules.validate("Привет\u0000\u0007 мир")

        assertTrue(result is ChatValidation.Valid)
        assertEquals("Привет мир", (result as ChatValidation.Valid).text)
    }

    @Test
    fun `перевод строки сохраняется`() {
        val result = ChatRules.validate("первая строка\nвторая")

        assertEquals("первая строка\nвторая", (result as ChatValidation.Valid).text)
    }

    @Test
    fun `слишком длинное сообщение отклоняется`() {
        val long = "а".repeat(com.example.sharedmusic.core.SharyConfig.MAX_CHAT_LENGTH + 1)

        assertTrue(ChatRules.validate(long) is ChatValidation.TooLong)
    }
}

class SyncPolicyTest {

    @Test
    fun `на паузе позиция не растёт`() {
        val expected = SyncPolicy.expectedPositionMs(
            positionMs = 10_000,
            isPlaying = false,
            updatedAtServerMs = 1_000,
            currentServerMs = 60_000
        )

        assertEquals(10_000, expected)
    }

    @Test
    fun `при воспроизведении учитывается прошедшее серверное время`() {
        val expected = SyncPolicy.expectedPositionMs(
            positionMs = 10_000,
            isPlaying = true,
            updatedAtServerMs = 5_000,
            currentServerMs = 8_000
        )

        assertEquals(13_000, expected)
    }

    @Test
    fun `малый дрейф игнорируется`() {
        assertEquals(SyncDecision.IGNORE, SyncPolicy.decide(149))
    }

    @Test
    fun `средний дрейф вызывает мягкую коррекцию`() {
        assertEquals(SyncDecision.SOFT, SyncPolicy.decide(400))
        assertEquals(SyncDecision.SOFT, SyncPolicy.decide(799))
    }

    @Test
    fun `большой дрейф требует перескока`() {
        assertEquals(SyncDecision.HARD, SyncPolicy.decide(801))
    }
}

class VinylGeometryTest {

    @Test
    fun `полный оборот по длительности даёт всю длину трека`() {
        val degrees = VinylGeometry.positionToDegrees(
            positionMs = 60_000,
            durationMs = 60_000
        )

        assertEquals(VinylGeometry.degreesPerTrack(), degrees, 0.5f)
    }

    @Test
    fun `угол переводится обратно в позицию`() {
        val position = VinylGeometry.degreesToPosition(
            degrees = VinylGeometry.degreesPerTrack() / 2f,
            durationMs = 120_000
        )

        assertEquals(60_000, position)
    }

    @Test
    fun `позиция ограничена длительностью`() {
        val position = VinylGeometry.degreesToPosition(
            degrees = VinylGeometry.degreesPerTrack() * 3f,
            durationMs = 10_000
        )

        assertEquals(10_000, position)
    }
}

class PlaybackQueueTest {

    private val queue = listOf("a", "b", "c")

    @Test
    fun `следующий трек идёт по порядку и зацикливается`() {
        assertEquals("b", PlaybackQueue.next(queue, "a", false, false))
        assertEquals("a", PlaybackQueue.next(queue, "c", false, false))
    }

    @Test
    fun `повтор одного трека возвращает тот же трек`() {
        assertEquals("b", PlaybackQueue.next(queue, "b", repeatOne = true, shuffle = false))
    }

    @Test
    fun `перемешивание не выбирает текущий трек`() {
        repeat(20) {
            val next = PlaybackQueue.next(queue, "b", repeatOne = false, shuffle = true)
            assertNotEquals("b", next)
        }
    }

    @Test
    fun `предыдущий трек переходит к началу списка`() {
        assertEquals("c", PlaybackQueue.previous(queue, "a", shuffle = false))
    }
}

class TrackIdentityTest {

    @Test
    fun `одинаковые метаданные дают одинаковый идентификатор`() {
        val first = TrackIdentity.weakId("song.mp3", 1_000, 60_000)
        val second = TrackIdentity.weakId("song.mp3", 1_000, 60_000)

        assertEquals(first, second)
    }

    @Test
    fun `разные метаданные дают разные идентификаторы`() {
        val first = TrackIdentity.weakId("song.mp3", 1_000, 60_000)
        val second = TrackIdentity.weakId("song.mp3", 2_000, 60_000)

        assertNotEquals(first, second)
    }

    @Test
    fun `идентификатор имеет длину SHA-256 в hex`() {
        assertEquals(64, TrackIdentity.weakId("song.mp3", 1, 1).length)
    }
}

class TimeFormatTest {

    @Test
    fun `секунды форматируются с ведущим нулём`() {
        assertEquals("0:05", TimeFormat.clock(5_000))
        assertEquals("1:00", TimeFormat.clock(60_000))
    }

    @Test
    fun `часы добавляются при длительности больше часа`() {
        assertEquals("1:00:00", TimeFormat.clock(3_600_000))
    }

    @Test
    fun `отрицательное время приводится к нулю`() {
        assertEquals("0:00", TimeFormat.clock(-500))
    }

    @Test
    fun `часы прослушивания округляются до одного знака`() {
        assertEquals("1.5", TimeFormat.hours(5_400_000))
    }
}

class ListenDurationTest {

    @Test
    fun `меньше минуты выделяется в отдельный вариант`() {
        assertEquals(ListenDuration.LessThanMinute, ListenDuration.of(59_000))
    }

    @Test
    fun `минуты отбрасывают неполные секунды`() {
        assertEquals(ListenDuration.Minutes(12), ListenDuration.of(12 * 60_000 + 59_000))
    }

    @Test
    fun `час и минуты раскладываются раздельно`() {
        assertEquals(ListenDuration.Hours(1, 5), ListenDuration.of(65 * 60_000))
        assertEquals(ListenDuration.Hours(2, 0), ListenDuration.of(120 * 60_000))
    }

    @Test
    fun `отрицательное значение считается нулевым`() {
        assertEquals(ListenDuration.LessThanMinute, ListenDuration.of(-1_000))
    }
}
