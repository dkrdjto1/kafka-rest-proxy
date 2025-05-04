package proxy.infra.kafka.result;

import javax.annotation.Nullable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토픽-파티션 정보 맵핑 클래스
 */
@Getter
@AllArgsConstructor
public class TopicPartition {

    @NotEmpty
    @Nullable
    private String topic; // 토픽명
    @PositiveOrZero
    @Nullable
    private Integer partition; // 파티션ID
}
