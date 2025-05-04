package proxy.common.cache;

import lombok.Getter;

/**
 * <pre>
 * Cache 설정값
 * - 마지막 액세스 후 지정된 시간이 지나면 캐시가 만료됨
 * - 캐시 만료 시간을 0으로 설정할 경우, 캐시를 사용하지 않는 것으로 간주하므로 주의
 * </pre>
 */
@Getter
public enum CacheConfig {
    ID_TO_SCHEMA(1440, 10000),      // 만료: 1일, 크기: 1만 개
    SCHEMA_TO_ID(1440, 10000),      // 만료: 1일, 크기: 1만 개
    SCHEMA_TO_VERSION(1440, 10000), // 만료: 1일, 크기: 1만 개

    MISSING_SCHEMA_ID(1, 10000),    // 만료: 1분, 크기: 1만 개
    MISSING_SCHEMA(1, 10000)        // 만료: 1분, 크기: 1만 개
    ;

    private int minutesForExpireAfterAccess;
    private int maximumSize;

    /**
     * Cache 정의
     * @param minutesForExpireAfterAccess // 캐시 만료 시간 (분)
     * @param maximumSize                 // 캐시 최대 크기 (건)
     */
    CacheConfig(int minutesForExpireAfterAccess, int maximumSize) {
        this.minutesForExpireAfterAccess = minutesForExpireAfterAccess;
        this.maximumSize = maximumSize;
    }

    public static final String KAFKA_REST_CACHE_CONFIG_BEAN_NAME = "kafka-rest-cache-config";
}
