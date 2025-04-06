package com.seongjun.chatback.redis

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.seongjun.chatback.entity.Message
import java.time.Instant
import java.time.LocalDateTime

data class RedisMessage(
    val id: Long = 0L,
    val roomId: Long = 0L,
    val senderId: Long = 0L,
    val senderName: String = "",
    val content: String = "",
    val type: Message.MessageType = Message.MessageType.TEXT,
    val createdAt: Long = System.currentTimeMillis()
)