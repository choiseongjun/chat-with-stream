package com.seongjun.chatback.service

import com.seongjun.chatback.dto.request.CreateRoomRequest
import com.seongjun.chatback.dto.response.ChatRoomResponse
import com.seongjun.chatback.entity.ChatRoom
import com.seongjun.chatback.entity.RoomMembership
import com.seongjun.chatback.repository.ChatRoomRepository
import com.seongjun.chatback.repository.MessageRepository
import com.seongjun.chatback.repository.RoomMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatRoomService(
    private val chatRoomRepository: ChatRoomRepository,
    private val roomMembershipRepository: RoomMembershipRepository,
    private val messageRepository: MessageRepository
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    suspend fun createRoom(userId: Long, request: CreateRoomRequest): ChatRoomResponse {
        val chatRoom = ChatRoom(
            name = request.name,
            description = request.description,
            createdBy = userId,
            isPrivate = request.isPrivate,
            maxUsers = request.maxUsers
        )

        val savedRoom = chatRoomRepository.save(chatRoom)

        // 방 생성자를 방의 OWNER로 추가
        val membership = RoomMembership(
            roomId = savedRoom.id!!,
            userId = userId,
            role = RoomMembership.MemberRole.OWNER
        )
        roomMembershipRepository.save(membership)

        return mapToChatRoomResponse(savedRoom, 1, 0)
    }

    @Cacheable(value = ["rooms"], key = "#roomId")
    suspend fun getRoomById(roomId: Long): ChatRoomResponse? {
        val room = chatRoomRepository.findById(roomId) ?: return null
        val memberCount = roomMembershipRepository.countByRoomId(roomId)

        return mapToChatRoomResponse(room, memberCount.toInt(), 0)
    }

    suspend fun getUserRooms(userId: Long): Flow<ChatRoomResponse> {
        return chatRoomRepository.findRoomsByUserId(userId)
            .map { room ->
                val memberCount = roomMembershipRepository.countByRoomId(room.id!!)
                val membership = roomMembershipRepository.findByRoomIdAndUserId(room.id, userId)

                val unreadCount = if (membership != null) {
                    messageRepository.countUnreadMessages(room.id, membership.lastReadAt).toInt()
                } else {
                    0
                }

                mapToChatRoomResponse(room, memberCount.toInt(), unreadCount)
            }
    }

    suspend fun getPublicRooms(): Flow<ChatRoomResponse> {
        return chatRoomRepository.findAllPublicRooms()
            .map { room ->
                val memberCount = roomMembershipRepository.countByRoomId(room.id!!)
                mapToChatRoomResponse(room, memberCount.toInt(), 0)
            }
    }

    @Transactional
    @CacheEvict(value = ["rooms"], key = "#roomId")
    suspend fun joinRoom(roomId: Long, userId: Long): Boolean {
        val room = chatRoomRepository.findById(roomId) ?: return false

        // 이미 방에 참여 중인지 확인
        val existingMembership = roomMembershipRepository.findByRoomIdAndUserId(roomId, userId)
        if (existingMembership != null) {
            return true
        }

        // 최대 사용자 수 체크
        if (room.maxUsers != null) {
            val currentCount = roomMembershipRepository.countByRoomId(roomId)
            if (currentCount >= room.maxUsers) {
                return false
            }
        }

        // 새 멤버십 생성
        val membership = RoomMembership(
            roomId = roomId,
            userId = userId,
            role = RoomMembership.MemberRole.MEMBER
        )
        roomMembershipRepository.save(membership)

        return true
    }

    @Transactional
    @CacheEvict(value = ["rooms"], key = "#roomId")
    suspend fun leaveRoom(roomId: Long, userId: Long): Boolean {
        val membership = roomMembershipRepository.findByRoomIdAndUserId(roomId, userId) ?: return false

        // 방의 마지막 멤버이거나 OWNER인 경우 처리 로직
        val memberCount = roomMembershipRepository.countByRoomId(roomId)
        if (memberCount <= 1 || membership.role == RoomMembership.MemberRole.OWNER) {
            if (memberCount <= 1) {
                // 마지막 멤버라면 방을 삭제
                chatRoomRepository.deleteById(roomId)
            } else if (membership.role == RoomMembership.MemberRole.OWNER) {
                // OWNER가 나가는 경우 다른 사람에게 권한 위임
                val allMemberships = roomMembershipRepository.findByRoomId(roomId).toList()

                // 그 다음 필터링 작업 수행
                val newOwnerMembership = allMemberships
                    .filter { it.userId != userId }
                    .firstOrNull()

                if (newOwnerMembership != null) {
                    val updatedMembership = newOwnerMembership.copy(
                        role = RoomMembership.MemberRole.OWNER
                    )
                    roomMembershipRepository.save(updatedMembership)
                }
                roomMembershipRepository.deleteById(membership.id!!)
            }
        } else {
            // 일반 멤버가 나가는 경우
            roomMembershipRepository.deleteById(membership.id!!)
        }

        return true
    }

    private fun mapToChatRoomResponse(chatRoom: ChatRoom, memberCount: Int, unreadCount: Int): ChatRoomResponse {
        return ChatRoomResponse(
            id = chatRoom.id!!,
            name = chatRoom.name,
            description = chatRoom.description,
            createdBy = chatRoom.createdBy,
            isPrivate = chatRoom.isPrivate,
            maxUsers = chatRoom.maxUsers,
            memberCount = memberCount,
            unreadCount = unreadCount,
            createdAt = chatRoom.createdAt,
            updatedAt = chatRoom.updatedAt
        )
    }
}