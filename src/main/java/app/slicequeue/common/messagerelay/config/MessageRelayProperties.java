package app.slicequeue.common.messagerelay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "message-relay")
public class MessageRelayProperties {
    private int corePoolSize = 20;
    private int maxPoolSize = 50;
    private int queueCapacity = 100;
    private String threadNamePrefix = "sq-msg-relay-pub-event-";
}
