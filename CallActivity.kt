package com.mbteam.fugas

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat

/** Полноэкранный экран входящего звонка: открывается поверх блокировки. */
class CallActivity : Activity() {

    private val timeout = Runnable { finishCall(accept = false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val title = intent.getStringExtra("title") ?: getString(R.string.call_incoming_audio)
        val body = intent.getStringExtra("body") ?: ""
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        root.addView(TextView(this).apply {
            text = title
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = body
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, pad * 2, 0, 0)
        }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(pad / 2, 0, pad / 2, 0)
        }
        row.addView(Button(this).apply {
            text = getString(R.string.call_decline)
            setOnClickListener { finishCall(accept = false) }
        }, lp)
        row.addView(Button(this).apply {
            text = getString(R.string.call_accept)
            setOnClickListener { finishCall(accept = true) }
        }, lp)
        root.addView(row)
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed(timeout, 45_000L)
    }

    override fun onDestroy() {
        Handler(Looper.getMainLooper()).removeCallbacks(timeout)
        super.onDestroy()
    }

    private fun finishCall(accept: Boolean) {
        NotificationManagerCompat.from(this).cancel(Notifications.CALL_TAG, Notifications.CALL_ID)
        if (accept) {
            // Открываем приложение: веб-страница сама переподключится по WebSocket,
            // и сервер повторно пришлёт висящий звонок (replay_pending_calls)
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }
        finish()
    }
}
