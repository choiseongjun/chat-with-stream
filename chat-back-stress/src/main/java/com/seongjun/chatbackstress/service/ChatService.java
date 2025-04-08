package com.seongjun.chatbackstress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seongjun.chatbackstress.dto.WebSocketMessageDto;
import com.seongjun.chatbackstress.entity.ChatMessage;
import com.seongjun.chatbackstress.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CHAT_KEY_PREFIX = "chat:room:";
    private static final int MESSAGE_RETENTION = 100; // 각 방마다 최근 100개의 메시지만 유지

    // @PostConstruct
    // public void init() {
    //     // 서버 시작 시 Redis의 모든 채팅 데이터 삭제 (선택사항)
    //     redisTemplate.keys(CHAT_KEY_PREFIX + "*")
    //         .flatMap(redisTemplate.opsForList()::delete)
    //         .subscribe();
    // }

    public Mono<Void> save(WebSocketMessageDto dto) {
        LocalDateTime now = LocalDateTime.now();
        ChatMessage entity = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : now)
                .createdAt(now)
                .build();

        // PostgreSQL에 저장
        Mono<Void> dbSave = chatMessageRepository.save(entity).then();
        
        // Redis에 저장
        Mono<Void> redisSave = saveToRedis(dto);

        // 두 저장소에 모두 저장
        return Mono.when(dbSave, redisSave);
    }

    private Mono<Void> saveToRedis(WebSocketMessageDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            String key = CHAT_KEY_PREFIX + dto.getRoomId();
            
            return redisTemplate.opsForList()
                    .leftPush(key, json)
                    .then(redisTemplate.opsForList().trim(key, 0, MESSAGE_RETENTION - 1))
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for Redis: {}", e.getMessage());
            return Mono.empty();
        }
    }

    public Flux<ChatMessage> getMessages(String roomId) {
        String key = CHAT_KEY_PREFIX + roomId;
        
        // Redis에서 메시지 조회
        Flux<ChatMessage> redisMessages = redisTemplate.opsForList().range(key, 0, -1)
                .map(json -> {
                    try {
                        WebSocketMessageDto dto = objectMapper.readValue(json, WebSocketMessageDto.class);
                        return ChatMessage.builder()
                                .roomId(dto.getRoomId())
                                .sender(dto.getSender())
                                .message(dto.getMessage())
                                .timestamp(dto.getTimestamp())
                                .createdAt(dto.getTimestamp())
                                .build();
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing message from Redis: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(message -> message != null);

        // PostgreSQL에서 메시지 조회
        Flux<ChatMessage> dbMessages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId);

        // Redis에 데이터가 없거나 PostgreSQL과 불일치하는 경우 동기화 수행
        return redisMessages
                .collectList()
                .flatMapMany(redisMsgs -> {
                    if (redisMsgs.isEmpty()) {
                        // Redis가 비어있으면 PostgreSQL 데이터로 채움
                        return syncFromPostgresToRedis(roomId, dbMessages);
                    }
                    
                    // PostgreSQL의 최근 메시지 수와 Redis의 메시지 수 비교
                    return dbMessages
                            .collectList()
                            .flatMapMany(dbMsgs -> {
                                if (dbMsgs.size() > redisMsgs.size()) {
                                    // PostgreSQL에 더 많은 메시지가 있으면 동기화
                                    return syncFromPostgresToRedis(roomId, Flux.fromIterable(dbMsgs));
                                }
                                // Redis 데이터 반환
                                return Flux.fromIterable(redisMsgs);
                            });
                })
                .sort(Comparator.comparing(ChatMessage::getCreatedAt).reversed());
    }

    private Flux<ChatMessage> syncFromPostgresToRedis(String roomId, Flux<ChatMessage> dbMessages) {
        return dbMessages
                .collectList()
                .flatMapMany(messages -> {
                    String key = CHAT_KEY_PREFIX + roomId;
                    
                    // 최근 MESSAGE_RETENTION개의 메시지만 유지
                    List<ChatMessage> recentMessages = messages.stream()
                            .sorted(Comparator.comparing(ChatMessage::getCreatedAt).reversed())
                            .limit(MESSAGE_RETENTION)
                            .collect(Collectors.toList());

                    // Redis 데이터 삭제 후 다시 저장
                    return redisTemplate.opsForList().delete(key)
                            .thenMany(Flux.fromIterable(recentMessages))
                            .flatMap(message -> {
                                try {
                                    WebSocketMessageDto dto = WebSocketMessageDto.builder()
                                            .type("HISTORY")
                                            .roomId(message.getRoomId())
                                            .sender(message.getSender())
                                            .message(message.getMessage())
                                            .timestamp(message.getCreatedAt())
                                            .build();
                                    String json = objectMapper.writeValueAsString(dto);
                                    return redisTemplate.opsForList().rightPush(key, json)
                                            .thenReturn(message);
                                } catch (JsonProcessingException e) {
                                    log.error("Error during sync: {}", e.getMessage());
                                    return Mono.just(message);
                                }
                            });
                });
    }
}