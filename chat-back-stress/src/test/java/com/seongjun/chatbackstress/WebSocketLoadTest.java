package com.seongjun.chatbackstress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seongjun.chatbackstress.dto.WebSocketMessageDto;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketLoadTest {
    private static final Logger log = LoggerFactory.getLogger(WebSocketLoadTest.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    // 시스템 리소스 제한을 고려하여 설정 조정
    private final ConnectionProvider provider = ConnectionProvider.builder("custom")
            .maxConnections(500) // 최대 연결 수 감소
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .build();
            
    private final LoopResources loopResources = LoopResources.create("custom", 4, true);
    
    private final WebSocketClient client = new ReactorNettyWebSocketClient(
            HttpClient.create(provider)
                    .responseTimeout(Duration.ofSeconds(30))
                    .runOn(loopResources)
    );
    
    private final String WS_URL = "ws://localhost:8080/ws/chat";
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    private final AtomicInteger connectedClients = new AtomicInteger(0);

    @Test
    public void loadTest() throws InterruptedException {
        int numberOfClients = 500; // 전체 클라이언트 수 감소
        int messagesPerClient = 50; // 클라이언트당 메시지 수 감소
        int batchSize = 25; // 배치 크기 감소
        CountDownLatch latch = new CountDownLatch(numberOfClients);

        // 클라이언트를 배치로 나누어 연결
        for (int batch = 0; batch < numberOfClients; batch += batchSize) {
            int currentBatchSize = Math.min(batchSize, numberOfClients - batch);
            List<Mono<Void>> batchConnections = new ArrayList<>();

            for (int i = 0; i < currentBatchSize; i++) {
                final int clientId = batch + i;
                Mono<Void> connection = client.execute(
                    URI.create(WS_URL),
                    session -> {
                        connectedClients.incrementAndGet();
                        log.info("Client {} connected. Total connected: {}", clientId, connectedClients.get());

                        // 입장 메시지 전송
                        WebSocketMessageDto enterMessage = WebSocketMessageDto.builder()
                                .type("ENTER")
                                .roomId("loadtest-room")
                                .sender("client-" + clientId)
                                .timestamp(LocalDateTime.now())
                                .build();

                        // 채팅 메시지 스트림 생성
                        Flux<WebSocketMessage> messageFlux = Flux.range(0, messagesPerClient)
                                .delayElements(Duration.ofMillis(200)) // 메시지 간격 증가
                                .map(seq -> {
                                    try {
                                        WebSocketMessageDto message = WebSocketMessageDto.builder()
                                                .type("CHAT")
                                                .roomId("loadtest-room")
                                                .sender("client-" + clientId)
                                                .message("Test message " + seq + " from client " + clientId)
                                                .timestamp(LocalDateTime.now())
                                                .build();
                                        return session.textMessage(objectMapper.writeValueAsString(message));
                                    } catch (Exception e) {
                                        log.error("Error creating message for client {}: {}", clientId, e.getMessage());
                                        return null;
                                    }
                                })
                                .filter(msg -> msg != null)
                                .onErrorResume(e -> {
                                    log.error("Error in message flux for client {}: {}", clientId, e.getMessage());
                                    return Flux.empty();
                                });

                        // 서버로부터 메시지 수신
                        Mono<Void> receive = session.receive()
                                .doOnNext(message -> {
                                    messageCounter.incrementAndGet();
                                    if (messageCounter.get() % 100 == 0) {
                                        log.info("Received {} messages", messageCounter.get());
                                    }
                                })
                                .onErrorResume(e -> {
                                    log.error("Error receiving message for client {}: {}", clientId, e.getMessage());
                                    return Mono.empty();
                                })
                                .then();

                        try {
                            return session.send(Mono.just(session.textMessage(objectMapper.writeValueAsString(enterMessage)))
                                    .concatWith(messageFlux))
                                    .then(receive)
                                    .doFinally(signalType -> {
                                        try {
                                            connectedClients.decrementAndGet();
                                            latch.countDown();
                                            log.info("Client {} disconnected. Remaining connected: {}", 
                                                clientId, connectedClients.get());
                                        } catch (Exception e) {
                                            log.error("Error in doFinally for client {}: {}", clientId, e.getMessage());
                                        }
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Error in client {}: {}", clientId, e.getMessage());
                                        latch.countDown();
                                        return Mono.empty();
                                    });
                        } catch (Exception e) {
                            log.error("Error in client {}: {}", clientId, e.getMessage());
                            latch.countDown();
                            return Mono.empty();
                        }
                    }
                )
                .timeout(Duration.ofSeconds(30))
                .retry(3)
                .onErrorResume(e -> {
                    log.error("Connection error for client {}: {}", clientId, e.getMessage());
                    latch.countDown();
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.boundedElastic());

                batchConnections.add(connection);
            }

            // 현재 배치의 연결 시작
            Flux.fromIterable(batchConnections)
                .delayElements(Duration.ofMillis(200)) // 연결 간 지연 증가
                .flatMap(conn -> conn)
                .subscribe();

            // 배치 간 지연 증가
            Thread.sleep(5000);
        }

        // 테스트 완료 대기
        latch.await();
        
        // 리소스 정리 전에 모든 연결이 완전히 종료되도록 대기
        Thread.sleep(5000);
        
        provider.dispose();
        loopResources.dispose();
        
        log.info("Load test completed. Total messages processed: {}", messageCounter.get());
    }
} 