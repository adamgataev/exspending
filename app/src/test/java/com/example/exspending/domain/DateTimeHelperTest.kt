package com.example.exspending.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateTimeHelperTest {
    @Test
    fun importTimes_startAt1300_andAddOneMinutePerSameDay() {
        val day = LocalDate.of(2025, 1, 10)
        val times = DateTimeHelper.importTimesForDates(listOf(day, day, day.plusDays(1)))
        assertEquals("2025-01-10 13:00", DateTimeHelper.format(times[0]))
        assertEquals("2025-01-10 13:01", DateTimeHelper.format(times[1]))
        assertEquals("2025-01-11 13:00", DateTimeHelper.format(times[2]))
    }
}
