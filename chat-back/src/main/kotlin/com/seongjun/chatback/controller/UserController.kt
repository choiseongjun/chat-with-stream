package com.seongjun.chatback.controller

import com.seongjun.chatback.dto.request.CreateUserRequest
import com.seongjun.chatback.dto.response.UserResponse
import com.seongjun.chatback.entity.User
import com.seongjun.chatback.service.UserService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@CrossOrigin
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createUser(@RequestBody request: CreateUserRequest): UserResponse {
        return userService.createUser(request)
    }

    @GetMapping("/{id}")
    suspend fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @GetMapping("/by-username/{username}")
    suspend fun getUserByUsername(@PathVariable username: String): ResponseEntity<UserResponse> {
        val user = userService.getUserByUsername(username)
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}/status")
    suspend fun updateUserStatus(
        @PathVariable id: Long,
        @RequestParam status: User.UserStatus
    ): ResponseEntity<UserResponse> {
        val user = userService.updateUserStatus(id, status)
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @GetMapping("/online")
    suspend fun getOnlineUsers(): Flow<UserResponse> {
        return userService.getOnlineUsers()
    }

    @GetMapping(path = ["/presence/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamUserPresence(): Flow<Any> {
        return userService.subscribeToUserPresence()
    }
}