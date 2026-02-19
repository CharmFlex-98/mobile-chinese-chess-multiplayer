package com.charmflex.app.mobile_chinese_chess_multiplayer.core.network

interface NetworkInterceptor<REQ, RES> {
    suspend fun intercept(request: REQ, chain: InterceptorChain<REQ, RES>): RES

    class InterceptorChain<REQ, RES>(
        interceptors: List<NetworkInterceptor<REQ, RES>>,
        private val onRequest: suspend (REQ) -> RES
    ) {
        private val _interceptors: List<NetworkInterceptor<REQ, RES>> = interceptors
        private var currentIndex = 0

        suspend fun proceed(request: REQ): RES {
            if (currentIndex == _interceptors.size) {
                return onRequest(request)
            }

            return _interceptors[currentIndex++].intercept(request, this)
        }
    }
}