package app.slicequeue.common.base.messagerelay.core;

import app.slicequeue.common.base.messagerelay.MessageRelayConstants;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 2. MessageRelayCoordinator — Redis 인스턴스 관리 & 샤드 분배
 * - Redis ZSet 구조를 이용해 살아있는 애플리케이션을 주기적으로 ping
 * - 앱이 죽으면 일정 시간 지나면 자동으로 ZSet 제거
 * - 이 리스트 기반으로 샤드 분배가 이루어짐
 */
@Component
@RequiredArgsConstructor
public class MessageRelayCoordinator {
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.application.name}")
    private String applicationName;

    // 서버당 UUID 배정
    private final String APP_ID = UUID.randomUUID().toString();  // 이 값으로 구분

    private final int PING_INTERNAL_SECONDS = 3; // 3초에 한번씩
    private final int PING_FAILURE_THRESHOLD = 3; // 3번 실패하면 애플리케이션이 죽었다고 판단

    public AssignedShard assignedShards() { // 현재 살아있는 인스턴스 리스트 기준으로 appId → shard 를 분배
        return AssignedShard.of(APP_ID, findAppIds(), MessageRelayConstants.SHARD_COUNT);
    }

    private List<String> findAppIds() {
        return redisTemplate.opsForZSet().reverseRange(generateKey(), 0, -1).stream()
                .sorted()
                .toList();
    }

    @Scheduled(fixedDelay = PING_INTERNAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void ping() { // 현재 앱이 살아있다는 걸 Redis 기록합니다 (ZSet 현재 시간 기준으로 기록)
        redisTemplate.executePipelined((RedisCallback<?>) action -> {
            StringRedisConnection conn = (StringRedisConnection) action;
            String key = generateKey();
            conn.zAdd(key, Instant.now().toEpochMilli(), APP_ID); // 키값 기준으로 시간 값으로 Z 스코어 값 지정, 그리고 값은 APP_ID
            conn.zRemRangeByScore( // 점수 범위의 제거 진행
                    key,  // 대상 키값으로
                    Double.NEGATIVE_INFINITY, // 음수 끝 ~ 9초 이전 값
                    Instant.now().minusSeconds(PING_INTERNAL_SECONDS * PING_FAILURE_THRESHOLD).toEpochMilli());
            return null;
        });
    }

    @PreDestroy
    public void leave() {
        redisTemplate.opsForZSet().remove(generateKey(), APP_ID);
    }

    private String generateKey() {
        return "message-relay-coordinator::app-list::%s".formatted(applicationName);
    }
}
