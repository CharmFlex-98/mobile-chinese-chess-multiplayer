package com.charmflex.xiangqi.server.model

data class LoginVerifyRequest(
    val token: String,
    val uid: String,
    val displayName: String
)

data class LoginVerifyResponse(
    val token: String,
    val uid: String,
    val displayName: String,
    val guest: Boolean
)