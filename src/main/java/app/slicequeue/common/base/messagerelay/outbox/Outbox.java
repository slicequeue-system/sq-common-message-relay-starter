package app.slicequeue.common.base.messagerelay.outbox;

import app.slicequeue.event.domain.EventDescriptor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Table(name = "outbox")
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class



Outbox {

    @EmbeddedId
    @AttributeOverride(name = "id", column = @Column(name = "outbox_id"))
    private OutboxId outboxId;
    private String eventTypeCode;
    private String eventTypeTopic;
    private String payload;
    private Long shardKey;
    private Instant createdAt;

    public static Outbox create(EventDescriptor eventType, String payload, Long shardKey) {
        Outbox outbox = new Outbox();
        outbox.outboxId = OutboxId.generateId();
        outbox.eventTypeCode = eventType.getCode();
        outbox.eventTypeTopic = eventType.getTopic();
        outbox.payload = payload;
        outbox.shardKey = shardKey;
        outbox.createdAt = Instant.now();
        return outbox;
    }
}
