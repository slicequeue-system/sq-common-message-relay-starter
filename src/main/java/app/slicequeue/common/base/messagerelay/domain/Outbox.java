package app.slicequeue.common.base.messagerelay.domain;

import app.slicequeue.common.base.messagerelay.event.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Table(name = "outbox")
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    @EmbeddedId
    @AttributeOverride(name = "id", column = @Column(name = "outbox_id"))
    private OutboxId outboxId;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    private String payload;
    private Long shardKey;
    private Instant createdAt;

    public static Outbox create(EventType eventType, String payload, Long shardKey) {
        Outbox outbox = new Outbox();
        outbox.outboxId = OutboxId.generateId();
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.shardKey = shardKey;
        outbox.createdAt = Instant.now();
        return outbox;
    }
}
