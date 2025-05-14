package app.slicequeue.common.base.messagerelay.event;

import app.slicequeue.common.base.messagerelay.util.DataSerializer;
import lombok.Getter;

import java.util.Map;

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

    public static Event<EventPayload> fromJson(String json, EventTypeRegistry eventTypeRegistry) {
        EventRaw eventRaw = DataSerializer.deserialize(json, EventRaw.class);
        if (eventRaw == null) return null;

        EventType type = eventTypeRegistry.find(eventRaw.getTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + eventRaw.getType()));

        Event<EventPayload> event = new Event<>();
        event.eventId = eventRaw.getEventId();
        event.type = type;
        event.payload = DataSerializer.deserialize(eventRaw.getPayload(), type.getPayloadClass());
        return event;
    }

    @Getter
    public static class EventRaw {
        private Long eventId;
        private Map<String, Object> type;
        private Object payload;

        public String getTypeCode() {
            Object code = type.get("code");
            if (code == null) throw  new IllegalArgumentException("Event type code value is null");
            return (String) code;
        }
    }
}
