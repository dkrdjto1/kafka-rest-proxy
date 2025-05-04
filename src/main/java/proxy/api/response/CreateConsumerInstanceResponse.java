package proxy.api.response;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 인스턴스 생성 응답
 */
@Getter
@RequiredArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateConsumerInstanceResponse {

    @NotBlank
    @Nullable
    private final String instanceId; // 컨슈머 인스턴스 고유ID

    @NotBlank
    @Nullable
    @URL
    private final String baseUri;    // 컨슈머 인스턴스 기본 URI
}
