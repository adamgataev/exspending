package com.example.exspending.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Brussels wall-clock helpers. The app stores local `yyyy-MM-dd HH:mm` strings
 * interpreted as Europe/Brussels (no separate timezone column).
 */
object DateTimeHelper {
    val BRUSSELS: ZoneId = ZoneId.of("Europe/Brussels")

    val DATE_TIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val MONTH_HEADER_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy")

    fun now(): LocalDateTime =
        LocalDateTime.now(BRUSSELS).withSecond(0).withNano(0)

    fun nowString(): String = format(now())

    fun format(dateTime: LocalDateTime): String =
        dateTime.withSecond(0).withNano(0).format(DATE_TIME_FORMAT)

    fun parse(value: String): LocalDateTime =
        LocalDateTime.parse(value.trim(), DATE_TIME_FORMAT)

    fun parseDate(value: String): LocalDate =
        LocalDate.parse(value.trim().take(10), DATE_FORMAT)

    fun formatDate(date: LocalDate): String = date.format(DATE_FORMAT)

    fun atTime(date: LocalDate, hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(date, LocalTime.of(hour, minute))

    /** Assign 13:00, then +1 minute for each next row on the same calendar day. */
    fun importTimesForDates(dates: List<LocalDate>): List<LocalDateTime> {
        val counters = mutableMapOf<LocalDate, Int>()
        return dates.map { date ->
            val index = counters.getOrDefault(date, 0)
            counters[date] = index + 1
            atTime(date, 13, 0).plusMinutes(index.toLong())
        }
    }

    fun monthKey(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    fun monthHeader(dateTime: LocalDateTime): String {
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val month = months[dateTime.monthValue - 1]
        return "$month ${dateTime.year}"
    }
}
