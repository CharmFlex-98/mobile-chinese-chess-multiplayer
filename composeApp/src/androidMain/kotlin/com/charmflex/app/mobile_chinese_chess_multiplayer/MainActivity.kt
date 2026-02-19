package com.charmflex.app.mobile_chinese_chess_multiplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependencies
import com.charmflex.app.mobile_chinese_chess_multiplayer.di.AppDependenciesProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var appDependencies: AppDependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        appDependencies = (application as AppDependenciesProvider).provideAppDependencies()
        val routeNavigator = appDependencies.provideRouteNavigator()
        val toastManager = appDependencies.provideToastManager()

        setContent {
            App(
                routeNavigator,
                toastManager
            ) {
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthDeepLink(intent)
    }

    private fun handleOAuthDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == AppConstant.DEEP_LINK_SCHEME && uri.host == AppConstant.DEEP_LINK_HOST) {
            val code = uri.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                try {
                    appDependencies.provideAuthService().exchangeCodeForSession(code)
                } catch (e: Exception) {
                    println("[AUTH] OAuth code exchange failed: ${e.message}")
                }
            }
        }
    }
}
