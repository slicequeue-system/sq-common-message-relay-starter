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
````

---

## ✅ 적용 방법

### 1. Gradle 의존성 설정

```groovy
dependencies {
    implementation("app.slicequeue:sq-common-message-relay-starter:0.0.3")
    implementation("app.slicequeue:sq-common-snowflake:0.0.1")
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
  core-pool-size: 16
  max-pool-size: 32
  queue-capacity: 200
  thread-name-prefix: relay-worker-
```

> Kafka 설정은 사용하는 쪽에서 반드시 구성해야 하며, Kafka 토픽은 사전에 생성되어 있어야 합니다.

---

### 3. JPA 연동 설정 (충돌 회피 필수)

> ✅ **starter를 사용하는 경우 반드시 아래 경로를 명시해야 합니다.**

```java
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.path.your_package...",                       // 사용자 Repository
    "app.slicequeue.common.base.messagerelay.domain"  // starter Repository
})
@EntityScan(basePackages = {
    "app.path.your_package...",                       // 사용자 Entity
    "app.slicequeue.common.base.messagerelay.domain"  // starter Entity
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

#### (2) 수동 생성 SQL (정확한 필드 구조)

```sql
CREATE TABLE outbox (
    outbox_id BIGINT NOT NULL,
    event_type_code VARCHAR(255),
    event_type_topic VARCHAR(255),
    payload TEXT,
    shard_key BIGINT,
    created_at TIMESTAMP(6),
    PRIMARY KEY (outbox_id)
);
```

> `event_type_code`와 `event_type_topic`은 EventType 인터페이스의 `getCode()`, `getTopic()`으로부터 분리되어 저장됩니다.

---

### 5. EventType 등록 및 구현

```java
@Configuration
public class MyEventTypeConfig {
    @Autowired
    public void registerTypes(EventTypeRegistry registry) {
        registry.register(UserEventType.USER_INFO_CHANGED);
    }
}

public enum UserEventType implements EventType {
    USER_INFO_CHANGED("user.info.changed", "user-topic", UserInfoChangedPayload.class);

    private final String code;
    private final String topic;
    private final Class<? extends EventPayload> payloadClass;

    UserEventType(String code, String topic, Class<? extends EventPayload> payloadClass) {
        this.code = code;
        this.topic = topic;
        this.payloadClass = payloadClass;
    }

    @Override public String getCode() { return code; }
    @Override public String getTopic() { return topic; }
    @Override public Class<? extends EventPayload> getPayloadClass() { return payloadClass; }
}
```

---

### 6. 이벤트 발행 예시

```java
eventPublisher.publish(
    UserEventType.USER_INFO_CHANGED,
    new UserInfoChangedPayload(123L, "닉네임"),
    userId
);
```

---

## 📦 구성 모듈 설명

| 구성 요소                        | 설명                                   |
| ---------------------------- | ------------------------------------ |
| `Outbox`, `OutboxRepository` | 트랜잭션 내 이벤트를 저장하는 JPA 엔티티             |
| `OutboxEventPublisher`       | 서비스 로직에서 직접 호출하는 발행 지점               |
| `MessageRelay`               | Kafka로 이벤트 발송 및 실패 자동 재시도            |
| `EventTypeRegistry`          | 타입별 Payload 매핑을 관리하는 중앙 레지스트리        |
| `MessageRelayAutoConfig`     | Kafka, Executor, Scheduling 자동 구성 등록 |

---

## ⚙️ 설정 안내: Artifactory 인증 및 Gradle 구성

이 라이브러리는 Slicequeue 내부 Artifactory를 통해 배포되고 있습니다.

### 1. 인증 정보 설정

```properties
ARTIFACTORY_USER=your-username
ARTIFACTORY_PASSWORD=your-password
```

> 계정은 Slicequeue 내부 시스템을 통해 발급되며, 관련 문의는 기술팀에 요청해주세요.

---

### 2. build.gradle에 repository 추가

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://af.slicequeue.app/artifactory/gradle-dev")
        credentials {
            username = findProperty("ARTIFACTORY_USER") as String
            password = findProperty("ARTIFACTORY_PASSWORD") as String
        }
    }
}
```

---

### 3. 전체 예시

```groovy
dependencies {
    implementation("app.slicequeue:sq-common-message-relay-starter:0.0.3")
    implementation("app.slicequeue:sq-common-snowflake:0.0.1")
}
```

---

## 📍 다음 단계 (예고)

* [ ] Flyway 기반의 outbox 테이블 자동 마이그레이션 SQL 제공
* [ ] Kafka 메시지 consumer 예제 연동 모듈 분리
* [ ] 운영환경을 고려한 dead-letter 및 retry 큐 확장 설계

---

> 실전 환경에서 Kafka 기반 이벤트 아키텍처가 필요한 경우,
> 이 starter 하나로 안정성과 확장성을 동시에 확보할 수 있도록 사용해가며 더 확장해 나갈 계획입니다.

---

## 🔍 참고

이 개발 내용은 인프런 [쿠케 "스프링부트로 직접 만들면서 배우는 대규모 시스템 설계 - 게시판"](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8%EB%A1%9C-%EB%8C%80%EA%B7%9C%EB%AA%A8-%EC%8B%9C%EC%8A%A4%ED%85%9C%EC%84%A4%EA%B3%84-%EA%B2%8C%EC%8B%9C%ED%8C%90) 강의를 참고하여 개발되었습니다.
