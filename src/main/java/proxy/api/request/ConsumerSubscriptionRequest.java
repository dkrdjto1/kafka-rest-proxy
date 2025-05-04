package proxy.api.request;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 구독 요청
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConsumerSubscriptionRequest {

    @Nullable
    private final List<String> topics; // 구독할 토픽명 목록

    @Nullable
    private final String topicPattern; // 구독할 토픽 패턴
}
