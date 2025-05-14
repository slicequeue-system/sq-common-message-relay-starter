package app.slicequeue.common.base.messagerelay.event;

import app.slicequeue.sq_user.common.event.payload.UserInfoChangedEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * 이벤트 통신을 위한 클래스
 */
@Log4j2
@Getter
@RequiredArgsConstructor
public enum EventType {

    USER_INFO_CHANGED(UserInfoChangedEventPayload.class, Topic.SQ_USER_INFO_CHANGED);

    private final Class<? extends EventPayload> payloadClass;   // 이 이벤트들이 어떤 페이로드를 가지는지 그 클래스 타입!
    private final String topic;                                 // 이 이벤트들이 어떤 카프카 토픽으로 전달 될 수 있는지

    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            log.error("[EventType.from] type={}", type, e);
            return null;
        }
    }

    public static class Topic {
        public static final String SQ_USER_INFO_CHANGED = "sq-user-info-changed";
    }
}
