package com.seongjun.chatback.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("messages")
data class Message(
    @Id val id: Long? = null,
    @Column("room_id") val roomId: Long,
    @Column("sender_id") val senderId: Long,
    @Column("content") val content: String,
    @Column("type") val type: MessageType = MessageType.TEXT,
    @Column("created_at") val createdAt: LocalDateTime = LocalDateTime.now()
) {
    enum class MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}