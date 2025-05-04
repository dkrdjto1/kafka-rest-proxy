package proxy.infra.kafka.result;

import java.time.Instant;
import java.util.Optional;

import org.apache.kafka.clients.producer.RecordMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 카프카로부터 반환받은 프로듀싱 응답값 맵핑 클래스
 */
@Getter
@Builder
@AllArgsConstructor
public class ProduceResult {

    public int partitionId;             // 파티션ID
    public long offset;                 // 오프셋 번호
    public Optional<Instant> timestamp; // 레코드가 브로커에 도착한 시각
    public int serializedKeySize;       // 직렬화된 메시지 키 크기
    public int serializedValueSize;     // 직렬화된 메시지 값 크기
    public Instant completionTimestamp; // 카프카로부터 응답 수신 시각

    /**
     * 프로듀싱 응답값 맵핑
     * @param metadata            // 카프카로부터 반환받은 응답값
     * @param completionTimestamp // 카프카로부터 응답 수신 시각
     * @return
     */
    public static ProduceResult fromRecordMetadata(RecordMetadata metadata, Instant completionTimestamp) {
        return ProduceResult.builder()
                .partitionId(metadata.partition())
                .offset(metadata.offset())
                .timestamp(Optional.ofNullable(metadata.hasTimestamp() ? Instant.ofEpochMilli(metadata.timestamp()) : null))
                .serializedKeySize(metadata.serializedKeySize())
                .serializedValueSize(metadata.serializedValueSize())
                .completionTimestamp(completionTimestamp)
                .build();
    }
}
