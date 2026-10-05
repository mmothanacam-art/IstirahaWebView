package com.istiraha.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = true
            allowContentAccess = true

            loadWithOverviewMode = true
            useWideViewPort = true

            cacheMode = WebSettings.LOAD_DEFAULT

            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false

            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // دعم تنزيل الصور والفيديوهات والملفات
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->

            try {

                val request = DownloadManager.Request(Uri.parse(url))

                val cookies = CookieManager.getInstance().getCookie(url)

                if (!cookies.isNullOrEmpty()) {
                    request.addRequestHeader("Cookie", cookies)
                }

                if (!userAgent.isNullOrEmpty()) {
                    request.addRequestHeader("User-Agent", userAgent)
                }

                val fileName = URLUtil.guessFileName(
                    url,
                    contentDisposition,
                    mimeType
                )

                request.setTitle(fileName)
                request.setDescription("جاري تنزيل الملف...")

                request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )

                if (!mimeType.isNullOrEmpty()) {
                    request.setMimeType(mimeType)
                }

                val downloadManager =
                    getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                downloadManager.enqueue(request)

                Toast.makeText(
                    this,
                    "بدأ تنزيل: $fileName",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "تعذر تنزيل الملف: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl("http://e7.net:888/")
        } else {
            webView.restoreState(savedInstanceState)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
