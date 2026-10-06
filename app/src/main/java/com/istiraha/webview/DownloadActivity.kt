package com.istiraha.app

import android.os.Bundle
import android.content.ContentValues
import android.provider.MediaStore
import android.content.BroadcastReceiver
import android.content.IntentFilter
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
private lateinit var backButton: Button
    private lateinit var progressReceiver: BroadcastReceiver
    @Volatile
private var isCancelled = false
 @Volatile
private var isPaused = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
progressReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
  val downloaded = intent?.getLongExtra("downloaded", 0L) ?: 0L  }
val total = intent?.getLongExtra("total", -1L) ?: -1L
downloadedText.text = "تم تنزيل: ${formatBytes(downloaded)}"
}if (total > 0) {
    totalSizeText.text = "الحجم الكلي: ${formatBytes(total)}"
}
        val percent = ((downloaded * 100) / total).toInt()
progressBar.progress = percent
percentText.text = "التقدم: $percent%"
        val remaining = total - downloaded
remainingText.text = "المتبقي: ${formatBytes(remaining)}"
      val filter = IntentFilter("com.istiraha.app.DOWNLOAD_PROGRESS")

androidx.core.content.ContextCompat.registerReceiver(
    this,
    progressReceiver,
    filter,
    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
)
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
            isEnabled = true
        }

        cancelButton = Button(this).apply {
            text = "إلغاء التنزيل"
            isEnabled = true
        }
backButton = Button(this).apply {
    text = "العودة إلى الاستراحة"
}
        pauseButton.setOnClickListener {
    isPaused = !isPaused

    if (isPaused) {
        pauseButton.text = "استئناف"
        statusText.text = "تم إيقاف التنزيل مؤقتًا"
    } else {
        pauseButton.text = "إيقاف مؤقت"
        statusText.text = "جاري التنزيل..."
    }
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
layout.addView(
    backButton,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
)
        setContentView(layout)
backButton.setOnClickListener {
    finish()
}
        cancelButton.setOnClickListener {
    isCancelled = true
    cancelButton.isEnabled = false
    statusText.text = "جاري إلغاء التنزيل..."
}
        val downloadUrl = intent.getStringExtra("url")
val userAgent = intent.getStringExtra("userAgent")
val cookies = intent.getStringExtra("cookies")
val referer = intent.getStringExtra("referer")
val fileName = intent.getStringExtra("fileName")

titleText.text = fileName ?: "تنزيل الملف"
statusText.text = "الرابط: ${downloadUrl ?: "NULL"}"
if (downloadUrl.isNullOrEmpty()) {
    statusText.text = "خطأ: رابط التنزيل غير موجود"
    return
}
val serviceIntent = android.content.Intent(
    this,
    DownloadService::class.java
).apply {
    putExtra("url", downloadUrl)
    putExtra("userAgent", userAgent)
    putExtra("cookies", cookies)
    putExtra("referer", referer)
    putExtra("fileName", fileName)
}
androidx.core.content.ContextCompat.startForegroundService(
    this,
    serviceIntent
)
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
