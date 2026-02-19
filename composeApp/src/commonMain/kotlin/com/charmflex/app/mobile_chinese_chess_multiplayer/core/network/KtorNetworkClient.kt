package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network


import com.charmflex.app.mobile_chinese_chess_multiplayer.core.config.AppConfigProvider
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.exceptions.APIException
import com.charmflex.app.mobile_chinese_chess_multiplayer.core.network.NetworkInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Singleton

@Singleton
class KtorNetworkClient(
    private val appConfigProvider: AppConfigProvider,
    private val interceptors: List<NetworkInterceptor<HttpRequestBuilder, HttpClientCall>>
) : NetworkClient {
    private val baseUrl = appConfigProvider.baseUrl()
    private val httpClient: HttpClient = getClient()

    private fun getClient(): HttpClient {
        val client =  HttpClient {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("==========")
                        println("HTTP::$message")
                        println("==========")
                    }
                }
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
        }
        client.plugin(HttpSend).intercept { request ->
            val chain = NetworkInterceptor.InterceptorChain(interceptors) {
                execute(request)
            }

            chain.proceed(request)
        }

        return client
    }

    override suspend fun <T : Any> get(
        endpoint: String,
        serializer: KSerializer<T>,
        networkAttributes: List<NetworkAttribute<Any>>?
    ): T {
        val response = httpClient.get {
            append(endpoint)
            networkAttributes?.forEach {
                attributes.put(AttributeKey(it.name), it.value)
            }
        }
        return decodeResponse(response, serializer)
    }

    override suspend fun <T: Any, R : Any> post(
        endpoint: String,
        body: T,
        requestSerializer: KSerializer<T>,
        responseSerializer: KSerializer<R>,
        networkAttributes: List<NetworkAttribute<Any>>?
    ): R {
        val response = httpClient.post {
            append(endpoint)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(requestSerializer, body))
            networkAttributes?.forEach {
                attributes.put(AttributeKey(it.name), it.value)
            }
        }
        return decodeResponse(response, responseSerializer)
    }

    override suspend fun <T : Any, R : Any> patch(
        endpoint: String,
        body: T,
        requestSerializer: KSerializer<T>,
        responseSerializer: KSerializer<R>
    ): R {
        val response = httpClient.patch {
            append(endpoint)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(requestSerializer, body))
        }
        return decodeResponse(response, responseSerializer)
    }

    override suspend fun <T, R : Any> delete(
        endpoint: String,
        body: T,
        responseSerializer: KSerializer<R>
    ): R {
        val response = httpClient.delete {
            append(endpoint)
        }
        return decodeResponse(response, responseSerializer)
    }

    override suspend fun <T : Any, R : Any> put(
        endpoint: String,
        body: T,
        requestSerializer: KSerializer<T>,
        responseSerializer: KSerializer<R>
    ): R {
        val response = httpClient.put {
            append(endpoint)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(requestSerializer, body))
        }
        return decodeResponse(response, responseSerializer)
    }

    private fun HttpRequestBuilder.append(url: String) {
        if (url.contains("https")) {
            url(url.trimStart('/'))
        } else {
            url("$baseUrl/${url.trimStart('/')}")
        }
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }

    private suspend fun <T : Any> decodeResponse(
        response: HttpResponse,
        serializer: KSerializer<T>
    ): T {
        if (response.status.isSuccess().not()) {
            throw APIException(response.status.value, response.bodyAsText())
        }
        val text = response.bodyAsText()
        return Json.decodeFromString(serializer, text)
    }
}

suspend inline fun <reified T : Any> NetworkClient.useGet(endPoint: String, attributesBuilder: MutableList<NetworkAttribute<Any>>.() -> Unit): T {
    val attrs = mutableListOf<NetworkAttribute<Any>>()
        .apply {
            attributesBuilder()
        }
    return this.get(endPoint, serializer<T>(), attrs.toList())
}

suspend inline fun <reified T : Any, reified U : Any> NetworkClient.usePost(
    endPoint: String,
    body: T,
    attributesBuilder: MutableList<NetworkAttribute<Any>>.() -> Unit = {}
): U {
    val attrs = mutableListOf<NetworkAttribute<Any>>()
        .apply {
            attributesBuilder()
        }
    return this.post(endPoint, body, serializer<T>(), serializer<U>(), attrs)
}

suspend inline fun <reified T : Any, reified U : Any> NetworkClient.usePatch(
    endPoint: String,
    body: T
) = this.patch(endPoint, body, serializer<T>(), serializer<U>())

suspend inline fun <T, reified U : Any> NetworkClient.delete(
    endPoint: String,
    body: T
) = this.delete(endPoint, body, serializer<U>())

suspend inline fun <reified T : Any, reified U : Any> NetworkClient.usePut(
    endPoint: String,
    body: T
) = this.put(endPoint, body, serializer<T>(), serializer<U>())