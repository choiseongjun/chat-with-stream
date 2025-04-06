package com.seongjun.chatback.controller

import com.seongjun.chatback.dto.request.CreateMessageRequest
import com.seongjun.chatback.dto.response.MessageResponse
import com.seongjun.chatback.redis.RedisMessage
import com.seongjun.chatback.service.MessageService
import kotlinx.coroutines.flow.Flow
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.LocalDateTime

@CrossOrigin
@RestController
@RequestMapping("/api/messages")
class MessageController(private val messageService: MessageService) {

    @PostMapping
    suspend fun sendMessage(
        @RequestAttribute("userId") userId: Long,
        @RequestBody request: CreateMessageRequest
    ): MessageResponse {
        return messageService.sendMessage(userId, request)
    }

    @GetMapping("/room/{roomId}")
    suspend fun getMessagesByRoomId(
        @PathVariable roomId: Long,
        @RequestParam(defaultValue = "50") limit: Int
    ): Flow<Any> {
        return messageService.getMessagesByRoomId(roomId, limit)
    }

    @GetMapping("/room/{roomId}/before")
    suspend fun getMessagesBefore(
        @PathVariable roomId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) before: LocalDateTime,
        @RequestParam(defaultValue = "50") limit: Int
    ): Flow<MessageResponse> {
        return messageService.getMessagesBefore(roomId, before, limit)
    }

    @PostMapping("/room/{roomId}/read")
    suspend fun markAsRead(
        @PathVariable roomId: Long,
        @RequestAttribute("userId") userId: Long
    ) {
        messageService.markAsRead(roomId, userId)
    }

    @GetMapping(path = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAllMessages(): Flux<RedisMessage> {
        return messageService.subscribeToMessages()
    }

    @GetMapping(path = ["/stream/room/{roomId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamRoomMessages(@PathVariable roomId: Long): Flux<RedisMessage> {
        return messageService.subscribeToRoomMessages(roomId)
    }
}
