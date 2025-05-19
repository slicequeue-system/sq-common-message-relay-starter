
# 📦 sq-common-message-relay-starter

Kafka 기반의 Outbox 이벤트 릴레이 모듈입니다.  
Outbox 패턴을 기반으로 트랜잭션 내에서 이벤트를 안전하게 저장하고, Kafka를 통해 비동기로 전달합니다.

---

## ✅ 사용 방법

### 1. Gradle 설정

```groovy
dependencies {
    implementation("com.yourgroup:sq-common-message-relay-starter:{version}")
}
```

---

### 2. application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

message-relay:
  core-pool-size: 16
  max-pool-size: 32
  queue-capacity: 200
  thread-name-prefix: relay-worker-
```

---

### 3. Outbox 테이블 자동 생성

JPA 설정이 아래와 같으면 `outbox` 테이블이 자동으로 생성됩니다.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

---

### 🛠️ 또는 직접 SQL로 생성할 경우

```sql
CREATE TABLE outbox (
    outbox_id BIGINT NOT NULL,
    event_type VARCHAR(255),
    payload TEXT,
    shard_key BIGINT,
    created_at TIMESTAMP(6),
    PRIMARY KEY (outbox_id)
);
```

---

### 4. EventType 등록

```java
@Configuration
public class MyEventTypeConfig {
    @Autowired
    public void registerTypes(EventTypeRegistry registry) {
        registry.register(UserEventType.USER_INFO_CHANGED);
    }
}
```

---

### 5. 이벤트 발행 예시

```java
eventPublisher.publish(
    UserEventType.USER_INFO_CHANGED,
    new UserInfoChangedPayload(123L, "tester"),
    userId
);
```

---

## ✅ 구성 모듈

- `Outbox`, `OutboxEvent` : 이벤트 영속화 도메인
- `EventTypeRegistry` : 이벤트 타입/페이로드 등록소
- `MessageRelay` : Kafka 전송 및 실패 재시도
- `MessageRelayAutoConfig` : Kafka/Executor 자동 설정
