package com.seongjun.chatbackstress.repository;

import com.seongjun.chatbackstress.entity.ChatMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ChatMessageRepository extends R2dbcRepository<ChatMessage, Long> {
    Flux<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId);
}