package com.seongjun.chatback.repository

import com.seongjun.chatback.entity.ChatRoom
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomRepository : CoroutineCrudRepository<ChatRoom, Long> {
    @Query("SELECT * FROM chat_rooms WHERE is_private = false ORDER BY created_at DESC")
    fun findAllPublicRooms(): Flow<ChatRoom>

    @Query("""
        SELECT cr.* FROM chat_rooms cr
        JOIN room_memberships rm ON cr.id = rm.room_id
        WHERE rm.user_id = :userId
        ORDER BY cr.updated_at DESC
    """)
    fun findRoomsByUserId(userId: Long): Flow<ChatRoom>
}