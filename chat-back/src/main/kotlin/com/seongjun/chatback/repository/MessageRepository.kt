package com.seongjun.chatback.repository

import com.seongjun.chatback.entity.Message
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MessageRepository : CoroutineCrudRepository<Message, Long> {
    fun findByRoomIdOrderByCreatedAtDesc(roomId: Long): Flow<Message>

    @Query("""
        SELECT * FROM messages
        WHERE room_id = :roomId AND created_at < :before
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findMessagesBefore(roomId: Long, before: LocalDateTime, limit: Int): Flow<Message>

    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE room_id = :roomId AND created_at > :since
    """)
    suspend fun countUnreadMessages(roomId: Long, since: LocalDateTime): Long
}