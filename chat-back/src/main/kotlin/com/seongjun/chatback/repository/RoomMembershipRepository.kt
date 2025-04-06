package com.seongjun.chatback.repository

import com.seongjun.chatback.entity.RoomMembership
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RoomMembershipRepository : CoroutineCrudRepository<RoomMembership, Long> {
    suspend fun findByRoomIdAndUserId(roomId: Long, userId: Long): RoomMembership?

    fun findByRoomId(roomId: Long): Flow<RoomMembership>

    fun findByUserId(userId: Long): Flow<RoomMembership>

    @Modifying
    @Query("UPDATE room_memberships SET last_read_at = now() WHERE room_id = :roomId AND user_id = :userId")
    suspend fun updateLastReadAt(roomId: Long, userId: Long)

    @Query("SELECT COUNT(*) FROM room_memberships WHERE room_id = :roomId")
    suspend fun countByRoomId(roomId: Long): Long
}