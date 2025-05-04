package proxy.api.request;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartition;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 컨슈머 파티션 수동 할당 요청
 */
@Getter
@NoArgsConstructor
public class ConsumerAssignmentRequest {

    @Nullable
    private List<TopicPartition> partitions; // 토픽-파티션 목록
}
