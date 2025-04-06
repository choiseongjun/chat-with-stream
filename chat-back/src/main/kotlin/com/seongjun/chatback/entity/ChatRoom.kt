package com.seongjun.chatback.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("chat_rooms")
data class ChatRoom(
    @Id val id: Long? = null,
    @Column("name") val name: String,
    @Column("description") val description: String? = null,
    @Column("created_by") val createdBy: Long,
    @Column("is_private") val isPrivate: Boolean = false,
    @Column("max_users") val maxUsers: Int? = null,
    @Column("created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)