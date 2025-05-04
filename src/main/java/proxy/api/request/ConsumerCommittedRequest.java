package proxy.api.request;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartition;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마지막으로 커밋된 오프셋 조회 요청
 */
@Getter
@NoArgsConstructor
public class ConsumerCommittedRequest {

    @Nullable
    private List<TopicPartition> partitions; // 토픽-파티션 목록
}
