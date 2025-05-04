package proxy.api.response;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 파티션 수동 할당 응답
 */
@Getter
@RequiredArgsConstructor
public class ConsumerAssignmentResponse {

    @Nullable
    private final List<TopicPartition> partitions; // 토픽-파티션 목록
}
