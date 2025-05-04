package proxy.infra.kafka.result;

import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.protobuf.ByteString;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * (BINARY) 컨슈머 레코드 응답값 맵핑 클래스
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BinaryConsumerRecord {

    @NotNull
    @Nullable
    private final String topic;      // 토픽명

    @Nullable
    private final byte[] key;        // 메시지 키

    @Nullable
    private final byte[] value;      // 메시지 값

    @PositiveOrZero
    @Nullable
    private final Integer partition; // 파티션ID

    @PositiveOrZero
    @Nullable
    private final Long offset;       // 오프셋 번호


    /**
     * {@link ConsumerRecord} -> {@link BinaryConsumerRecord} 변환
     * @param record
     * @return
     */
    public static BinaryConsumerRecord fromConsumerRecord(ConsumerRecord<ByteString, ByteString> record) {
        // 파티션ID 형식이 올바르지 않은 경우
        if (record.getPartition() < 0) throw new IllegalArgumentException();

        // 오프셋 번호 형식이 올바르지 않은 경우
        if (record.getOffset() < 0) throw new IllegalArgumentException();

        // 응답값 맵핑 및 반환
        return new BinaryConsumerRecord(
            Objects.requireNonNull(record.getTopic()),
            record.getKey() != null ? record.getKey().toByteArray() : null,
            record.getValue() != null ? record.getValue().toByteArray() : null,
            record.getPartition(),
            record.getOffset());
    }
}
