package proxy.common.callback;

import java.util.List;

import proxy.infra.kafka.result.ConsumerRecord;

/**
 * <pre>
 * 메시지 컨슈밍 작업 실행 후 결과/예외 반환 콜백
 * -> 컨슈밍 성공 시, 레코드 응답값 맵핑
 * -> 컨슈밍 실패 시, 예외 반환
 * </pre>
 */
public interface ConsumerReadCallback<K, V> {
    void onCompletion(List<ConsumerRecord<K, V>> records, Exception e);
}
