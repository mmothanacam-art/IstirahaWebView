package com.istiraha.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
@Volatile
private var isPaused = false
  @Volatile
private var isCancelled = false
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001
   const val ACTION_PAUSE = "com.istiraha.app.ACTION_PAUSE_DOWNLOAD"
  const val ACTION_RESUME = "com.istiraha.app.ACTION_RESUME_DOWNLOAD"
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
    ): Int if (intent?.action == ACTION_PAUSE) {
    isPaused = true

    val resumeIntent = Intent(this, DownloadService::class.java).apply {
        action = ACTION_RESUME
    }

    val resumePendingIntent = PendingIntent.getService(
        this,
        2,
        resumeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val pausedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("التنزيل متوقف مؤقتًا")
        .setContentText("اضغط استئناف لمتابعة التنزيل")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
            android.R.drawable.ic_media_play,
            "استئناف",
            resumePendingIntent
        )
        .build()

    getSystemService(NotificationManager::class.java)
        .notify(NOTIFICATION_ID, pausedNotification)

    return START_NOT_STICKY
    }
if (intent?.action == ACTION_RESUME) {
    isPaused = false

    val pauseIntent = Intent(this, DownloadService::class.java).apply {
        action = ACTION_PAUSE
    }

    val pausePendingIntent = PendingIntent.getService(
        this,
        1,
        pauseIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val resumedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("جاري التنزيل")
        .setContentText("تم استئناف التنزيل")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
            android.R.drawable.ic_media_pause,
            "إيقاف مؤقت",
            pausePendingIntent
        )
        .build()

    getSystemService(NotificationManager::class.java)
        .notify(NOTIFICATION_ID, resumedNotification)

    return START_NOT_STICKY
}
        val fileName =
            intent?.getStringExtra("fileName") ?: "download.mp4"
val downloadUrl = intent?.getStringExtra("url")
val userAgent = intent?.getStringExtra("userAgent")
val cookies = intent?.getStringExtra("cookies")
val referer = intent?.getStringExtra("referer")
       val pauseIntent = Intent(this, DownloadService::class.java).apply {
    action = ACTION_PAUSE
}

val pausePendingIntent = PendingIntent.getService(
    this,
    1,
    pauseIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
val resumeIntent = Intent(this, DownloadService::class.java).apply {
    action = ACTION_RESUME
}

val resumePendingIntent = PendingIntent.getService(
    this,
    2,
    resumeIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
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
           .addAction(
    android.R.drawable.ic_media_pause,
    "إيقاف مؤقت",
    pausePendingIntent
)
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
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "تم ضبط إعدادات GET والوقت",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
        if (!userAgent.isNullOrEmpty()) {
            connection.setRequestProperty("User-Agent", userAgent)
        }

        if (!cookies.isNullOrEmpty()) {
            connection.setRequestProperty("Cookie", cookies)
        }
android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "تم تجاوز User-Agent",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
        if (!referer.isNullOrEmpty()) {
            connection.setRequestProperty("Referer", referer)
        }

        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Accept-Encoding", "identity")
android.os.Handler(android.os.Looper.getMainLooper()).post {
    android.widget.Toast.makeText(
        applicationContext,
        "تم تجهيز جميع إعدادات الاتصال",
        android.widget.Toast.LENGTH_LONG
    ).show()
}
        try {
    connection.connect()
android.util.Log.e("IstirahaDownload", "CONNECT SUCCESS")
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
var downloadedBytes = 0L
while (input.read(buffer).also { count = it } != -1) {
    while (isPaused) {
    Thread.sleep(200)
    }
    output.write(buffer, 0, count)
downloadedBytes += count 
val progressIntent = Intent("com.istiraha.app.DOWNLOAD_PROGRESS").apply {
    setPackage(packageName)
    putExtra("downloaded", downloadedBytes)
    putExtra("total", contentLength)
}
sendBroadcast(progressIntent)
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
