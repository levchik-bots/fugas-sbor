package com.mbteam.fugas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Скачивает APK с сервера во внутреннее хранилище приложения (не в «Загрузки»)
 * и открывает системный установщик. Для установки нужно одно подтверждение пользователя.
 */
object UpdateInstaller {

    fun download(ctx: Context) {
        Toast.makeText(ctx, R.string.update_downloading, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val dir = File(ctx.filesDir, "updates").apply { mkdirs() }
                val target = File(dir, "fugas-update.apk")
                val conn = URL(BuildConfig.SERVER_URL + "/api/app-update/download")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 120_000
                conn.instanceFollowRedirects = true
                try {
                    conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
                } finally {
                    conn.disconnect()
                }
                Handler(Looper.getMainLooper()).post { install(ctx, target) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, R.string.update_failed, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun install(ctx: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(settings)
            Toast.makeText(ctx, R.string.update_allow_install, Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}
