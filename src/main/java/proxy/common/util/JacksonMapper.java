package proxy.common.util;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * A utility class wrapping a generic ObjectMapper singleton
 */
public class JacksonMapper {
    public static final ObjectMapper INSTANCE = JsonMapper.builder()
        .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
        .build();
}
