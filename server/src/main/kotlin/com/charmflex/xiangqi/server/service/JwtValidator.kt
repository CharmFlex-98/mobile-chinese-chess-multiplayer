package com.charmflex.xiangqi.server.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JwtValidator(
    @Value("\${supabase.jwt-secret:}") private val jwtSecret: String
) {
    private val log = LoggerFactory.getLogger(JwtValidator::class.java)

    fun validateAndGetUserId(token: String): String? {
        if (jwtSecret.isBlank()) {
            log.warn("[JWT] No JWT secret configured, skipping validation - extracting sub from unverified token")
            return try {
                val decoded = JWT.decode(token)
                decoded.subject
            } catch (e: Exception) {
                log.error("[JWT] Failed to decode token: {}", e.message)
                null
            }
        }

        return try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm).build()
            val decoded = verifier.verify(token)
            val userId = decoded.subject
            log.info("[JWT] Token validated, userId={}", userId)
            userId
        } catch (e: Exception) {
            log.error("[JWT] Token validation failed: {}", e.message)
            null
        }
    }

    fun isJwtToken(token: String): Boolean {
        return token.count { it == '.' } == 2
    }
}
