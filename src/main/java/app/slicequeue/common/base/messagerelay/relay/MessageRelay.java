package app.slicequeue.common.base.messagerelay.relay;

import app.slicequeue.common.base.messagerelay.outbox.Outbox;
import app.slicequeue.common.base.messagerelay.outbox.OutboxEvent;
import app.slicequeue.common.base.messagerelay.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 1. MessageRelay — 핵심 이벤트 처리자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRelay {
    private final OutboxRepository outboxRepository;
    private final MessageRelayCoordinator messageRelayCoordinator;  // 살아있는 애플리케이션이 떠있는지 확인할 수 있음
    private final KafkaTemplate<String, String> messageRelayKafkaTemplate; // MessageRelayConfig 에서 빈 등록한 messageRelayKafkaTemplate 주입하기 위함

    /**
     * 트랜잭션 커밋 전에 이벤트를 Outbox 테이블에 저장
     * @param outboxEvent 아웃박스 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createOutbox(OutboxEvent outboxEvent) {
        log.info("[MessageRelay.createOutbox] outboxEvent={}", outboxEvent);
        outboxRepository.save(outboxEvent.getOutbox());
    }

    /**
     * 트랜잭션이 커밋된 후에 Kafka 비동기로 메시지를 전송
     * @param outboxEvent 아웃박스 이벤트
     */
    @Async("messageRelayPublishEventExecutor") // MessageRelayConfig 에서 빈 등록한 messageRelayPublishEventExecutor 지정
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishEvent(OutboxEvent outboxEvent) {
        publishEvent(outboxEvent.getOutbox());
    }

    private void publishEvent(Outbox outbox) {
        try {
            messageRelayKafkaTemplate.send(
                    outbox.getEventTypeTopic(),
                    String.valueOf(outbox.getShardKey()),
                    outbox.getPayload()
            ).get(1, TimeUnit.SECONDS);
            outboxRepository.delete(outbox); // 전송 성공 시 DB 에서 해당 Outbox 레코드 삭제합니다.
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("[MessageRelay.publishEvent] outbox={}", outbox, e);
        }
    }

    @Scheduled(
            fixedDelay = 10,
            initialDelay = 5,
            timeUnit = TimeUnit.SECONDS,
            scheduler = "messageRelayPublishPendingEventExecutor"
    ) // 비동기 전송 실패했다면 일정 시간 지난 Outbox 데이터들을 주기적으로 스캔하여 전송 재시도
    public void publishPendingEvent() {
        // "AssignedShard"를 기준으로 자기 인스턴스가 담당해야 할 Shard 의 이벤트만 처리
        AssignedShard assignedShard = messageRelayCoordinator.assignedShards();
        log.info("[MessageRelay.publishEvent assignedShared size={}]", assignedShard.getShards().size());
        for (Long shard : assignedShard.getShards()) {
            List<Outbox> outboxes =
                    outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                            shard, Instant.now().minusSeconds(10), Pageable.ofSize(100));
            for (Outbox outbox : outboxes) {
                publishEvent(outbox);
            }
        }
    }

}
