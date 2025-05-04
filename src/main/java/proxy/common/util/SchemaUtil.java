package proxy.common.util;

/**
 * Schema utility 클래스
 */
public class SchemaUtil {

    /**
     * (Default) 스키마 식별을 위해, "토픽명"으로 스키마 주제 생성
     * @param topic // 토픽명
     * @param isKey // 키 여부
     * @return
     */
    public static String getSubjectName(String topic, boolean isKey) {
        // 주어진 토픽이 없는 경우, null 반환
        if (topic == null) return null;

        // 메시지 키: 토픽명 + -key , 메시지 값: 토픽명 + -value
        return isKey ? topic + "-key" : topic + "-value";
    }

    /**
     * 키 여부 String type 반환
     * @param isKey // 키 여부
     * @return
     */
    public static String getSchemaType(Boolean isKey) {
        if (isKey == null) return "unknown";
        else if (isKey) return "key";
        else return "value";
    }
}
