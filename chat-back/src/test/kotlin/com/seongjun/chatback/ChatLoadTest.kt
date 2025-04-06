package com.seongjun.chatback

import com.seongjun.chatback.dto.request.CreateMessageRequest
import com.seongjun.chatback.dto.request.CreateRoomRequest
import com.seongjun.chatback.entity.User
import com.seongjun.chatback.repository.MessageRepository
import com.seongjun.chatback.repository.UserRepository
import com.seongjun.chatback.service.ChatRoomService
import com.seongjun.chatback.service.MessageService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import kotlin.math.roundToInt
import kotlin.properties.Delegates

@SpringBootTest
class ChatRoomLoadTest @Autowired constructor(
    private val chatRoomService: ChatRoomService,
    private val messageService: MessageService,
    private val userRepository:UserRepository,
    private val messageRepository:MessageRepository
) {

    private var testRoomId =1L
    private val testUserCount = 5 // 동시 접속 사용자 수
    private val messagePerUserCount = 200 // 사용자당 메시지 수

//    @BeforeEach
//    fun setUp() = runBlocking {
//        // 테스트용 채팅방 생성 (필요한 경우)
//        val room = chatRoomService.createRoom(
//            CreateRoomRequest(
//                name = "대규모 부하 테스트 채팅방",
//                description = "성능 테스트를 위한 채팅방",
//                isPrivate = false,
//                maxUsers = testUserCount + 10,
//                userId = 0
//            )
//        )
//        testRoomId = room.id!!
//    }

    @Test
    fun `대량 사용자 채팅방 동시 참여 테스트`() = runBlocking {
        val startTime = System.currentTimeMillis()

//        val users = (1..testUserCount).map { userId ->
//            userRepository.save(
//                User(
//                    username = "testUser$userId",
//                    email = "testuser$userId@example.com",
//                )
//            )
//        }

        coroutineScope {
            val joinJobs = (1..testUserCount).map { userId ->
                async {
                    val result = chatRoomService.joinRoom(testRoomId, userId.toLong())
                    assertThat(result).isTrue()
                }
            }

            // 모든 참여 작업 완료 대기
            joinJobs.forEach { it.await() }
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        println("총 사용자 수: $testUserCount")
        println("총 소요 시간: $totalTime ms")
        println("초당 참여 사용자 수: ${(testUserCount * 1000.0 / totalTime).roundToInt()} users/sec")
    }

    @Test
    fun `대량 사용자 채팅방 참여 및 메시지 발송 통합 테스트`() = runBlocking {
        val startTime = System.currentTimeMillis()

        coroutineScope {
            // 사용자 참여 (변경 없음)
            val joinJobs = (1..testUserCount).map { userId ->
                async {
                    val joinResult = chatRoomService.joinRoom(testRoomId, userId.toLong())
                    assertThat(joinResult).isTrue()
                }
            }
            joinJobs.forEach { it.await() }

            // 메시지 발송
            val messageJobs = (1..testUserCount).map { userId ->
                async {
                    repeat(messagePerUserCount) { messageIndex ->
                        val request = CreateMessageRequest(
                            roomId = testRoomId,
                            content = "부하 테스트 메시지 - 사용자 $userId, 메시지 $messageIndex",
                            type = "TEXT"
                        )

                        val response = messageService.sendMessage(userId.toLong(), request)
                        assertThat(response).isNotNull()
                    }
                }
            }

            messageJobs.forEach { it.await() }
        }

        // 비동기 작업이 완료될 시간을 주기 위한 지연
        delay(2000) // 2초 대기

        // 검증 추가: 데이터베이스에 저장된 메시지 수 확인
//        val savedMessages = messageRepository.findByRoomId(testRoomId)
//        println("저장된 메시지 수: ${savedMessages.size}")
//        assertThat(savedMessages.size).isEqualTo(testUserCount * messagePerUserCount)

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val totalMessages = testUserCount * messagePerUserCount

        println("총 사용자 수: $testUserCount")
        println("총 메시지 수: $totalMessages")
        println("총 소요 시간: $totalTime ms")
        println("초당 처리 사용자 수: ${(testUserCount * 1000.0 / totalTime).roundToInt()} users/sec")
        println("초당 처리 메시지 수: ${(totalMessages * 1000.0 / totalTime).roundToInt()} msg/sec")
    }
}