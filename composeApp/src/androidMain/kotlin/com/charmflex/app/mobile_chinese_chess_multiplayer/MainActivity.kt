package com.charmflex.app.mobile_chinese_chess_multiplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.di.AppModule
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppModule.initialize()

        // Handle deep link if app was launched from OAuth redirect
        handleOAuthDeepLink(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthDeepLink(intent)
    }

    private fun handleOAuthDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == AppModule.DEEP_LINK_SCHEME && uri.host == AppModule.DEEP_LINK_HOST) {
            val code = uri.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                try {
                    AppModule.instance.supabaseAuthClient.exchangeCodeForSession(code)
                } catch (e: Exception) {
                    println("[AUTH] OAuth code exchange failed: ${e.message}")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
