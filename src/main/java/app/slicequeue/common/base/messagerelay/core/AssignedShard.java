package app.slicequeue.common.base.messagerelay.core;

import lombok.Getter;

import java.util.List;
import java.util.stream.LongStream;

/**
 * AssignedShard 각 인스턴스가 담당할 shard 계산
 */
@Getter
public class AssignedShard {

    private List<Long> shards;

    public static AssignedShard of(String appId, List<String> appIds, long shardCount) {
        // Redis 기록된 살아있는 앱 ID 리스트를 기준으로
        // 현재 인스턴스가 담당할 샤드 범위를 계산해서 리턴
        AssignedShard assignedShard = new AssignedShard();
        assignedShard.shards = assign(appId, appIds, shardCount);
        return assignedShard;
    }

    private static List<Long> assign(String appId, List<String> appIds, long shardCount) {
        int appIndex = findAppIndex(appId, appIds);
        if (appIndex == -1) {
            return List.of();
        }
        long start = appIndex * shardCount / appIds.size();
        long end = (appIndex + 1) * shardCount / appIds.size() - 1;

        return LongStream.rangeClosed(start, end).boxed().toList();
    }

    private static int findAppIndex(String appId, List<String> appIds) {
        for (int i = 0; i < appIds.size(); i++) {
            if (appIds.get(i).equals(appId)) {
                return i;
            }
        }
        return -1;
    }
}
