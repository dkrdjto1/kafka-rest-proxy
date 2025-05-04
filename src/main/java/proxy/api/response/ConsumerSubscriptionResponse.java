package proxy.api.response;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 구독 응답
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConsumerSubscriptionResponse {

    @Nullable
    private final List<String> topics; // 구독 중인 토픽명 목록
}
