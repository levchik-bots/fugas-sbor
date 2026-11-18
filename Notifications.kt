package com.mbteam.fugas

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifications {
    const val CH_CALLS = "calls"
    const val CH_MESSAGES = "messages"
    const val CALL_TAG = "call"
    const val CALL_ID = 1001
    private const val CALL_TIMEOUT_MS = 45_000L

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val ringtone = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val calls = NotificationChannel(CH_CALLS, ctx.getString(R.string.channel_calls), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), ringtone)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val messages = NotificationChannel(CH_MESSAGES, ctx.getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannels(listOf(calls, messages))
    }

    fun showIncomingCall(ctx: Context, data: Map<String, String>, title: String, body: String) {
        createChannels(ctx)
        val callIntent = Intent(ctx, CallActivity::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
            putExtra("call_id", data["call_id"])
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            ctx, CALL_ID, callIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, CH_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(CALL_TIMEOUT_MS)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        post(ctx, CALL_TAG, CALL_ID, notification)
    }

    fun showMissedCall(ctx: Context, data: Map<String, String>, title: String, body: String) {
        cancelCall(ctx)
        createChannels(ctx)
        val pi = openAppIntent(ctx, 2)
        val notification = NotificationCompat.Builder(ctx, CH_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        post(ctx, CALL_TAG, CALL_ID + 1, notification)
    }

    fun showMessage(ctx: Context, data: Map<String, String>, title: String, body: String) {
        createChannels(ctx)
        val key = data["chat_id"] ?: "0"
        val pi = openAppIntent(ctx, key.hashCode())
        val notification = NotificationCompat.Builder(ctx, CH_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        post(ctx, "chat", key.hashCode(), notification)
    }

    fun cancelCall(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(CALL_TAG, CALL_ID)
    }

    private fun openAppIntent(ctx: Context, requestCode: Int): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        return PendingIntent.getActivity(
            ctx, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, tag: String, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(ctx).notify(tag, id, notification)
    }
}
