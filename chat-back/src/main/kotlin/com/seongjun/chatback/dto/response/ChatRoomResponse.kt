package com.seongjun.chatback.dto.response

import java.time.LocalDateTime

data class ChatRoomResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdBy: Long,
    val isPrivate: Boolean,
    val maxUsers: Int?,
    val memberCount: Int = 0,
    val unreadCount: Int = 0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)