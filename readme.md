# Real-time Chat Application Backend

실시간 채팅 애플리케이션의 백엔드 서버입니다. Spring WebFlux와 Redis Pub/Sub을 활용하여 구현된 반응형 채팅 시스템입니다.

## 기술 스택

- **Language**: Kotlin
- **Framework**: Spring Boot (WebFlux)
- **Database**:
  - Primary: R2DBC (Reactive Relational Database)
  - Cache & Messaging: Redis
- **Build Tool**: Gradle

## 주요 기능

### 1. 실시간 채팅

- Redis Pub/Sub 기반의 실시간 메시지 전송
- 채팅방별 메시지 스트리밍
- 최근 메시지 캐싱 및 이전 메시지 조회
- 읽지 않은 메시지 카운트 기능

### 2. 채팅방 관리

- 공개/비공개 채팅방 생성
- 채팅방 참여/퇴장
- 채팅방 멤버 역할 관리 (OWNER, MEMBER)
- 최대 참여 인원 제한 기능

### 3. 사용자 관리

- 사용자 상태 관리 (온라인/오프라인)
- 실시간 사용자 presence 업데이트
- 사용자 정보 캐싱

## 아키텍처

### Redis 활용

1. **메시지 브로드캐스팅**

   - 전체 메시지 채널: 모든 메시지 발행
   - 룸별 채널: 특정 채팅방의 메시지만 발행

2. **메시지 캐싱**

   - 각 채팅방별 최근 메시지 저장
   - 설정된 개수만큼의 최근 메시지 유지

3. **사용자 Presence**
   - 사용자 온라인 상태 관리
   - 실시간 상태 업데이트 전파

### 반응형 프로그래밍

- Spring WebFlux를 활용한 비동기 처리
- Kotlin Coroutines 활용
- 반응형 데이터베이스 접근 (R2DBC)

## API 엔드포인트

### 메시지

- `POST /api/messages` - 새 메시지 전송
- `GET /api/messages/room/{roomId}` - 채팅방 메시지 조회
- `GET /api/messages/stream` - 전체 메시지 스트림
- `GET /api/messages/stream/room/{roomId}` - 채팅방별 메시지 스트림

### 채팅방

- `POST /api/rooms` - 새 채팅방 생성
- `GET /api/rooms/public` - 공개 채팅방 목록
- `GET /api/rooms/user` - 사용자 참여 채팅방 목록
- `POST /api/rooms/{roomId}/join` - 채팅방 참여

### 사용자

- `GET /api/users/presence/stream` - 사용자 상태 스트림
- `POST /api/users` - 새 사용자 등록
- `PUT /api/users/{id}/status` - 사용자 상태 업데이트

## 설정

### Redis 설정

```properties
app.redis.message-channel=chat:messages
app.redis.room-channel-prefix=chat:room:
app.redis.message-history-size=100
app.redis.user-presence-prefix=presence:user:
```

## 보안

- CORS 설정을 통한 허용된 오리진만 접근 가능
- 사용자 인증 필요 (JWT 기반)
