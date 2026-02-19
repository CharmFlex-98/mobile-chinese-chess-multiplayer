package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.auth.domain.AuthService
import com.charmflex.app.mobile_chinese_chess_multiplayer.feature.session.SessionManager
import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey
import org.koin.core.annotation.Singleton

@Singleton
internal class AccessTokenInjector(
    private val sessionManager: SessionManager
) : NetworkInterceptor<HttpRequestBuilder, HttpClientCall> {
    override suspend fun intercept(
        request: HttpRequestBuilder,
        chain: NetworkInterceptor.InterceptorChain<HttpRequestBuilder, HttpClientCall>
    ): HttpClientCall {
        val needToken = request.attributes.getOrNull(AttributeKey(NetworkAttributes.needToken.name)) == true
        if (!needToken) {
            return chain.proceed(request)
        }

        request.headers.apply {
            val token = sessionManager.currentUserSession.value?.token
            if (token.isNullOrEmpty()) {
                return@apply
            }

            append(AUTHORIZATION_HEADER, "Bearer $token")
        }
        return chain.proceed(request)
    }
}

private const val AUTHORIZATION_HEADER = "Authorization"