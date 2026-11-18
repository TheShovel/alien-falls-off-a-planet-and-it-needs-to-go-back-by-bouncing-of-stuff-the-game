package com.penguinmod.template.ui.view

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import com.penguinmod.template.BuildConfig

/**
 * Returns true when this is a low-end device, in which case the game renders
 * into a half-resolution backing store that is scaled up to fill the screen.
 * The WebView compositor then does 4x less fill work, which is the biggest
 * remaining FPS lever on weak GPUs. Touch is mapped by the FrameLayout's
 * view-level scale (scaleX/scaleY), so gameplay input lands 1:1 on the right
 * page coordinates.
 *
 * Detection uses [ActivityManager.isLowRamDevice], Android's own signal for
 * low-end hardware. The result is cached for the lifetime of the process.
 */
private var lowEndDeviceCache: Boolean? = null

fun isLowEndDevice(context: Context): Boolean = lowEndDeviceCache ?: run {
    val activityManager = context.applicationContext
        .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.isLowRamDevice.also { lowEndDeviceCache = it }
}

/**
 * Creates the game WebView fully configured and immediately starts loading the
 * game. Called in Activity.onCreate, before the Compose tree exists, so the
 * heavy page load overlaps with first-frame boot.
 *
 * The packaged PenguinMod project is served from APK assets over
 * https://appassets.androidplatform.net/, which is a secure origin: needed for
 * service workers, storage APIs and anything that rejects http:// or file://.
 */
fun createGameWebView(context: Context): android.webkit.WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    val webViewUrl = "https://appassets.androidplatform.net/assets/index.html"

    return android.webkit.WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setBackgroundColor(Color.BLACK)
        overScrollMode = View.OVER_SCROLL_NEVER
        isLongClickable = false
        isHapticFeedbackEnabled = false
        setOnLongClickListener { true }
        requestDisallowInterceptTouchEvent(true)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val settings = settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        // It's a game: pinch / double-tap zoom has no use and only costs re-layout.
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // The app theme is light, so Android would force-dark the WebView in
        // system dark mode. That ruins the game's art and burns CPU/GPU on
        // every frame. Turn it off explicitly.
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)

        // No point in Safe Browsing lookups for local content.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
        }

        webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldInterceptRequest(
                view: android.webkit.WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        if (BuildConfig.DEBUG) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(true)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    Log.d(
                        "PenguinModWeb",
                        "${msg.messageLevel()} ${msg.message()} @${msg.sourceId()}:${msg.lineNumber()}"
                    )
                    return super.onConsoleMessage(msg)
                }
            }
        }

        // API 26+: keep the renderer process at high priority so system
        // overlays (volume/brightness popups, notifications) don't tank fps.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setRendererPriorityPolicy(
                android.webkit.WebView.RENDERER_PRIORITY_IMPORTANT,
                false
            )
        }

        loadUrl(webViewUrl)
    }
}

@Composable
fun GameWebView(webView: android.webkit.WebView) {
    val context = LocalContext.current
    if (isLowEndDevice(context)) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current.density

        // Render at 50% of the screen resolution and scale up.
        val renderWidth = (configuration.screenWidthDp * 0.5f * density).toInt()
        val renderHeight = (configuration.screenHeightDp * 0.5f * density).toInt()

        AndroidView(
            factory = {
                FrameLayout(it).apply {
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            renderWidth,
                            renderHeight,
                            android.view.Gravity.CENTER
                        )
                    )
                    webView.scaleX = 2f
                    webView.scaleY = 2f
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}
