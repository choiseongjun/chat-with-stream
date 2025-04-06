package com.seongjun.chatback.jwt.filter

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class AuthenticationFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // 실제로는 헤더에서 토큰을 가져와 검증하는 로직이 필요함
        // 여기서는 테스트를 위해 간단하게 처리
        val userId = exchange.request.headers.getFirst("X-User-Id")?.toLongOrNull() ?: 1L

        // 사용자 ID를 요청에 추가 (컨트롤러에서 접근 가능하도록)
        val mutatedExchange = exchange.mutate()
            .request(exchange.request.mutate().build())
            .build()

        mutatedExchange.attributes["userId"] = userId

        return chain.filter(mutatedExchange)
    }
}