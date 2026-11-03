package com.polino.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class EventDayTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun eventsOnDay_sameDayDifferentTimes() {
        val events = listOf(
            EventEntity("1", "قسط", millis(2026, 10, 25, 9, 0)),
            EventEntity("2", "حقوق", millis(2026, 10, 25, 23, 59)),
            EventEntity("3", "دیگر", millis(2026, 10, 26, 0, 1))
        )
        val result = eventsOnDay(events, millis(2026, 10, 25, 0, 0))
        assertEquals(listOf("1", "2"), result.map { it.id })
    }

    @Test
    fun eventsOnDay_midnightBoundary() {
        val events = listOf(
            EventEntity("1", "نیمه‌شب", millis(2026, 10, 25, 0, 0)),
            EventEntity("2", "قبل", millis(2026, 10, 24, 23, 59))
        )
        val result = eventsOnDay(events, millis(2026, 10, 25, 12, 0))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun eventsOnDay_empty() {
        assertEquals(
            emptyList<EventEntity>(),
            eventsOnDay(emptyList(), millis(2026, 10, 25, 0, 0))
        )
    }
}
