package com.istiraha.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var rootContainer: FrameLayout
    private lateinit var wifiButton: Button

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootContainer = FrameLayout(this)

        webView = WebView(this)

        rootContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // زر WiFi
        wifiButton = Button(this).apply {
            text = "📶 WiFi 💪"
            textSize = 14f
            isAllCaps = false
            elevation = 20f

            setOnClickListener {
                val intent = Intent(
                    this@MainActivity,
                    WifiActivity::class.java
                )
                startActivity(intent)
            }
        }

        val density = resources.displayMetrics.density

        // وضع زر WiFi أعلى يمين الشاشة
        val wifiButtonParams = FrameLayout.LayoutParams(
            (125 * density).toInt(),
            (55 * density).toInt()
        ).apply {
            gravity = Gravity.TOP or Gravity.RIGHT
            rightMargin = (8 * density).toInt()
            topMargin = (8 * density).toInt()
        }

        rootContainer.addView(
            wifiButton,
            wifiButtonParams
        )

        setContentView(rootContainer)

        // ضمان بقاء الزر فوق الموقع
        wifiButton.bringToFront()

        // إعدادات WebView
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

            mixedContentMode =
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient()

        // دعم الفيديو Full Screen
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(
                view: View?,
                callback: CustomViewCallback?
            ) {
                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }

                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                webView.visibility = View.GONE
                wifiButton.visibility = View.GONE

                rootContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                hideSystemUI()
            }

            override fun onHideCustomView() {
                exitFullScreen()
            }
        }

        // تنزيل الملفات
        webView.setDownloadListener {
                url,
                userAgent,
                contentDisposition,
                mimeType,
                _ ->

            try {
                val request =
                    DownloadManager.Request(Uri.parse(url))

                val cookies =
                    CookieManager.getInstance().getCookie(url)

                if (!cookies.isNullOrEmpty()) {
                    request.addRequestHeader(
                        "Cookie",
                        cookies
                    )
                }

                if (!userAgent.isNullOrEmpty()) {
                    request.addRequestHeader(
                        "User-Agent",
                        userAgent
                    )
                }

                val fileName =
                    URLUtil.guessFileName(
                        url,
                        contentDisposition,
                        mimeType
                    )

                request.setTitle(fileName)
                request.setDescription("جاري تنزيل الملف...")

                request.setNotificationVisibility(
                    DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )

                if (!mimeType.isNullOrEmpty()) {
                    request.setMimeType(mimeType)
                }

                val referer = webView.url

                if (!referer.isNullOrEmpty()) {
                    request.addRequestHeader(
                        "Referer",
                        referer
                    )
                }

                val downloadManager =
                    getSystemService(
                        Context.DOWNLOAD_SERVICE
                    ) as DownloadManager

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

        // فتح الموقع
        if (savedInstanceState == null) {
            webView.loadUrl("http://e7.net:888/")
        } else {
            webView.restoreState(savedInstanceState)
        }

        // زر الرجوع
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (customView != null) {
                        exitFullScreen()
                        return
                    }

                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @Suppress("DEPRECATION")
    private fun exitFullScreen() {

        val view = customView ?: return

        rootContainer.removeView(view)

        customView = null

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        webView.visibility = View.VISIBLE

        wifiButton.visibility = View.VISIBLE
        wifiButton.bringToFront()

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        if (customView != null) {
            exitFullScreen()
        }

        webView.stopLoading()
        webView.destroy()

        super.onDestroy()
    }
}
