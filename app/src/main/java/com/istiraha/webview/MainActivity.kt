package com.istiraha.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var rootContainer: FrameLayout

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // الحاوية الرئيسية
        rootContainer = FrameLayout(this)

        // WebView
        webView = WebView(this)

        rootContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(rootContainer)

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

            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient()

        // دعم الفيديو بملء الشاشة
        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowCustomView(
                view: View?,
                callback: CustomViewCallback?
            ) {

                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }

                // إذا كان Fullscreen مفتوحًا بالفعل
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                // إخفاء صفحة الموقع مؤقتًا
                webView.visibility = View.GONE

                // إضافة الفيديو إلى الشاشة كاملة
                rootContainer.addView(
                    customView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                // الوضع الأفقي
                requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                // إخفاء شريط الحالة والتنقل
                hideSystemUI()
            }

            override fun onHideCustomView() {
                exitFullScreen()
            }
        }

        // دعم تنزيل الصور والفيديوهات والملفات
        webView.setDownloadListener {
                url,
                userAgent,
                contentDisposition,
                mimeType,
                _ ->

            try {

                val request = DownloadManager.Request(Uri.parse(url))

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

                request.setDescription(
                    "جاري تنزيل الملف..."
                )

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

            webView.loadUrl(
                "http://e7.net:888/"
            )

        } else {

            webView.restoreState(
                savedInstanceState
            )
        }

        // زر الرجوع
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressed
