package app.slicequeue.common.messagerelay.relay;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;



class AssignedShardTest {

    @Test
    void ofTest() {
        // given
        long sharedCount = 64L;
        List<String> appList = List.of("appId1", "appId2", "appId3");

        // when
        AssignedShard assignedShard1 = AssignedShard.of(appList.get(0), appList, sharedCount);
        AssignedShard assignedShard2 = AssignedShard.of(appList.get(1), appList, sharedCount);
        AssignedShard assignedShard3 = AssignedShard.of(appList.get(2), appList, sharedCount);
        AssignedShard assignedShard4 = AssignedShard.of("invalid", appList, sharedCount);

        // then
        List<Long> result = Stream.of(
                        assignedShard1.getShards(),
                        assignedShard2.getShards(),
                        assignedShard3.getShards(),
                        assignedShard4.getShards()
                ).flatMap(List::stream)
                .toList();
        assertThat(result).hasSize((int) sharedCount);

        for (int i = 0; i < sharedCount; i++) {
            assertThat(result.get(i)).isEqualTo(i);
        }

        assertThat(assignedShard4.getShards()).isEmpty();
    }
}
