package com.seongjun.chatback.dto.response

import com.seongjun.chatback.entity.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val status: User.UserStatus,
    val lastActiveAt: LocalDateTime,
    val createdAt: LocalDateTime
)
