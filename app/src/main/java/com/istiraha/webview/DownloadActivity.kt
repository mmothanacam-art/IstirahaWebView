package com.istiraha.app

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
    }
}
