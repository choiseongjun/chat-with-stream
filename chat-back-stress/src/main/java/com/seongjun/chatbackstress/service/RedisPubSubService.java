package com.seongjun.chatbackstress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seongjun.chatbackstress.dto.WebSocketMessageDto;
import com.seongjun.chatbackstress.utils.ChatSessionManager;
import io.micrometer.observation.ObservationFilter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
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
    private final ObjectMapper objectMapper;
    private final ChatSessionManager sessionManager;
    private final Map<String, Sinks.Many<String>> sinkMap = new ConcurrentHashMap<>();
    private ReactiveSubscription subscription;

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
                        Sinks.Many<String> sink = sinkMap.get(sessionId);
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
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("chat", json)
                .doOnError(error -> log.error("Error publishing message: {}", error.getMessage()))
                .subscribe();
        } catch (JsonProcessingException e) {
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

    public Flux<String> getMessageStream(WebSocketSession session) {
        return redisTemplate.listenTo(ChannelTopic.of("chat"))
                .map(message -> message.getMessage());
    }

    public void registerSink(String sessionId, Sinks.Many<String> sink) {
        sinkMap.put(sessionId, sink);
        log.info("Registered sink for session: {}", sessionId);
    }

    public void unregisterSink(String sessionId) {
        Sinks.Many<String> sink = sinkMap.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
        log.info("Unregistered sink for session: {}", sessionId);
    }
}