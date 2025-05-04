package proxy.api.request;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartitionOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 컨슈머 오프셋 목록 커밋 요청
 */
@Getter
@NoArgsConstructor
public class ConsumerOffsetCommitRequest {

    @Nullable
    private List<TopicPartitionOffset> offsets; // 커밋할 오프셋 목록
}
