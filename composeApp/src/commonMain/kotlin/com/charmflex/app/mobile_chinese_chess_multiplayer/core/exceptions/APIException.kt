package com.charmflex.app.mobile_chinese_chess_multiplayer.core.exceptions


internal data class APIException(
    val errorCode: Int,
    val errorMessage: String
) : Exception(errorMessage)