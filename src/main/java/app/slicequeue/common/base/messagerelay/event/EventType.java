package app.slicequeue.common.base.messagerelay.event;

public interface EventType {

    String getCode();

    String getTopic();  // 예: "user-events"

    Class<? extends EventPayload> getPayloadClass();
}
