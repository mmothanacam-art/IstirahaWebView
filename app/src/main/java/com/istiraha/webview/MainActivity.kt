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

        // إنشاء WebView
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

                // إخفاء WebView أثناء ملء الشاشة
                webView.visibility = View.GONE

                // إضافة مشغل الفيديو فوق WebView
                rootContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                // تحويل الشاشة إلى الوضع الأفقي
                requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                hideSystemUI()
            }

            override fun onHideCustomView() {
                exitFullScreen()
            }
        }

  // تنزيل الصور والفيديوهات والملفات
webView.setDownloadListener {
        url,
        userAgent,
        contentDisposition,
        mimeType,
        _ ->
Toast.makeText(
    this,
    "الرابط الأصلي: $url",
    Toast.LENGTH_LONG
).show()
    val cookies =
        CookieManager.getInstance().getCookie(url)

    val fileName = URLUtil.guessFileName(
        url,
        contentDisposition,
        mimeType
    )

    val referer = webView.url

    val intent = android.content.Intent(
        this,
        DownloadActivity::class.java
    ).apply {
        putExtra("url", url)
        putExtra("fileName", fileName)
        putExtra("mimeType", mimeType)
        putExtra("userAgent", userAgent)
        putExtra("cookies", cookies)
        putExtra("referer", referer)
    }

    startActivity(intent)
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

                    // إذا كان الفيديو Full Screen
                    if (customView != null) {
                        exitFullScreen()
                        return
                    }

                    // الرجوع داخل الموقع
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    // إخفاء أشرطة النظام أثناء الفيديو
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

    // الخروج من Full Screen
    @Suppress("DEPRECATION")
    private fun exitFullScreen() {

        val view = customView ?: return

        rootContainer.removeView(view)

        customView = null

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null

        webView.visibility = View.VISIBLE

        // العودة للوضع العمودي
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        // إعادة أشرطة النظام
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
