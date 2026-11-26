package io.heckel.ntfy.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/*
 * Day-relative times, as the Kompakt's Messages app shows them. Nothing here counts minutes
 * ("5 min ago"): a label only changes when the date does, so an E Ink screen never needs a
 * timer to keep it right; callers recompute on resume and when the date changes.
 */

/** Calendar day of [epochMillis] in [zone]. */
fun localDay(epochMillis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

/** Time of day in the phone's locale and 12/24-hour setting, e.g. "4:19 PM" or "16:19". */
fun timeOfDayLabel(epochMillis: Long, zone: ZoneId, locale: Locale, use24h: Boolean): String =
    DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** "23 Nov", or "23 Nov 2025" when [date] isn't in [today]'s year. */
private fun dateLabel(date: LocalDate, today: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMM" else "d MMM yyyy", locale).format(date)

/**
 * A list row's time: the time today, "Yesterday", the weekday within the last 6 days ("Mon"),
 * then "23 Nov", with the year when it isn't this year. Future dates show their date.
 */
fun listTimeLabel(epochMillis: Long, today: LocalDate, zone: ZoneId, locale: Locale, use24h: Boolean): String {
    val day = localDay(epochMillis, zone)
    return when (ChronoUnit.DAYS.between(day, today)) {
        0L -> timeOfDayLabel(epochMillis, zone, locale, use24h)
        1L -> "Yesterday"
        in 2L..6L -> DateTimeFormatter.ofPattern("EEE", locale).format(day)
        else -> dateLabel(day, today, locale)
    }
}

/** A chat's day marker: "Today", "Yesterday", else "23 Nov" (with the year when not this year). */
fun dayMarkerLabel(day: LocalDate, today: LocalDate, locale: Locale): String = when (ChronoUnit.DAYS.between(day, today)) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> dateLabel(day, today, locale)
}
