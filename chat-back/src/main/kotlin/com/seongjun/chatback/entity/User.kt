package com.seongjun.chatback.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
data class User(
    @Id val id: Long? = null,
    @Column("username") val username: String,
    @Column("email") val email: String,
    @Column("status") val status: UserStatus = UserStatus.OFFLINE,
    @Column("last_active_at") val lastActiveAt: LocalDateTime = LocalDateTime.now(),
    @Column("created_at") val createdAt: LocalDateTime = LocalDateTime.now()
) {
    enum class UserStatus {
        ONLINE, OFFLINE, AWAY
    }
}