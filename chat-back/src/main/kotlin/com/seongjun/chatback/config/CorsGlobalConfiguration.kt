package com.seongjun.chatback.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer


@Configuration
class CorsGlobalConfiguration : WebFluxConfigurer {
    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry.addMapping("/**")
            .allowedOrigins("http://allowed-origin.com")
            .allowedMethods("POST")
            .allowedMethods("GET")
            .allowedMethods("DELETE")
            .allowedMethods("PUT")
            .maxAge(3600)
    }
}