package com.seongjun.chatback.service

import com.seongjun.chatback.dto.request.CreateMessageRequest
import com.seongjun.chatback.dto.response.MessageResponse
import com.seongjun.chatback.entity.Message
import com.seongjun.chatback.redis.RedisMessage
import com.seongjun.chatback.repository.MessageRepository
import com.seongjun.chatback.repository.RoomMembershipRepository
import com.seongjun.chatback.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CompletableFuture

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val roomMembershipRepository: RoomMembershipRepository,
    private val redisMessageTemplate: ReactiveRedisTemplate<String, RedisMessage>,
    private val coroutineScope: CoroutineScope // 코루틴 스코프 주입

) {
    private val logger = KotlinLogging.logger {}

    // 메시지 실시간 브로드캐스팅을 위한 Sink 객체
    private val messageSink = Sinks.many().multicast().onBackpressureBuffer<RedisMessage>()
    private val messageFlux = messageSink.asFlux()

    @Value("\${app.redis.message-channel}")
    private lateinit var messageChannel: String

    @Value("\${app.redis.room-channel-prefix}")
    private lateinit var roomChannelPrefix: String

    @Value("\${app.redis.message-history-size}")
    private var messageHistorySize: Int = 100

//    @Transactional
//    suspend fun sendMessage(userId: Long, request: CreateMessageRequest): MessageResponse {
//        // 사용자가 채팅방에 속해있는지 확인
//        val membership = roomMembershipRepository.findByRoomIdAndUserId(request.roomId, userId)
//        if (membership == null) {
//            throw IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.")
//        }
//
//        // 메시지 타입 결정
//        val messageType = try {
//            Message.MessageType.valueOf(request.type)
//        } catch (e: IllegalArgumentException) {
//            Message.MessageType.TEXT
//        }
//
//        // 메시지 저장
//        val message = Message(
//            roomId = request.roomId,
//            senderId = userId,
//            content = request.content,
//            type = messageType
//        )
//
//        val savedMessage = messageRepository.save(message)
//
//        // 메시지를 보낸 사용자 정보 가져오기
//        val sender = userRepository.findById(userId)
//            ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")
//
//        // Redis를 통해 메시지 브로드캐스팅
//        val redisMessage = RedisMessage(
//            id = savedMessage.id!!,
//            roomId = savedMessage.roomId,
//            senderId = savedMessage.senderId,
//            senderName = sender.username,
//            content = savedMessage.content,
//            type = savedMessage.type,
//            createdAt = System.currentTimeMillis() // 현재 시간의 밀리초 타임스탬프
//        )
//
//        // 전체 메시지 채널로 발행
//        redisMessageTemplate.convertAndSend(messageChannel, redisMessage).awaitFirstOrNull()
//
//        // 특정 채팅방 채널로 발행
//        val roomChannel = "$roomChannelPrefix${savedMessage.roomId}"
//        redisMessageTemplate.convertAndSend(roomChannel, redisMessage).awaitFirstOrNull()
//
//        // 리스트에 최근 메시지 추가 (각 방마다 N개의 최근 메시지 유지)
//        val messageListKey = "chat:room:${savedMessage.roomId}:messages"
//        redisMessageTemplate.opsForList()
//            .leftPush(messageListKey, redisMessage)
//            .awaitFirst()
//
//        // 최근 N개만 유지하도록 트리밍
//        redisMessageTemplate.opsForList()
//            .trim(messageListKey, 0, messageHistorySize - 1L)
//            .awaitFirstOrNull()
//
//        // 로컬 Sink에도 발행
//        messageSink.tryEmitNext(redisMessage)
//
//        return MessageResponse(
//            id = savedMessage.id,
//            roomId = savedMessage.roomId,
//            senderId = savedMessage.senderId,
//            senderName = sender.username,
//            content = savedMessage.content,
//            type = savedMessage.type,
//            createdAt = LocalDateTime.now()
//        )
//    }
suspend fun sendMessage(userId: Long, request: CreateMessageRequest): MessageResponse {
    // 사용자가 채팅방에 속해있는지 확인
    val membership = roomMembershipRepository.findByRoomIdAndUserId(request.roomId, userId)
        ?: throw IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다.")

    // 메시지 타입 결정
    val messageType = try {
        Message.MessageType.valueOf(request.type)
    } catch (e: IllegalArgumentException) {
        Message.MessageType.TEXT
    }

    // 메시지 정보 준비
    val sender = userRepository.findById(userId)
        ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")

    // Redis 메시지 준비
    val redisMessage = RedisMessage(
        roomId = request.roomId,
        senderId = userId,
        senderName = sender.username,
        content = request.content,
        type = messageType,
        createdAt = System.currentTimeMillis()
    )
    val message = Message(
        roomId = request.roomId,
        senderId = userId,
        content = request.content,
        type = messageType,
        createdAt = LocalDateTime.now()
    )

    // 코루틴 스코프에서 비동기 작업 실행
    coroutineScope.launch {
        // Redis 발행
        redisMessageTemplate.convertAndSend(messageChannel, redisMessage)

        // 특정 채팅방 채널로 발행
        val roomChannel = "$roomChannelPrefix${request.roomId}"
        redisMessageTemplate.convertAndSend(roomChannel, redisMessage)

        // 리스트에 최근 메시지 추가
        val messageListKey = "chat:room:${request.roomId}:messages"
        redisMessageTemplate.opsForList()
            .leftPush(messageListKey, redisMessage)
            .awaitFirstOrNull()

        // 최근 N개만 유지하도록 트리밍
        redisMessageTemplate.opsForList()
            .trim(messageListKey, 0, messageHistorySize - 1L)
            .awaitFirstOrNull()
    }
    messageRepository.save(message)


    // 로컬 Sink에 발행
    messageSink.tryEmitNext(redisMessage)

    // 즉시 응답
    return MessageResponse(
        id = redisMessage.id,
        roomId = request.roomId,
        senderId = userId,
        senderName = sender.username,
        content = request.content,
        type = messageType,
        createdAt = LocalDateTime.now()
    )
}

    suspend fun getMessagesByRoomId(roomId: Long, limit: Int = 50): Flow<Any> {
        // 먼저 Redis에서 최근 메시지를 가져오려고 시도
        val messageListKey = "chat:room:$roomId:messages"
        val cachedMessages = redisMessageTemplate.opsForList()
            .range(messageListKey, 0, limit - 1L)
            .collectList()
            .awaitFirstOrNull()

        if (cachedMessages != null && cachedMessages.isNotEmpty()) {
            // Redis에 캐시된 메시지가 있으면 사용
            return Flux.fromIterable(cachedMessages).asFlow()
        }

        // Redis에 캐시된 메시지가 없으면 DB에서 가져오기
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
            .map { message ->
                val sender = userRepository.findById(message.senderId)
                MessageResponse(
                    id = message.id!!,
                    roomId = message.roomId,
                    senderId = message.senderId,
                    senderName = sender?.username ?: "알 수 없는 사용자",
                    content = message.content,
                    type = message.type,
                    createdAt = message.createdAt
                )
            }
    }

    suspend fun getMessagesBefore(roomId: Long, before: LocalDateTime, limit: Int = 50): Flow<MessageResponse> {
        return messageRepository.findMessagesBefore(roomId, before, limit)
            .map { message ->
                val sender = userRepository.findById(message.senderId)
                MessageResponse(
                    id = message.id!!,
                    roomId = message.roomId,
                    senderId = message.senderId,
                    senderName = sender?.username ?: "알 수 없는 사용자",
                    content = message.content,
                    type = message.type,
                    createdAt = message.createdAt
                )
            }
    }

    suspend fun markAsRead(roomId: Long, userId: Long) {
        roomMembershipRepository.updateLastReadAt(roomId, userId)
    }

    fun subscribeToMessages(): Flux<RedisMessage> {
        return messageFlux
    }

    fun subscribeToRoomMessages(roomId: Long): Flux<RedisMessage> {
        val roomChannel = "$roomChannelPrefix$roomId"
        val topic: Topic = ChannelTopic(roomChannel)
        return redisMessageTemplate.listenTo(topic)
            .map { it.message }
    }

}