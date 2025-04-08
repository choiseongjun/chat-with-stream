# Chat Back Stress Test Project

이 프로젝트는 WebSocket 기반의 실시간 채팅 서버의 부하 테스트를 위한 애플리케이션입니다. Redis Pub/Sub을 활용한 분산 처리와 쿠버네티스 기반의 스케일 아웃을 지원합니다.

## 기술 스택

- **Backend**

  - Spring Boot WebFlux
  - WebSocket
  - Redis Pub/Sub
  - PostgreSQL
  - R2DBC
  - Project Reactor

- **Infrastructure**

  - Kubernetes
  - Docker
  - Redis
  - PostgreSQL

- **Monitoring**
  - Spring Boot Actuator
  - Prometheus
  - Micrometer

## 주요 기능

1. **WebSocket 실시간 채팅**

   - 채팅방 기반 메시지 전송
   - 입장/퇴장 이벤트 처리
   - 채팅 이력 조회

2. **분산 처리**

   - Redis Pub/Sub을 통한 메시지 브로드캐스팅
   - 다중 인스턴스 간 세션 공유
   - 스케일 아웃 지원

3. **모니터링**
   - 실시간 메트릭스 수집
   - 헬스 체크
   - 성능 모니터링

## 프로젝트 구조

```
chat-back-stress/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/seongjun/chatbackstress/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       ├── service/
│   │   │       └── utils/
│   │   └── resources/
│   └── test/
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── redis.yaml
│   └── postgres.yaml
└── build.gradle.kts
```

## 로컬 개발 환경 설정

1. **필수 요구사항**

```bash
# Java 17 이상
java -version

# Docker Desktop 설치
docker --version

# Kubernetes 활성화 (Docker Desktop)
kubectl version

# Redis 설치 (로컬 테스트용)
docker run --name redis -p 6379:6379 -d redis

# PostgreSQL 설치 (로컬 테스트용)
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

2. **프로젝트 빌드**

```bash
# 프로젝트 빌드
./gradlew build

# Docker 이미지 빌드
./gradlew bootBuildImage --imageName=chat-back-stress:latest
```

## 쿠버네티스 배포

1. **네임스페이스 생성**

```bash
kubectl create namespace chat-app
```

2. **PostgreSQL PVC 생성**

```bash
# PVC 생성을 위한 StorageClass가 필요합니다
kubectl apply -f k8s/postgres-pvc.yaml -n chat-app
```

3. **시크릿 및 설정 배포**

```bash
# PostgreSQL 비밀번호 시크릿 생성
kubectl apply -f k8s/postgres.yaml -n chat-app
```

4. **인프라 컴포넌트 배포**

```bash
# Redis 배포
kubectl apply -f k8s/redis.yaml -n chat-app

# PostgreSQL 배포
kubectl apply -f k8s/postgres.yaml -n chat-app
```

5. **애플리케이션 배포**

```bash
# 애플리케이션 Deployment 배포
kubectl apply -f k8s/deployment.yaml -n chat-app

# 서비스 배포
kubectl apply -f k8s/service.yaml -n chat-app
```

6. **배포 상태 확인**

```bash
# Pod 상태 확인
kubectl get pods -n chat-app

# 서비스 상태 확인
kubectl get svc -n chat-app
```

## 스케일링

1. **수동 스케일링**

```bash
# 레플리카 수 조정
kubectl scale deployment chat-back-stress --replicas=5 -n chat-app
```

2. **자동 스케일링**

```bash
# HPA 설정
kubectl autoscale deployment chat-back-stress --cpu-percent=70 --min=3 --max=10 -n chat-app

# HPA 상태 확인
kubectl get hpa -n chat-app
```

## 모니터링

1. **Actuator 엔드포인트**

```
# 헬스 체크
http://localhost:8080/actuator/health

# 메트릭스
http://localhost:8080/actuator/metrics

# Prometheus 메트릭스
http://localhost:8080/actuator/prometheus
```

2. **로그 확인**

```bash
# 애플리케이션 로그
kubectl logs -f deployment/chat-back-stress -n chat-app

# Redis 로그
kubectl logs -f deployment/redis -n chat-app

# PostgreSQL 로그
kubectl logs -f deployment/postgres -n chat-app
```

## 부하 테스트

1. **테스트 실행**

```bash
# 테스트 클래스 실행
./gradlew test --tests WebSocketLoadTest
```

2. **테스트 파라미터**

- 동시 접속자 수: 500
- 클라이언트당 메시지 수: 50
- 배치 크기: 25
- 배치 간 간격: 5초

## 문제 해결

1. **연결 문제**

- WebSocket 연결 실패 시 로그 확인
- 네트워크 정책 확인
- 서비스 DNS 확인

2. **성능 문제**

- 리소스 사용량 모니터링
- 로그 레벨 조정
- JVM 힙 크기 조정

## 주의사항

1. **리소스 관리**

- 적절한 리소스 제한 설정
- 모니터링 메트릭스 주기적 확인
- 로그 레벨 관리

2. **보안**

- 시크릿 관리
- 네트워크 정책 설정
- 접근 제어 설정
