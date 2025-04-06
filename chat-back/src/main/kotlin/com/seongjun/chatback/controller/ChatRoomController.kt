package com.seongjun.chatback.controller

import com.seongjun.chatback.dto.request.CreateRoomRequest
import com.seongjun.chatback.dto.response.ChatRoomResponse
import com.seongjun.chatback.service.ChatRoomService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/api/rooms")
class ChatRoomController (private val chatRoomService: ChatRoomService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createRoom(
//        @RequestParam("userId") userId: Long,
        @RequestBody request: CreateRoomRequest
    ): ChatRoomResponse {
        return chatRoomService.createRoom(request.userId, request)
    }

    @GetMapping("/{id}")
    suspend fun getRoomById(@PathVariable id: Long): ResponseEntity<ChatRoomResponse> {
        val room = chatRoomService.getRoomById(id)
        return if (room != null) ResponseEntity.ok(room) else ResponseEntity.notFound().build()
    }

    @GetMapping("/user")
    suspend fun getUserRooms(@RequestAttribute("userId") userId: Long): Flow<ChatRoomResponse> {
        return chatRoomService.getUserRooms(userId)
    }

    @GetMapping("/public")
    suspend fun getPublicRooms(): Flow<ChatRoomResponse> {
        return chatRoomService.getPublicRooms()
    }

    @PostMapping("/{roomId}/join")
    suspend fun joinRoom(
        @PathVariable roomId: Long,
        @RequestAttribute("userId") userId: Long
    ): ResponseEntity<Void> {
        val success = chatRoomService.joinRoom(roomId, userId)
        return if (success) ResponseEntity.ok().build() else ResponseEntity.badRequest().build()
    }

    @PostMapping("/{roomId}/leave")
    suspend fun leaveRoom(
        @PathVariable roomId: Long,
        @RequestAttribute("userId") userId: Long
    ): ResponseEntity<Void> {
        val success = chatRoomService.leaveRoom(roomId, userId)
        return if (success) ResponseEntity.ok().build() else ResponseEntity.badRequest().build()
    }
}