package proxy.infra.kafka.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 레코드 및 메시지 바이트 사이즈 응답값 맵핑 클래스
 */
@Getter
@RequiredArgsConstructor
public class ConsumerRecordAndSize<K, V> {

    private final ConsumerRecord<K, V> record; // 컨슈머 레코드
    private final long size;                   // 메시지 바이트 사이즈
}
