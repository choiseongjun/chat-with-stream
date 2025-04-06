package com.seongjun.chatback.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("room_memberships")
data class RoomMembership(
    @Id val id: Long? = null,
    @Column("room_id") val roomId: Long,
    @Column("user_id") val userId: Long,
    @Column("role") val role: MemberRole = MemberRole.MEMBER,
    @Column("joined_at") val joinedAt: LocalDateTime = LocalDateTime.now(),
    @Column("last_read_at") val lastReadAt: LocalDateTime = LocalDateTime.now()
) {
    enum class MemberRole {
        OWNER, ADMIN, MEMBER
    }
}
