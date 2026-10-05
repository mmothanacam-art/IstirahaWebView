package com.istiraha.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class DownloadActivity : Activity() {

    private lateinit var titleText: TextView
    private lateinit var sizeText: TextView
    private lateinit var downloadedText: TextView
    private lateinit var remainingText: TextView
    private lateinit var speedText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressText: TextView

    private lateinit var progressBar: ProgressBar
    private lateinit var pauseButton: Button
    private lateinit var cancelButton: Button

    private var url = ""
    private var fileName = ""
    private var mimeType = ""
    private var userAgent = ""
    private var cookies = ""
    private var referer = ""

    @Volatile
    private var paused = false

    private val cancelled = AtomicBoolean(false)

    @Volatile
    private var downloading = false

    private var downloadedBytes = 0L
    private var totalBytes = -1L

    private lateinit var outputFile: File

    companion object {

        fun start(
            context: Context,
            url: String,
            fileName: String,
            mimeType: String,
            userAgent: String,
            cookies: String,
            referer: String
        ) {
            val intent = Intent(context, DownloadActivity::class.java)

            intent.putExtra("url", url)
            intent.putExtra("fileName", fileName)
            intent.putExtra("mimeType", mimeType)
            intent.putExtra("userAgent", userAgent)
            intent.putExtra("cookies", cookies)
            intent.putExtra("referer", referer)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        url = intent.getStringExtra("url") ?: ""
        fileName = intent.getStringExtra("fileName") ?: "download.bin"
        mimeType = intent.getStringExtra("mimeType") ?: ""
        userAgent = intent.getStringExtra("userAgent") ?: ""
        cookies = intent.getStringExtra("cookies") ?: ""
        referer = intent.getStringExtra("referer") ?: ""

        createUI()

        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        outputFile = createUniqueFile(downloadsDir, fileName)

        titleText.text = fileName

        startDownload()
    }

    private fun createUI() {

        val density = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                (20 * density).toInt(),
                (35 * density).toInt(),
                (20 * density).toInt(),
                (20 * density).toInt()
            )
        }

        titleText = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "جاري الاتصال بالخادم..."
            textSize = 17f
            gravity = Gravity.CENTER
        }

        sizeText = TextView(this).apply {
            text = "الحجم: جاري الحساب..."
            textSize = 16f
        }

        downloadedText = TextView(this).apply {
            text = "تم تنزيل: 0 B"
            textSize = 16f
        }

        remainingText = TextView(this).apply {
            text = "المتبقي: جاري الحساب..."
            textSize = 16f
        }

        speedText = TextView(this).apply {
            text = "السرعة: --"
            textSize = 16f
        }

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
        }

        progressText = TextView(this).apply {
            text = "0%"
            textSize = 18f
            gravity = Gravity.CENTER
        }

        pauseButton = Button(this).apply {
            text = "إيقاف مؤقت"

            setOnClickListener {
                togglePause()
            }
        }

        cancelButton = Button(this).apply {
            text = "إلغاء التنزيل"

            setOnClickListener {
                cancelDownload()
            }
        }

        val elements = listOf<View>(
            titleText,
            statusText,
            sizeText,
            downloadedText,
            remainingText,
            speedText,
            progressBar,
            progressText,
            pauseButton,
            cancelButton
        )

        elements.forEach {

            root.addView(
                it,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (12 * density).toInt()
                }
            )
        }

        setContentView(root)
    }

    private fun startDownload() {

        if (downloading) return

        downloading = true
        paused = false
        cancelled.set(false)

        thread {

            var connection: HttpURLConnection? = null

            try {

                val existingLength =
                    if (outputFile.exists()) outputFile.length()
                    else 0L

                downloadedBytes = existingLength

                connection =
                    URL(url).openConnection() as HttpURLConnection

                connection.instanceFollowRedirects = true
                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                connection.setRequestProperty(
                    "User-Agent",
                    userAgent
                )

                if (cookies.isNotBlank()) {
                    connection.setRequestProperty(
                        "Cookie",
                        cookies
                    )
                }

                if (referer.isNotBlank()) {
                    connection.setRequestProperty(
                        "Referer",
                        referer
                    )
                }

                connection.setRequestProperty(
                    "Accept",
                    "*/*"
                )

                connection.setRequestProperty(
                    "Accept-Encoding",
                    "identity"
                )

                if (existingLength > 0L) {
                    connection.setRequestProperty(
                        "Range",
                        "bytes=$existingLength-"
                    )
                }

                connection.connect()

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK &&
                    responseCode != HttpURLConnection.HTTP_PARTIAL
                ) {
                    throw Exception(
                        "الخادم رفض التنزيل. HTTP $responseCode"
                    )
                }

                val contentLength =
                    connection.contentLengthLong

                totalBytes =
                    if (responseCode ==
                        HttpURLConnection.HTTP_PARTIAL
                    ) {
                        if (contentLength > 0)
                            existingLength + contentLength
                        else
                            -1L
                    } else {
                        contentLength
                    }

                // الخادم تجاهل Range وبدأ من الصفر
                if (existingLength > 0L &&
                    responseCode == HttpURLConnection.HTTP_OK
                ) {
                    downloadedBytes = 0L
                }

                runOnUiThread {

                    statusText.text = "جاري التنزيل"

                    if (totalBytes > 0) {
                        sizeText.text =
                            "الحجم: ${formatBytes(totalBytes)}"
                    } else {
                        sizeText.text =
                            "الحجم: غير معروف"
                    }
                }

                val append =
                    responseCode ==
                        HttpURLConnection.HTTP_PARTIAL &&
                    existingLength > 0L

                val input = connection.inputStream

                val output =
                    FileOutputStream(
                        outputFile,
                        append
                    )

                val buffer = ByteArray(64 * 1024)

                var lastTime =
                    System.currentTimeMillis()

                var lastBytes =
                    downloadedBytes

                while (true) {

                    if (cancelled.get()) {
                        break
                    }

                    while (paused &&
                        !cancelled.get()
                    ) {
                        Thread.sleep(200)
                    }

                    if (cancelled.get()) {
                        break
                    }

                    val count =
                        input.read(buffer)

                    if (count == -1) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        count
                    )

                    downloadedBytes += count

                    val now =
                        System.currentTimeMillis()

                    if (now - lastTime >= 500) {

                        val elapsed =
                            now - lastTime

                        val bytesDifference =
                            downloadedBytes - lastBytes

                        val bytesPerSecond =
                            if (elapsed > 0)
                                bytesDifference * 1000L / elapsed
                            else
                                0L

                        updateProgress(
                            bytesPerSecond
                        )

                        lastTime = now
                        lastBytes = downloadedBytes
                    }
                }

                output.flush()
                output.close()
                input.close()

                if (cancelled.get()) {

                    runOnUiThread {
                        statusText.text =
                            "تم إلغاء التنزيل"
                    }

                } else {

                    updateProgress(0)

                    runOnUiThread {

                        statusText.text =
                            "اكتمل التنزيل ✓"

                        pauseButton.isEnabled =
                            false

                        cancelButton.text =
                            "إغلاق"

                        cancelButton.setOnClickListener {
                            finish()
                        }

                        Toast.makeText(
                            this,
                            "تم حفظ الملف في Downloads",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    statusText.text =
                        "فشل التنزيل"

                    Toast.makeText(
                        this,
                        "تعذر التنزيل: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
                downloading = false
            }
        }
    }

    private fun updateProgress(
        bytesPerSecond: Long
    ) {

        val downloaded =
            downloadedBytes

        val total =
            totalBytes

        runOnUiThread {

            downloadedText.text =
                "تم تنزيل: ${formatBytes(downloaded)}"

            speedText.text =
                if (bytesPerSecond > 0) {
                    "السرعة: ${formatBytes(bytesPerSecond)}/ث"
                } else {
                    "السرعة: --"
                }

            if (total > 0) {

                val remaining =
                    (total - downloaded)
                        .coerceAtLeast(0)

                val percent =
                    ((downloaded * 100L) / total)
                        .coerceIn(0L, 100L)
                        .toInt()

                progressBar.isIndeterminate =
                    false

                progressBar.progress =
                    percent

                progressText.text =
                    "$percent%"

                sizeText.text =
                    "الحجم: ${formatBytes(total)}"

                remainingText.text =
                    "المتبقي: ${formatBytes(remaining)}"

            } else {

                progressBar.isIndeterminate =
                    true

                progressText.text =
                    "جاري التنزيل..."

                remainingText.text =
                    "المتبقي: غير معروف"
            }
        }
    }

    private fun togglePause() {

        if (!downloading) return

        paused = !paused

        if (paused) {

            pauseButton.text =
                "استئناف"

            statusText.text =
                "متوقف مؤقتًا"

        } else {

            pauseButton.text =
                "إيقاف مؤقت"

            statusText.text =
                "جاري التنزيل"
        }
    }

    private fun cancelDownload() {

        if (!downloading) {
            finish()
            return
        }

        cancelled.set(true)
        paused = false

        if (outputFile.exists()) {
            outputFile.delete()
        }

        statusText.text =
            "تم إلغاء التنزيل"

        pauseButton.isEnabled =
            false

        cancelButton.text =
            "إغلاق"

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun createUniqueFile(
        directory: File,
        requestedName: String
    ): File {

        var safeName =
            requestedName
                .replace("/", "_")
                .replace("\\", "_")

        if (safeName.isBlank()) {
            safeName = "download.bin"
        }

        var file =
            File(directory, safeName)

        if (!file.exists()) {
            return file
        }

        val dot =
            safeName.lastIndexOf('.')

        val base =
            if (dot > 0)
                safeName.substring(0, dot)
            else
                safeName

        val extension =
            if (dot > 0)
                safeName.substring(dot)
            else
                ""

        var number = 1

        while (file.exists()) {

            file =
                File(
                    directory,
                    "$base ($number)$extension"
                )

            number++
        }

        return file
    }

    private fun formatBytes(
        bytes: Long
    ): String {

        if (bytes < 0) {
            return "غير معروف"
        }

        if (bytes < 1024) {
            return "$bytes B"
        }

        val kb =
            bytes / 1024.0

        if (kb < 1024) {
            return String.format(
                "%.1f KB",
                kb
            )
        }

        val mb =
            kb / 1024.0

        if (mb < 1024) {
            return String.format(
                "%.1f MB",
                mb
            )
        }

        val gb =
            mb / 1024.0

        return String.format(
            "%.2f GB",
            gb
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
