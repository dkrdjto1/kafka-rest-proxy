package proxy.infra.kafka.result;

import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * (AVRO, JSONSCHEMA, PROTOBUF) 컨슈머 레코드 응답값 맵핑 클래스
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SchemaConsumerRecord {

    @NotNull
    @Nullable
    private final String topic;      // 토픽명

    @Nullable
    private final JsonNode key;      // 메시지 키

    @Nullable
    private final JsonNode value;    // 메시지 값

    @PositiveOrZero
    @Nullable
    private final Integer partition; // 파티션ID

    @PositiveOrZero
    @Nullable
    private final Long offset;       // 오프셋 번호


    /**
     * {@link ConsumerRecord} -> {@link SchemaConsumerRecord} 변환
     * @param record
     * @return
     */
    public static SchemaConsumerRecord fromConsumerRecord(ConsumerRecord<JsonNode, JsonNode> record) {
        // 파티션ID 형식이 올바르지 않은 경우
        if (record.getPartition() < 0) throw new IllegalArgumentException();

        // 오프셋 번호 형식이 올바르지 않은 경우
        if (record.getOffset() < 0) throw new IllegalArgumentException();

        // 응답값 맵핑 및 반환
        return new SchemaConsumerRecord(
            Objects.requireNonNull(record.getTopic()),
            record.getKey(),
            record.getValue(),
            record.getPartition(),
            record.getOffset());
    }
}
