package com.penguinmod.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.penguinmod.template.ui.view.GameWebView
import com.penguinmod.template.ui.view.createGameWebView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Hide both bars: on tall phones the stage is as large as the
            // screen allows, and a status bar over the game looks unfinished.
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // Create the WebView and start loading the game immediately, so the
        // heavy page load (script parse, project.json deserialize, hundreds of
        // asset decodes) overlaps with first-frame boot.
        val webView = createGameWebView(this)

        setContent {
            GameWebView(webView)
        }
    }
}
