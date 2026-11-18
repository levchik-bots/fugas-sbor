package com.mbteam.fugas

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingMediaRequest: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
        }
        webView.addJavascriptInterface(NativeBridge(this, webView), "FugasNative")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(WebShim.JS, null)
                // Токен отдаём странице, если она уже подписалась на него (после входа)
                if (hasNotifyPermission()) NativeBridge(this@MainActivity, webView).requestToken()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (uri.scheme == "blob" || uri.scheme == "data") return false
                val serverHost = Uri.parse(BuildConfig.SERVER_URL).host
                if (uri.host == serverHost) return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: ActivityNotFoundException) {
                    // нет приложения для этой ссылки — ничего не делаем
                }
                return true
            }
        }

        // Звонки: разрешаем микрофон и камеру для страницы, но только после системного запроса Android
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val needed = mutableListOf<String>()
                request.resources.forEach { res ->
                    when (res) {
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> needed += Manifest.permission.RECORD_AUDIO
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> needed += Manifest.permission.CAMERA
                    }
                }
                val missing = needed.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    request.grant(request.resources)
                } else {
                    pendingMediaRequest = request
                    ActivityCompat.requestPermissions(this@MainActivity, missing.toTypedArray(), REQ_MEDIA)
                }
            }
        }

        webView.loadUrl(BuildConfig.SERVER_URL)
    }

    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            notifyPermissionResult()
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
    }

    private fun notifyPermissionResult() {
        webView.post {
            webView.evaluateJavascript("window.__fugasPermResolve && window.__fugasPermResolve()", null)
        }
    }

    private fun hasNotifyPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when (requestCode) {
            REQ_NOTIFY -> notifyPermissionResult()
            REQ_MEDIA -> {
                pendingMediaRequest?.let { req -> if (allGranted) req.grant(req.resources) else req.deny() }
                pendingMediaRequest = null
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val REQ_NOTIFY = 1
        private const val REQ_MEDIA = 2
    }
}
