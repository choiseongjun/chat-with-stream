package com.seongjun.chatbackstress.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seongjun.chatbackstress.dto.WebSocketMessageDto;
import com.seongjun.chatbackstress.service.ChatService;
import com.seongjun.chatbackstress.service.RedisPubSubService;
import com.seongjun.chatbackstress.utils.ChatSessionManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ObjectMapper objectMapper;
    private final RedisPubSubService redisPubSubService;
    private final ChatService chatService;
    private final ChatSessionManager sessionManager;

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 단일 구독자를 위한 Sink 생성
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        // 세션 등록 (처음에는 세션 ID로만 등록)
        sessionManager.addSession(session.getId(), session);
        redisPubSubService.registerSink(session.getId(), sink);

        // 연결 상태 모니터링
        session.closeStatus()
            .doOnNext(status -> log.info("WebSocket closed with status: {}", status))
            .subscribe();

        // 클라이언트로부터 메시지 수신
        Mono<Void> input = session.receive()
                .doOnSubscribe(sub -> log.info("WebSocket connection established for session: {}", session.getId()))
                .doOnNext(message -> {
                    try {
                        String payload = message.getPayloadAsText();
                        WebSocketMessageDto dto = objectMapper.readValue(payload, WebSocketMessageDto.class);
                        
                        // 첫 메시지일 때만 방에 세션 등록
                        if (!sessionManager.isSessionInRoom(dto.getRoomId(), session)) {
                            sessionManager.addSession(dto.getRoomId(), session);
                            
                            // 방 입장 시 이전 메시지 조회
                            if ("ENTER".equals(dto.getType())) {
                                fetchChatHistory(dto.getRoomId(), sink);
                            }
                        }

                        if ("CHAT".equals(dto.getType())) {
                            // 메시지 저장 후 발행
                            chatService.save(dto)
                                .doOnSuccess(v -> redisPubSubService.publishMessage(dto))
                                .subscribe();
                        }
                    } catch (Exception e) {
                        log.error("Error processing message: ", e);
                        sink.tryEmitError(e);
                    }
                })
                .doOnComplete(() -> {
                    log.info("WebSocket connection completed for session: {}", session.getId());
                    cleanup(session);
                })
                .doOnError(error -> {
                    log.error("WebSocket error occurred: ", error);
                    cleanup(session);
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .doBeforeRetry(signal -> log.warn("Retrying connection after error: {}", signal.failure().getMessage()))
                )
                .then();

        // 클라이언트로 메시지 송신
        Mono<Void> output = session.send(
                sink.asFlux()
                    .map(session::textMessage)
                    .doOnError(error -> log.error("Error sending message: ", error))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("Retrying message send after error: {}", signal.failure().getMessage()))
                    )
        );

        // 입력과 출력 스트림 결합
        return Mono.zip(input, output)
                .then()
                .doFinally(signalType -> cleanup(session))
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.error("WebSocket connection error: ", e);
                    return Mono.empty();
                });
    }

    private void fetchChatHistory(String roomId, Sinks.Many<String> sink) {
        chatService.getMessages(roomId)
            .subscribeOn(Schedulers.boundedElastic())
            .map(message -> {
                try {
                    WebSocketMessageDto historyDto = WebSocketMessageDto.builder()
                            .type("HISTORY")
                            .roomId(message.getRoomId())
                            .sender(message.getSender())
                            .message(message.getMessage())
                            .timestamp(message.getTimestamp())
                            .build();
                    return objectMapper.writeValueAsString(historyDto);
                } catch (Exception e) {
                    log.error("Error serializing history message: {}", e.getMessage());
                    return null;
                }
            })
            .filter(json -> json != null)
            .subscribe(
                json -> sink.tryEmitNext(json),
                error -> log.error("Error fetching chat history: {}", error.getMessage())
            );
    }

    private void cleanup(WebSocketSession session) {
        sessionManager.removeSession(session.getId());
        redisPubSubService.unregisterSink(session.getId());
    }
}
