package proxy.api.request;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartition;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 컨슈머 초기 오프셋으로 이동 요청
 */
@Getter
@NoArgsConstructor
public class ConsumerSeekToRequest {

    @Nullable
    private List<TopicPartition> partitions; // 토픽-파티션 목록
}
