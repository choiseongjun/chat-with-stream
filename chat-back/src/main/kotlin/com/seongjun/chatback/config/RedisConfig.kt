package com.seongjun.chatback.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import com.seongjun.chatback.redis.RedisMessage
import com.seongjun.chatback.redis.UserPresence
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class RedisConfig {
    @Bean
    fun reactiveRedisMessageTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, RedisMessage> {
        val serializer = Jackson2JsonRedisSerializer(RedisMessage::class.java)
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, RedisMessage>(StringRedisSerializer())
            .value(serializer)
            .build()

        return ReactiveRedisTemplate(factory, serializationContext)
    }
    @Bean
    fun reactiveRedisUserPresenceTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, UserPresence> {
        val serializer = Jackson2JsonRedisSerializer(UserPresence::class.java)
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, UserPresence>(StringRedisSerializer())
            .value(serializer)
            .build()

        return ReactiveRedisTemplate(factory, serializationContext)
    }
    @Bean
    fun objectMapper(): ObjectMapper {
        return Jackson2ObjectMapperBuilder()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .modulesToInstall(JavaTimeModule())
            .build()
    }
}