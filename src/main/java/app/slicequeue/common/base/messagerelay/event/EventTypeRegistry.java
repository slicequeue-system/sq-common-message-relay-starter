package app.slicequeue.common.base.messagerelay.event;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * EventType code(String) → 실제 EventType 구현체로 매핑하는 레지스트리
 */
@Component
public class EventTypeRegistry {

    private final Map<String, EventType> registry = new HashMap<>();

    /**
     * 코드 기반 등록
     * @param eventType 예: UserInfoChangedEventType
     */
    public void register(EventType eventType) {
        registry.put(eventType.getCode(), eventType);
    }

    /**
     * 코드로 조회
     */
    public Optional<EventType> find(String code) {
        return Optional.ofNullable(registry.get(code));
    }

    /**
     * 존재 여부
     */
    public boolean contains(String code) {
        return registry.containsKey(code);
    }

    /**
     * 전체 조회 (디버깅용)
     */
    public Map<String, EventType> all() {
        return registry;
    }
}
