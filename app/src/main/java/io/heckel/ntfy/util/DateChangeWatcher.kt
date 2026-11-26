package io.heckel.ntfy.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Calls [onChange] when the date, time or time zone changes, so day-relative labels
 * ("Yesterday", "Mon") move on at midnight without a ticking timer. Start in onResume and
 * stop in onPause.
 */
class DateChangeWatcher(private val context: Context, private val onChange: () -> Unit) {
    private var receiver: BroadcastReceiver? = null

    fun start() {
        if (receiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = onChange()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiver = r
    }

    fun stop() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}
