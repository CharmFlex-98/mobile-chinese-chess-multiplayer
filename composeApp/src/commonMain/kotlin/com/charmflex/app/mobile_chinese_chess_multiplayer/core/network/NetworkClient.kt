package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

import kotlinx.serialization.KSerializer

interface NetworkClient {
    suspend fun <T: Any> get(endpoint: String, serializer: KSerializer<T>, networkAttributes: List<NetworkAttribute<Any>>? = null): T

    suspend fun <T : Any, R: Any> post(endpoint: String, body: T, requestSerializer: KSerializer<T>, responseSerializer: KSerializer<R>, networkAttributes: List<NetworkAttribute<Any>>? = null): R

    suspend fun <T: Any, R: Any> patch(endpoint: String, body: T, requestSerializer: KSerializer<T>, responseSerializer: KSerializer<R>): R

    suspend fun <T, R: Any> delete(endpoint: String, body: T, responseSerializer: KSerializer<R>): R

    suspend fun <T : Any, R: Any> put(endpoint: String, body: T, requestSerializer: KSerializer<T>, responseSerializer: KSerializer<R>): R
}

data class NetworkAttribute<out T : Any>(
    val name: String,
    val value: T,
)

object NetworkAttributes {
    val needToken = NetworkAttribute("needToken", true)
}