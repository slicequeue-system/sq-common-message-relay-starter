package app.slicequeue.common.base.messagerelay.publisher;

import app.slicequeue.common.base.messagerelay.MessageRelayConstants;
import app.slicequeue.common.base.messagerelay.domain.Outbox;
import app.slicequeue.common.base.messagerelay.domain.OutboxEvent;
import app.slicequeue.common.base.messagerelay.event.Event;
import app.slicequeue.common.base.messagerelay.event.EventPayload;
import app.slicequeue.common.base.messagerelay.event.EventType;
import app.slicequeue.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final Snowflake outboxIdSnowflake = new Snowflake();
    private final Snowflake eventIdSnowflake = new Snowflake();
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventType type, EventPayload payload, Long shardKey) {
        Outbox outbox = Outbox.create(
                type,
                Event.of(eventIdSnowflake.nextId(), type, payload).toJson(),
                shardKey % MessageRelayConstants.SHARD_COUNT);
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
    }
}
