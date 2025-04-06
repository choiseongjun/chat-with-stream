package com.seongjun.chatback.service

import com.seongjun.chatback.dto.request.CreateUserRequest
import com.seongjun.chatback.dto.response.UserResponse
import com.seongjun.chatback.entity.User
import com.seongjun.chatback.redis.UserPresence
import com.seongjun.chatback.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val redisUserPresenceTemplate: ReactiveRedisTemplate<String, UserPresence>
) {
    private val logger = KotlinLogging.logger {}

    @Value("\${app.redis.user-presence-prefix}")
    private lateinit var userPresencePrefix: String

    suspend fun createUser(request: CreateUserRequest): UserResponse {
        // 실제 구현에서는 비밀번호 해싱 등 추가 작업 필요
        val existingUser = userRepository.findByUsername(request.username)
        if (existingUser != null) {
            throw IllegalArgumentException("이미 사용 중인 사용자 이름입니다.")
        }

        val user = User(
            username = request.username,
            email = request.email,
            status = User.UserStatus.OFFLINE
        )

        val savedUser = userRepository.save(user)
        return mapToUserResponse(savedUser)
    }

    @Cacheable(value = ["users"], key = "#id")
    suspend fun getUserById(id: Long): UserResponse? {
        val user = userRepository.findById(id) ?: return null
        return mapToUserResponse(user)
    }

    suspend fun getUserByUsername(username: String): UserResponse? {
        val user = userRepository.findByUsername(username) ?: return null
        return mapToUserResponse(user)
    }

    @CacheEvict(value = ["users"], key = "#id")
    suspend fun updateUserStatus(id: Long, status: User.UserStatus): UserResponse? {
        val user = userRepository.findById(id) ?: return null

        val updatedUser = user.copy(
            status = status,
            lastActiveAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(updatedUser)
        updateUserPresence(savedUser)

        return mapToUserResponse(savedUser)
    }

    suspend fun getOnlineUsers(): Flow<UserResponse> {
        return userRepository.findByStatus(User.UserStatus.ONLINE)
            .map { mapToUserResponse(it) }
    }

    private suspend fun updateUserPresence(user: User) {
        val presence = UserPresence(
            userId = user.id!!,
            username = user.username,
            status = user.status,
            lastActiveAt = user.lastActiveAt
        )

        val key = "$userPresencePrefix${user.id}"
        redisUserPresenceTemplate.opsForValue()
            .set(key, presence, Duration.ofHours(1))
            .awaitFirstOrNull()
    }

    suspend fun getUserPresence(userId: Long): UserPresence? {
        val key = "$userPresencePrefix$userId"
        return redisUserPresenceTemplate.opsForValue()
            .get(key)
            .awaitFirstOrNull()
    }

    fun subscribeToUserPresence(): Flow<UserPresence> {
        return redisUserPresenceTemplate.listenToPattern("$userPresencePrefix*")
            .map { it.message }
            .asFlow()
    }

    private fun mapToUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            status = user.status,
            lastActiveAt = user.lastActiveAt,
            createdAt = user.createdAt
        )
    }
}