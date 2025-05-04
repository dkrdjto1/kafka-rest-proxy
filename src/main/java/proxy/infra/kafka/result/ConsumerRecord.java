package proxy.infra.kafka.result;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 레코드 응답값 맵핑 클래스
 */
@Getter
@RequiredArgsConstructor
public class ConsumerRecord<K, V> {

    private final String topic;  // 토픽명

    @Nullable
    private final K key;         // 메시지 키

    @Nullable
    private final V value;       // 메시지 값

    private final int partition; // 파티션ID

    private final long offset;   // 오프셋 번호
}
