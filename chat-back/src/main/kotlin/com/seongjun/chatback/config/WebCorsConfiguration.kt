package com.seongjun.chatback.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebCorsConfiguration : WebFluxConfigurer {

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfiguration = CorsConfiguration().apply {
            // 모든 오리진 허용 (개발 환경)
            allowedOriginPatterns = listOf("*")

            // 구체적인 오리진을 사용하려면 아래 코드 사용
            // allowedOrigins = listOf("http://localhost:3000", "https://yourdomain.com")

            allowedMethods = listOf(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
            )
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-User-Id",
                "Accept",
                "Origin"
            )
            allowCredentials = true
            maxAge = 3600
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfiguration)
        }

        return CorsWebFilter(source)
    }

    // 전역 CORS 설정 추가 (선택적)
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}