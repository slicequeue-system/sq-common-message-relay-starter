package app.slicequeue.common.base.messagerelay.publisher;

import app.slicequeue.common.base.messagerelay.MessageRelayConstants;
import app.slicequeue.common.base.messagerelay.outbox.Outbox;
import app.slicequeue.common.base.messagerelay.outbox.OutboxEvent;
import app.slicequeue.common.snowflake.Snowflake;
import app.slicequeue.event.domain.Event;
import app.slicequeue.event.domain.EventDescriptor;
import app.slicequeue.event.domain.EventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final Snowflake outboxIdSnowflake = new Snowflake();
    private final Snowflake eventIdSnowflake = new Snowflake();
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventDescriptor type, EventPayload payload, Long shardKey) {
        Outbox outbox = Outbox.create(
                type,
                Event.of(eventIdSnowflake.nextId(), type, payload).toJson(),
                shardKey % MessageRelayConstants.SHARD_COUNT);
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
    }
}
