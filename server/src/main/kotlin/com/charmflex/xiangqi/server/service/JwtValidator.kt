package com.charmflex.xiangqi.server.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service


@Service
class JwtValidator(
//    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private val jwtDecoder: JwtDecoder
) {
    private val log = LoggerFactory.getLogger(JwtValidator::class.java)

    fun validateAndGetUserId(token: String): JwtResult? {
        return try {
            if (token.isBlank()) {
                throw Exception("Not a valid token. Token is blanked.")
            }

            val jwt = jwtDecoder.decode(token)
            log.info("[JWT] Token validated. subject={}", jwt.subject)
            val isAnonymous = (jwt.claims["is_anonymous"] as? Boolean)?: false

            JwtResult(jwt.subject, isAnonymous)
        } catch (e: Exception) {
            log.error("[JWT] Failed to decode token: {}", e.message)
            null
        }
    }

    fun isJwtToken(token: String): Boolean {
        return token.count { it == '.' } == 2
    }
}

data class JwtResult(
    val userId: String,
    val guest: Boolean = false
)
