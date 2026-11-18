package com.mbteam.fugas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

/** Методы, которые веб-страница вызывает через window.FugasNative. */
class NativeBridge(private val activity: MainActivity, private val webView: WebView) {

    @JavascriptInterface
    fun appVersion(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface
    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread { activity.askNotificationPermission() }
    }

    @JavascriptInterface
    fun requestToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            pushTokenToPage(token)
        }
    }

    @JavascriptInterface
    fun installUpdate() {
        activity.runOnUiThread { UpdateInstaller.download(activity) }
    }

    fun pushTokenToPage(token: String) {
        // Токен — набор символов из URL-safe алфавита, экранирование не нужно
        webView.post {
            webView.evaluateJavascript("window.__fugasOnToken && window.__fugasOnToken('$token')", null)
        }
    }
}
