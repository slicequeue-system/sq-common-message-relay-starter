package app.slicequeue.common.messagerelay.outbox;

import app.slicequeue.common.base.id_entity.BaseSnowflakeId;
import app.slicequeue.common.base.id_entity.SnowflakeFactory;
import app.slicequeue.common.snowflake.Snowflake;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxId extends BaseSnowflakeId<OutboxId> {

    static Snowflake snowflake = SnowflakeFactory.create();

    public OutboxId(Long id) {
        super(id);
    }

    @Override
    protected Snowflake getSnowflake() {
        return snowflake;
    }

    public static OutboxId generateId() {
        return new OutboxId().generate();
    }

    public static OutboxId from(Long idValue) {
        return from(idValue, OutboxId.class);
    }
}

