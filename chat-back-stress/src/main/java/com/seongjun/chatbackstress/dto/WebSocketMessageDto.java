package com.seongjun.chatbackstress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDto {
    private String type; // CHAT, ENTER, READ, HISTORY 등
    private String roomId;
    private String sender;
    private String message;
    private LocalDateTime timestamp;
}