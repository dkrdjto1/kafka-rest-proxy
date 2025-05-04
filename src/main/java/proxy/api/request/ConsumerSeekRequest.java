package proxy.api.request;

import java.util.List;

import proxy.infra.kafka.result.TopicPartitionOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 컨슈머 패치 오프셋 업데이트 요청
 */
@Getter
@NoArgsConstructor
public class ConsumerSeekRequest {

    private List<TopicPartitionOffset> offsets; // 업데이트할 패치 오프셋 목록
}
