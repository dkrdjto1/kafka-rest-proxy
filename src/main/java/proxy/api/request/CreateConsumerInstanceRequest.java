package proxy.api.request;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import proxy.common.config.ConsumerInstanceConfig;
import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨슈머 인스턴스 생성 요청
 */
@Getter
@RequiredArgsConstructor
public class CreateConsumerInstanceRequest {

    // 요청 포맷이 없는 경우, BINARY 기본 설정
    private static final EmbeddedFormat DEFAULT_FORMAT = EmbeddedFormat.BINARY;

    // 컨슈머 인스턴스ID
    @Nullable
    private final String id;

    // 컨슈머 인스턴스명
    @Nullable
    private final String name;

    // 메시지 포맷
    @NotNull
    private final String format;

    // 오프셋 자동 재설정 [latest, earliest, none]
    @Nullable
    @JsonProperty(value = "auto.offset.reset")
    private final String autoOffsetReset;

    // 컨슈머 오프셋 자동 커밋
    @Nullable
    @JsonProperty(value = "enable.auto.commit")
    private final String enableAutoCommit;

    // fetch 요청에 대해 카프카 서버가 응답할 최소 데이터 크기
    @Nullable
    @JsonProperty(value = "fetch.min.bytes")
    private final Integer fetchMinBytes;

    // 최대 요청 크기에 도달하지 않은 경우, 클라이언트가 요청의 응답을 기다리는 최대 메시지 대기 시간
    @Nullable
    @JsonProperty(value = "request.timeout.ms")
    private final Integer requestTimeoutMs;

    /**
     * {@link CreateConsumerInstanceRequest} -> {@link ConsumerInstanceConfig} 변환
     * @return
     */
    public ConsumerInstanceConfig toConsumerInstanceConfig() {
        return new ConsumerInstanceConfig(
            id, name, computeFormat(format), autoOffsetReset, enableAutoCommit, fetchMinBytes, requestTimeoutMs);
    }

    private EmbeddedFormat computeFormat(@Nullable String format) {
        if (format == null) return DEFAULT_FORMAT;
        
        String formatCanonical = format.toUpperCase();
        for (EmbeddedFormat f : EmbeddedFormat.values()) {
            if (f.name().equals(formatCanonical)) {
                return f;
            }
        }
        throw new ProxyException(Errors.INVALID_FORMAT_TYPE);
    }
}
