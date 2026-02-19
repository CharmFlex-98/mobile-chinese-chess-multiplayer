package com.charmflex.xiangqi.server.exception

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
internal class GlobalExceptionHandler {
    @ExceptionHandler(ExceptionBase::class)
    fun handleException(exceptionBase: ExceptionBase): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "errorCode" to exceptionBase.errorCode,
            "errorMessage" to exceptionBase.errorMessage
        )
        return ResponseEntity
            .status(exceptionBase.statusCode)
            .body(body)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleSecurityException(exception: BadCredentialsException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "errorCode" to "5555",
            "errorMessage" to "Cannot"
        )
        return ResponseEntity
            .status(HttpServletResponse.SC_BAD_GATEWAY)
            .body(body)
    }
}

abstract class ExceptionBase(
    val statusCode: Int,
    val errorCode: String,
    val errorMessage: String
) : Throwable() {
    fun toBodyString(): String {
        val objectMapper = ObjectMapper()
        return objectMapper.writer().writeValueAsString(mapOf("errorCode" to errorCode, "errorMessage" to errorCode))
    }
}

object GenericException : ExceptionBase(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "GENERIC_ERROR", "generic error occurred.")
object UnauthorizedException : ExceptionBase(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "unauthorized access!")
