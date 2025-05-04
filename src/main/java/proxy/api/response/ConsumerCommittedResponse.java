package proxy.api.response;

import java.util.List;

import javax.annotation.Nullable;

import proxy.infra.kafka.result.TopicPartitionOffset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 마지막으로 커밋된 오프셋 조회 응답
 */
@Getter
@RequiredArgsConstructor
public class ConsumerCommittedResponse {

    @Nullable
    private final List<TopicPartitionOffset> offsets; // 오프셋 목록
}