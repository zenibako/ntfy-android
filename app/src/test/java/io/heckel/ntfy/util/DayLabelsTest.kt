package io.heckel.ntfy.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class DayLabelsTest {
    private val zone = ZoneId.of("America/New_York")
    private val en = Locale.US
    private val today = LocalDate.of(2026, 11, 26) // a Thursday
    private val names = DayNames("Today", "Yesterday")

    private fun at(y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun list(ms: Long, use24h: Boolean = false) = listTimeLabel(ms, today, zone, en, use24h, names)

    @Test fun todayShowsTimeIn12h() = assertEquals("4:19 PM", list(at(2026, 11, 26, 16, 19)))

    @Test fun todayShowsTimeIn24h() = assertEquals("16:19", list(at(2026, 11, 26, 16, 19), use24h = true))

    @Test fun justAfterMidnightIsToday() = assertEquals("12:01 AM", list(at(2026, 11, 26, 0, 1)))

    @Test fun justBeforeMidnightIsYesterday() = assertEquals("Yesterday", list(at(2026, 11, 25, 23, 59)))

    @Test fun twoToSixDaysAgoShowTheWeekday() {
        assertEquals("Tue", list(at(2026, 11, 24)))
        assertEquals("Fri", list(at(2026, 11, 20))) // 6 days ago
    }

    @Test fun sevenDaysAgoShowsTheDate() = assertEquals("19 Nov", list(at(2026, 11, 19)))

    @Test fun earlierThisYearHasNoYear() = assertEquals("3 Jan", list(at(2026, 1, 3)))

    @Test fun lastYearHasTheYear() = assertEquals("31 Dec 2025", list(at(2025, 12, 31)))

    @Test fun yesterdayAcrossNewYear() = assertEquals(
        "Yesterday",
        listTimeLabel(at(2025, 12, 31, 22, 0), LocalDate.of(2026, 1, 1), zone, en, false, names),
    )

    @Test fun futureShowsTheDate() = assertEquals("28 Nov", list(at(2026, 11, 28)))

    @Test fun markers() {
        assertEquals("Today", dayMarkerLabel(today, today, en, names))
        assertEquals("Yesterday", dayMarkerLabel(today.minusDays(1), today, en, names))
        assertEquals("23 Nov", dayMarkerLabel(LocalDate.of(2026, 11, 23), today, en, names))
        assertEquals("23 Nov 2025", dayMarkerLabel(LocalDate.of(2025, 11, 23), today, en, names))
    }

    @Test fun localDayUsesTheZone() =
        assertEquals(LocalDate.of(2026, 11, 25), localDay(at(2026, 11, 25, 23, 30), zone))

    @Test fun usesTheGivenWords() {
        val de = Locale.GERMANY
        val german = DayNames("Heute", "Gestern")
        assertEquals("Gestern", listTimeLabel(at(2026, 11, 25), today, zone, de, true, german))
        assertEquals("Heute", dayMarkerLabel(today, today, de, german))
        assertEquals("Gestern", dayMarkerLabel(today.minusDays(1), today, de, german))
        assertEquals("Di.", listTimeLabel(at(2026, 11, 24), today, zone, de, true, german))
    }
}
