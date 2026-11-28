package io.heckel.ntfy.ui

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.util.clickTarget

/**
 * Full-screen, alarm-style card for max-priority (5) messages on E Ink phones. Static layout,
 * black on white, no animation: one refresh to show it, one to dismiss it. "Open" follows the
 * message's Click URL when it has one (a link into another app, such as a Hermes chat), else
 * goes to the topic's message list; the notification itself stays in the shade.
 */
class MaxPriorityAlertActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Show over the lock screen without asking to unlock; unlocking waits for "Open".
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContentView(R.layout.activity_max_priority_alert)
        findViewById<Button>(R.id.alert_open).setOnClickListener { open() }
        findViewById<Button>(R.id.alert_dismiss).setOnClickListener {
            stopInsistentSound()
            finish()
        }
        render()
    }

    // singleInstance: a newer alert arrives here while an older one is still showing
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
        // A replacement alert starts at the top of its own message
        findViewById<ScrollView>(R.id.alert_scroll).scrollTo(0, 0)
    }

    private fun render() {
        findViewById<TextView>(R.id.alert_title).text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.alert_message).text = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
    }

    // Built when it is started, from the current intent: a newer alert may have replaced this
    // one (onNewIntent) while the unlock prompt was showing.
    private fun buildDetailIntent() = Intent(this, DetailActivity::class.java).apply {
        intent.extras?.let { putExtras(it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // The Click URL's target, or the topic's messages when there is none or nothing handles it.
    private fun startTarget() {
        val click = clickTarget(intent.getStringExtra(EXTRA_CLICK).orEmpty())
        if (click != null) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, click.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // No app for that link: fall back to the topic
            }
        }
        startActivity(buildDetailIntent())
    }

    private fun open() {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    startTarget()
                    finish()
                }
            })
        } else {
            startTarget()
            finish()
        }
    }

    // Same as DetailActivity: stop the looping max-priority sound, if it is playing
    private fun stopInsistentSound() {
        try {
            Repository.getInstance(this).mediaPlayer.stop()
        } catch (_: Exception) {
            // Not playing
        }
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
        const val EXTRA_CLICK = "io.heckel.ntfy.kompakt.ALERT_CLICK"
    }
}
