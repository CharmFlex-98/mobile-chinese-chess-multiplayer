package com.charmflex.app.mobile_chinese_chess_multiplayer.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAuthClient(
    val supabaseClient: SupabaseClient
) {
    val sessionStatus: Flow<SessionStatus> = supabaseClient.auth.sessionStatus

    suspend fun signInWithGoogle() {
        supabaseClient.auth.signInWith(Google)
    }

    suspend fun exchangeCodeForSession(code: String) {
        supabaseClient.auth.exchangeCodeForSession(code)
    }

    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    suspend fun refreshSession() {
        supabaseClient.auth.refreshCurrentSession()
    }

    fun currentAccessToken(): String? {
        return supabaseClient.auth.currentSessionOrNull()?.accessToken
    }

    fun currentUserId(): String? {
        return supabaseClient.auth.currentSessionOrNull()?.user?.id
    }

    fun currentUserEmail(): String? {
        return supabaseClient.auth.currentSessionOrNull()?.user?.email
    }

    fun currentDisplayName(): String? {
        val user = supabaseClient.auth.currentSessionOrNull()?.user ?: return null
        val metadata = user.userMetadata ?: return user.email?.substringBefore("@")
        return metadata["display_name"]?.jsonPrimitive?.content
            ?: metadata["full_name"]?.jsonPrimitive?.content
            ?: metadata["name"]?.jsonPrimitive?.content
            ?: user.email?.substringBefore("@")
    }
}
