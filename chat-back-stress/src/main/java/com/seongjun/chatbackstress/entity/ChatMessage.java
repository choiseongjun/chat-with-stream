package com.seongjun.chatbackstress.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name="chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    private Long id;
    private String roomId;
    private String sender;
    private String message;
    private LocalDateTime timestamp;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
