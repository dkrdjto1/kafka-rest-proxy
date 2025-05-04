package proxy.api.response;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로듀싱 응답
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProduceResponse {

    @NotEmpty
    @Nullable
    private final List<PartitionOffset> offsets; // 파티션 및 오프셋 목록
    @Nullable
    private final Integer keySchemaId;           // 키 스키마ID
    @Nullable
    private final Integer valueSchemaId;         // 값 스키마ID

    @Getter
    @RequiredArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PartitionOffset {
        
        @PositiveOrZero
        @Nullable
        private final Integer partition; // 파티션ID (성공 시)
        @PositiveOrZero
        @Nullable
        private final Long offset;       // 오프셋 번호 (성공 시)
        @Nullable
        private final Integer errorCode; // 오류 코드 (실패 시)
        @Nullable
        private final String error;      // 오류 메시지 (실패 시)
    }

}
