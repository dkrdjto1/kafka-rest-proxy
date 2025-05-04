package proxy.api.request;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

/**
 * 프로듀싱 요청
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProduceRequest {

    @NotEmpty
    public List<ProduceRecord> records;     // 레코드 목록

    // 스키마ID, 스키마 -> 상호배타적
    public Optional<Integer> keySchemaId;   // 키 스키마ID
    public Optional<String> keySchema;      // 키 스키마
    public Optional<Integer> valueSchemaId; // 값 스키마ID
    public Optional<String> valueSchema;    // 값 스키마

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ProduceRecord {

        public Optional<Integer> partition; // 파티션ID
        public Optional<JsonNode> key;      // 메시지 키
        public Optional<JsonNode> value;    // 메시지 값
    }
}
