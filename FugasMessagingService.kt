package com.mbteam.fugas

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Получает push даже когда приложение закрыто.
 * Сервер шлёт data-only сообщения (см. server_patch), поэтому уведомление строим здесь сами,
 * включая полноэкранный экран звонка.
 */
class FugasMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Fugas"
        val body = data["body"] ?: message.notification?.body ?: ""

        when (data["kind"]) {
            "call" -> Notifications.showIncomingCall(this, data, title, body)
            "call_missed" -> Notifications.showMissedCall(this, data, title, body)
            else -> Notifications.showMessage(this, data, title, body)
        }
    }
}
