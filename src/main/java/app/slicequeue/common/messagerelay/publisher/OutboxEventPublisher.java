package app.slicequeue.common.messagerelay.publisher;

import app.slicequeue.common.messagerelay.MessageRelayConstants;
import app.slicequeue.common.messagerelay.outbox.Outbox;
import app.slicequeue.common.messagerelay.outbox.OutboxEvent;
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
