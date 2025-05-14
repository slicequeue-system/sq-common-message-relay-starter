package app.slicequeue.common.base.messagerelay.event;

import app.slicequeue.sq_user.common.util.DataSerializer;
import lombok.Getter;

/**
 * 이벤트 통신을 위한 클래스
 */
@Getter
public class Event <T extends EventPayload>{
    private Long eventId;
    private EventType type;
    private T payload;

    public static Event<EventPayload> of(Long eventId, EventType type, EventPayload payload) {
        Event<EventPayload> event = new Event<>();
        event.eventId = eventId;
        event.type = type;
        event.payload = payload;
        return event;
    }

    public String toJson() {
        return DataSerializer.serialize(this);
    }

    public static Event<EventPayload> fromJson(String json) {
        EventRaw eventRaw = DataSerializer.deserialize(json, EventRaw.class);
        if (eventRaw == null) {
            return null;
        }
        Event<EventPayload> event = new Event<>();
        event.eventId = eventRaw.getEventId();
        event.type = EventType.from(eventRaw.getType());
        event.payload = DataSerializer.deserialize(eventRaw.getPayload(), event.type.getPayloadClass());
        return event;
    }

    @Getter
    public static class EventRaw {
        private Long eventId;
        private String type;
        private Object payload;
    }
}
