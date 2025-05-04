package io.spitha.felice.common.config;

import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.kafka.clients.consumer.ConsumerConfig;

import io.spitha.felice.common.format.EmbeddedFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 컨슈머 인스턴스 설정값
 */
@Getter
@AllArgsConstructor
public class ConsumerInstanceConfig {

    // 컨슈머 인스턴스ID
    @Nullable
    private final String id;
    
    // 컨슈머 인스턴스명
    @Nullable
    private final String name;
    
    // 메시지 포맷
    private final EmbeddedFormat format;

    // 오프셋 자동 재설정 [latest, earliest, none]
    private String autoOffsetReset;

    // 컨슈머 오프셋 자동 커밋
    private String enableAutoCommit;

    // fetch 요청에 대해 카프카 서버가 응답할 최소 데이터 크기
    private Integer fetchMinBytes;

    // fetch 요청에 대해 카프카 서버가 응답할 최대 데이터 크기
    private Integer fetchMaxBytes;

    // 최대 요청 크기에 도달하지 않은 경우, 클라이언트가 요청의 응답을 기다리는 최대 메시지 대기 시간
    private Integer requestTimeoutMs;

    // 한 번의 fetch 요청으로 컨슈머가 가져올 수 있는 최대 메시지 개수
    private Integer maxPollRecords;

    // fetch 요청에 대해 서버에서 응답할 데이터가 [fetchMinBytes]에 미치지 못한 경우 응답을 기다리는 최대 시간
    private Integer fetchMaxWaitMs;

    // 이 설정값 이내에 poll() 메서드가 호출되지 않으면, broker는 해당 consumer가 죽었다고 판단하고 해당 consumer의 파티션 할당을 다른 consumer에게 재할당함.
    private Integer maxPollIntervalMs;

    /**
     * Constructor for consumer instance create request
     * @param id
     * @param name
     * @param format
     * @param autoOffsetReset
     * @param enableAutoCommit
     * @param fetchMinBytes
     * @param requestTimeoutMs
     */
    public ConsumerInstanceConfig(String id, String name, EmbeddedFormat format, String autoOffsetReset,
            String enableAutoCommit, Integer fetchMinBytes, Integer requestTimeoutMs) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.autoOffsetReset = autoOffsetReset;
        this.enableAutoCommit = enableAutoCommit;
        this.fetchMinBytes = fetchMinBytes;
        this.requestTimeoutMs = requestTimeoutMs;
    }

    /**
     * 컨슈머 인스턴스 설정값 세팅 및 반환
     * @param props
     * @return
     */
    public ConsumerInstanceConfig setInstanceConfig(Properties props) {
        this.autoOffsetReset = props.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG);
        this.enableAutoCommit = props.getProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG);
        this.fetchMinBytes = (Integer) props.get(ConsumerConfig.FETCH_MIN_BYTES_CONFIG);
        this.fetchMaxBytes = (Integer) props.get(ConsumerConfig.FETCH_MAX_BYTES_CONFIG);
        this.requestTimeoutMs = (Integer) props.get(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG);
        this.maxPollRecords = (Integer) props.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG);
        this.fetchMaxWaitMs = (Integer) props.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG);
        this.maxPollIntervalMs = (Integer) props.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG);

        return this;
    }
}
