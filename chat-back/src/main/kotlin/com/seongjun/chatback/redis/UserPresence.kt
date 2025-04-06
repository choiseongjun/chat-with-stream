package com.seongjun.chatback.redis

import com.seongjun.chatback.entity.User
import java.time.LocalDateTime

data class UserPresence(
    val userId: Long,
    val username: String,
    val status: User.UserStatus,
    val lastActiveAt: LocalDateTime
)