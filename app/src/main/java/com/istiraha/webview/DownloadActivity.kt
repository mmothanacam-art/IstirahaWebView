package com.istiraha.app

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
class DownloadActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var statusText: TextView
    private lateinit var totalSizeText: TextView
    private lateinit var downloadedText: TextView
    private lateinit var remainingText: TextView
    private lateinit var speedText: TextView
    private lateinit var percentText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pauseButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (20 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        titleText = TextView(this).apply {
            text = "تنزيل الملف"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "جاري تجهيز التنزيل..."
            textSize = 16f
        }

        totalSizeText = TextView(this).apply {
            text = "الحجم الكلي: --"
        }

        downloadedText = TextView(this).apply {
            text = "تم تنزيل: 0 MB"
        }

        remainingText = TextView(this).apply {
            text = "المتبقي: --"
        }

        speedText = TextView(this).apply {
            text = "السرعة: --"
        }

        percentText = TextView(this).apply {
            text = "التقدم: 0%"
        }

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
        }

        pauseButton = Button(this).apply {
            text = "إيقاف مؤقت"
            isEnabled = false
        }

        cancelButton = Button(this).apply {
            text = "إلغاء التنزيل"
            isEnabled = false
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(totalSizeText)
        layout.addView(downloadedText)
        layout.addView(remainingText)
        layout.addView(speedText)
        layout.addView(percentText)

        layout.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            pauseButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
val downloadUrl = intent.getStringExtra("url")
val userAgent = intent.getStringExtra("userAgent")
val cookies = intent.getStringExtra("cookies")
val referer = intent.getStringExtra("referer")
val fileName = intent.getStringExtra("fileName")

titleText.text = fileName ?: "تنزيل الملف"

if (downloadUrl.isNullOrEmpty()) {
    statusText.text = "خطأ: رابط التنزيل غير موجود"
    return
}

thread {
    var connection: HttpURLConnection? = null

    try {
        connection = URL(downloadUrl).openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20000
        connection.readTimeout = 20000

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

        connection.connect()

        val responseCode = connection.responseCode
        val totalBytes = connection.contentLengthLong

        runOnUiThread {
            if (responseCode in 200..299) {
                statusText.text = "جاهز للتنزيل"

                if (totalBytes > 0) {
                    totalSizeText.text =
                        "الحجم الكلي: ${formatBytes(totalBytes)}"
                    remainingText.text =
                        "المتبقي: ${formatBytes(totalBytes)}"
                } else {
                    totalSizeText.text = "الحجم الكلي: غير معروف"
                    remainingText.text = "المتبقي: غير معروف"
                }
            } else {
                statusText.text = "تعذر الاتصال بالخادم: $responseCode"
            }
        }

    } catch (e: Exception) {
        runOnUiThread {
            statusText.text =
                "خطأ في قراءة حجم الملف: ${e.message ?: "غير معروف"}"
        }
    } finally {
        connection?.disconnect()
    }
}
    }
    private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.2f KB", kb)

    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.2f MB", mb)

    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
    }
