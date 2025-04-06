package com.seongjun.chatback.dto.response

import com.seongjun.chatback.entity.Message
import java.time.LocalDateTime

data class MessageResponse(
    val id: Long,
    val roomId: Long,
    val senderId: Long,
    val senderName: String,
    val content: String,
    val type: Message.MessageType,
    val createdAt: LocalDateTime
)