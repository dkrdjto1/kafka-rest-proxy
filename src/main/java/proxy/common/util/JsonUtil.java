package proxy.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Json utility 클래스
 */
@Slf4j
public class JsonUtil {

    /**
     * 입력된 오브젝트를 JSON string으로 변환하여 반환
     * @param obj
     * @return
     */
    public static String writeValueAsString(Object obj) {
        String str = null;
        try {
            str = new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("", e.getMessage());
        }

        return str;
    }
}
