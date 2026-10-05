package com.istiraha.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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

        rootContainer = FrameLayout(this)
        webView = WebView(this)

        rootContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(rootContainer)

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

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }

        // Fullscreen video
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

        // اعتراض أي تنزيل صادر من الموقع
        webView.setDownloadListener {
                url,
                userAgent,
                contentDisposition,
                mimeType,
                _ ->

            if (url.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "رابط التنزيل غير صالح",
                    Toast.LENGTH_LONG
                ).show()
                return@setDownloadListener
            }

            // blob يحتاج معالجة مختلفة
            if (url.startsWith("blob:", ignoreCase = true)) {
                AlertDialog.Builder(this)
                    .setTitle("رابط فيديو مؤقت")
                    .setMessage(
                        "هذا الملف يستخدم رابط Blob داخل صفحة الويب، " +
                        "ولا يمكن تنزيله كملف HTTP مباشر بهذه الطريقة."
                    )
                    .setPositiveButton("حسنًا", null)
                    .show()

                return@setDownloadListener
            }

            if (!url.startsWith("http://", true) &&
                !url.startsWith("https://", true)
            ) {
                Toast.makeText(
                    this,
                    "نوع رابط التنزيل غير مدعوم: ${Uri.parse(url).scheme}",
                    Toast.LENGTH_LONG
                ).show()

                return@setDownloadListener
            }

            val fileName = URLUtil.guessFileName(
                url,
                contentDisposition,
                mimeType
            )

            val cookies =
                CookieManager.getInstance().getCookie(url) ?: ""

            val referer =
                webView.url ?: "http://e7.net:888/"

            DownloadActivity.start(
                context = this,
                url = url,
                fileName = fileName,
                mimeType = mimeType ?: "application/octet-stream",
                userAgent = userAgent ?: webView.settings.userAgentString,
                cookies = cookies,
                referer = referer
            )
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
