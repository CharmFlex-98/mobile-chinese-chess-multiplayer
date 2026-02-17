package com.charmflex.xiangqi.server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
internal class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private val jwkUrl: String
) {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http // Disable CSRF if you're only building a REST API
            .csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/public/**", "/api/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer { oauth2: OAuth2ResourceServerConfigurer<HttpSecurity?> ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            } // modern replacement for oauth2.jwt()

        return http.build()
    }


    @Bean
    fun jwtDecoder(): JwtDecoder {
        // Replace with your actual JWKS URI
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwkUrl)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build()
        // ES256 uses Elliptic Curve keys, not RSA. This example assumes you're using appropriate EC keys.
        // For ES256, you'd configure the verifier accordingly, often handled implicitly by Nimbus if keys are properly formatted in JWKS.
        return decoder
    }
//
//    @Bean
//    fun jwtGrantedAuthoritiesConverter(): JwtGrantedAuthoritiesConverter {
//        val converter = JwtGrantedAuthoritiesConverter()
//        converter.setAuthorityPrefix("ROLE_") // Standard Spring Security prefix
//        converter.setAuthoritiesClaimName("roles") // Name of the claim holding roles
//        return converter
//    }
}