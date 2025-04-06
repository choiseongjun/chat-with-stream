package com.seongjun.chatback.repository

import com.seongjun.chatback.entity.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, Long> {
    suspend fun findByUsername(username: String): User?
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE status = :status")
    fun findByStatus(status: User.UserStatus): Flow<User>
}