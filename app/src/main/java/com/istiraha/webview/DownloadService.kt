package com.istiraha.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "التنزيلات",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تنزيل الملفات"
            }

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val fileName =
            intent?.getStringExtra("fileName") ?: "download.mp4"
val downloadUrl = intent?.getStringExtra("url")
val userAgent = intent?.getStringExtra("userAgent")
val cookies = intent?.getStringExtra("cookies")
val referer = intent?.getStringExtra("referer")
        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText("جاري تجهيز التنزيل...")
            .setProgress(100, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification
        )
android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "URL = $downloadUrl",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
        if (downloadUrl.isNullOrEmpty()) {
    stopSelf()
    return START_NOT_STICKY
}
       thread {
    var connection: HttpURLConnection? = null

    try {
       android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "وصلت الخدمة إلى بداية الاتصال",
        android.widget.Toast.LENGTH_LONG
    ).show()
       }
        connection = URL(downloadUrl).openConnection() as HttpURLConnection
android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "تم إنشاء الاتصال",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 30000

        if (!userAgent.isNullOrEmpty()) {
            connection.setRequestProperty("User-Agent", userAgent)
        }

        if (!cookies.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookies)
        }

        if (!referer.isNullOrEmpty()) {
            connection.setRequestProperty("Referer", referer)
        }

        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Accept-Encoding", "identity")
try {
    connection.connect()
} catch (e: Exception) {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        android.widget.Toast.makeText(
            applicationContext,
            "فشل الاتصال: ${e.javaClass.simpleName} - ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    throw e
}
        android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "كود الخادم: ${connection.responseCode}",
        android.widget.Toast.LENGTH_LONG
    ).show()
        }
        val responseCode = connection.responseCode
android.util.Log.e("IstirahaDownload", "HTTP CODE = $responseCode")
if (responseCode !in 200..299) {
    throw Exception("خطأ من الخادم: $responseCode")
}
val contentLength = connection.contentLengthLong

android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "حجم الملف من الخادم: $contentLength بايت",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
val input = connection.inputStream

val values = ContentValues().apply {
    put(
        MediaStore.Downloads.DISPLAY_NAME,
        fileName
    )
    put(
        MediaStore.Downloads.MIME_TYPE,
        "video/mp4"
    )
    put(MediaStore.Downloads.IS_PENDING, 1)
}

val fileUri = contentResolver.insert(
    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
    values
) ?: throw Exception("تعذر إنشاء ملف التنزيل")

val output = contentResolver.openOutputStream(fileUri)
    ?: throw Exception("تعذر فتح ملف التنزيل")

val buffer = ByteArray(8192)
var count: Int

while (input.read(buffer).also { count = it } != -1) {
    output.write(buffer, 0, count)
}

output.flush()
output.close()
input.close()

values.clear()
values.put(MediaStore.Downloads.IS_PENDING, 0)
contentResolver.update(fileUri, values, null, null)

stopForeground(true)
stopSelf()
    } catch (e: Exception) {
        android.util.Log.e("IstirahaDownload", "DOWNLOAD ERROR", e)

android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "خطأ التنزيل: ${e.message}",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

stopSelf()
    } finally {
        connection?.disconnect()
    }
       }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
