package com.seongjun.chatbackstress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seongjun.chatbackstress.dto.WebSocketMessageDto;
import com.seongjun.chatbackstress.utils.ChatSessionManager;
import io.micrometer.observation.ObservationFilter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPubSubService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveRedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final ChatSessionManager sessionManager;
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();
    private final ReactiveRedisMessageListenerContainer container;

    @PostConstruct
    public void subscribe() {
        redisTemplate.listenTo(ChannelTopic.of("chat"))
            .doOnNext(message -> {
                try {
                    WebSocketMessageDto dto = objectMapper.readValue(message.getMessage(), WebSocketMessageDto.class);
                    String roomId = dto.getRoomId();
                    
                    // 해당 방의 세션들에게만 메시지 전달
                    sessionManager.getSessions(roomId).forEach(session -> {
                        String sessionId = session.getId();
                        Sinks.Many<String> sink = sinks.get(sessionId);
                        if (sink != null && session.isOpen()) {
                            sink.tryEmitNext(message.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.error("Error processing Redis message: {}", e.getMessage());
                }
            })
            .doOnError(error -> log.error("Redis subscription error: {}", error.getMessage()))
            .retry()
            .subscribe();
    }

    public void publishMessage(WebSocketMessageDto message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat", jsonMessage)
                    .doOnError(e -> log.error("Error publishing message: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error serializing message: {}", e.getMessage());
        }
    }

    public Flux<WebSocketMessage> subscribeToRoom(WebSocketSession session) {
        return redisTemplate.listenTo(ChannelTopic.of("chat"))
                .map(message -> {
                    try {
                        return session.textMessage(message.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .doOnError(Throwable::printStackTrace);
    }

    public Flux<String> getMessageStream() {
        return container.receive(ChannelTopic.of("chat"))
                .map(message -> message.getMessage())
                .doOnError(e -> log.error("Error in message stream: {}", e.getMessage()));
    }

    public void registerSink(String sessionId, Sinks.Many<String> sink) {
        sinks.put(sessionId, sink);
        
        // Redis 채널 구독
        container.receive(ChannelTopic.of("chat"))
                .map(message -> message.getMessage())
                .doOnNext(message -> {
                    sinks.values().forEach(s -> s.tryEmitNext(message));
                })
                .doOnError(e -> log.error("Error in Redis subscription: {}", e.getMessage()))
                .subscribe();
    }

    public void unregisterSink(String sessionId) {
        sinks.remove(sessionId);
    }
}