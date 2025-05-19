
# 📦 sq-common-message-relay-starter

**Kafka 기반 Outbox 이벤트 릴레이 모듈**  
트랜잭션 로그를 안전하게 Kafka로 발행하기 위한 Outbox 패턴 기반의 메시지 릴레이 구성입니다.  
Spring Boot 3.1+ / Hibernate 6 환경에 최적화되어 있으며, Kafka 메시지 브로커 기반의 **확장 가능하고 신뢰성 높은 비동기 아키텍처**를 구현할 수 있습니다.

---

## 📌 주요 특징

- ✅ **Outbox Pattern 기반**으로 트랜잭션 내 안전한 이벤트 저장
- ✅ Kafka를 통한 **비동기 메시지 릴레이** 및 자동 재시도
- ✅ Spring Scheduling, Async Executor 포함한 **즉시 사용 가능한 구성**
- ✅ `EventTypeRegistry`를 통한 **동적 타입 매핑 확장성**
- ✅ Snowflake 기반 고유 ID 생성 지원 (`sq-common-snowflake`)

---

## ⚙️ 메시지 릴레이 아키텍처 요약

```text
[ 서비스 로직 ]
     ↓
[ Outbox 테이블 INSERT (Tx 보장) ]
     ↓ (비동기)
[ Kafka 전송 ]
     ↓
[ 전송 성공 시 삭제 or 무시 ]
```

---

## ✅ 적용 방법

### 1. Gradle 의존성 설정

```groovy
dependencies {
    implementation("app.slicequeue:sq-common-message-relay-starter:0.0.3")
    implementation("app.slicequeue:sq-common-snowflake:0.0.1") // ID 생성기 (필수)
}
```

---

### 2. application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: your-group-id
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: false

message-relay:
  core-pool-size: 16       # 메시지 전송 executor core 쓰레드 수
  max-pool-size: 32        # 최대 쓰레드 수 (burst 대응)
  queue-capacity: 200      # 큐 용량 (작업 적체 허용량)
  thread-name-prefix: relay-worker- # 쓰레드 이름 prefix (디버깅용)
```

> 📌 Kafka 설정은 사용하는 쪽에서 반드시 지정해야 하며, 위 설정은 기본적인 consumer 구성 예시입니다.

---

### 3. JPA 연동 설정 (충돌 회피 필수)

> Outbox 테이블과 Repository는 별도 모듈에 정의되어 있으므로,  
> 사용하는 프로젝트에서 **JPA Repository 스캔 충돌을 방지하기 위해 반드시 명시적 경로 지정**이 필요합니다.

```java
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.path.your_package...",               // 사용하는 쪽 리포지토리 스캔 정보
    "app.slicequeue.common.base.messagerelay.domain"          // starter Repository
})
@EntityScan(basePackages = {
    "app.path.your_package...",              // 사용하는 쪽 엔티티 스캔경로
    "app.slicequeue.common.base.messagerelay.domain"           // starter Entity
})
public class JpaConfig {}
```

---

### 4. Outbox 테이블 생성

#### (1) 자동 생성

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

#### (2) 수동 생성 SQL

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

### 5. EventType 등록

```java
@Configuration
public class MyEventTypeConfig {
    @Autowired
    public void registerTypes(EventTypeRegistry registry) {
        registry.register(UserEventType.USER_INFO_CHANGED);
    }
}
```

> `EventType`은 enum 또는 class 형태로 정의되며, Kafka 토픽명과 payload 매핑 정보를 포함합니다.

---

### 6. 이벤트 발행 예시

```java
eventPublisher.publish(
    UserEventType.USER_INFO_CHANGED,
    new UserInfoChangedPayload(123L, "닉네임"),
    userId
);
```

> 내부적으로 Outbox 테이블에 저장되고, 트랜잭션 커밋 이후 Kafka로 비동기 전송됩니다.

---

## 📦 구성 모듈 설명

| 구성 요소 | 설명 |
|-----------|------|
| `Outbox`, `OutboxRepository` | 트랜잭션 내 이벤트를 저장하는 JPA 엔티티 |
| `OutboxEventPublisher` | 서비스 로직에서 직접 호출하는 발행 지점 |
| `MessageRelay` | Kafka로 이벤트 발송 및 실패 재시도 로직 |
| `EventTypeRegistry` | 타입별 Payload 매핑을 관리하는 중앙 레지스트리 |
| `MessageRelayAutoConfig` | Kafka, Executor, Scheduling 자동 구성 등록 |

---

## 📍 다음 단계 (예고)

향후 버전에서는 다음 기능을 지원할 예정입니다:

- [ ] Flyway 기반의 outbox 테이블 자동 마이그레이션 SQL 제공
- [ ] Kafka 메시지 consumer 예제 연동 모듈 분리
- [ ] 운영환경을 고려한 dead-letter 및 retry 큐 확장 설계

---

> 실전 환경에서 Kafka 기반 이벤트 아키텍처가 필요한 경우,  
> 이 starter 하나로 안정성과 확장성을 동시에 확보할 수 있도록 사용해가며 더 확장해 나갈 계획
