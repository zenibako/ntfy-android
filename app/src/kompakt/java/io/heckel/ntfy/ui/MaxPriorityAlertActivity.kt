package io.heckel.ntfy.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.heckel.ntfy.R

/**
 * Full-screen, alarm-style card for max-priority (5) messages on E Ink phones. Static layout,
 * black on white, no animation: one refresh to show it, one to dismiss it. "Open" goes to
 * the topic's message list; the notification itself stays in the shade.
 */
class MaxPriorityAlertActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)
        }
        setContentView(R.layout.activity_max_priority_alert)
        findViewById<TextView>(R.id.alert_title).text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.alert_message).text = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        findViewById<Button>(R.id.alert_open).setOnClickListener {
            val detail = Intent(this, DetailActivity::class.java).apply {
                intent.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(detail)
            finish()
        }
        findViewById<Button>(R.id.alert_dismiss).setOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()
        // Leave without a transition: on E Ink an animation is several ghosting refreshes.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_TITLE = "io.heckel.ntfy.kompakt.ALERT_TITLE"
        const val EXTRA_MESSAGE = "io.heckel.ntfy.kompakt.ALERT_MESSAGE"
    }
}
