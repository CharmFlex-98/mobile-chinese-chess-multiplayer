package com.charmflex.app.mobile_chinese_chess_multiplayer.core.utils

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.data.SupabaseAuthClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Factory
class DeepLinkHandler(
    private val supabaseAuthClient: SupabaseAuthClient
) : KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun handle(urlString: String) {
        scope.launch {
            supabaseAuthClient.handleDeepLink(urlString)
        }
    }
}