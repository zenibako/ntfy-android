package io.heckel.ntfy.util

import android.text.format.DateUtils

/**
 * "Today" and "Yesterday" from Android's own translated strings, capitalized as a label, so the
 * day labels follow the phone's language without strings of our own.
 */
fun systemDayNames(nowMillis: Long = System.currentTimeMillis()): DayNames {
    fun word(t: Long) = DateUtils.getRelativeTimeSpanString(t, nowMillis, DateUtils.DAY_IN_MILLIS).toString()
        .replaceFirstChar { it.titlecase() }
    return DayNames(today = word(nowMillis), yesterday = word(nowMillis - DateUtils.DAY_IN_MILLIS))
}
