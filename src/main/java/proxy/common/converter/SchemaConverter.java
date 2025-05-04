package proxy.common.converter;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Converting data to {@link JsonNode} type
 */
public interface SchemaConverter {

    /**
     * 스키마(AVRO, JSONSCHEMA, PROTOBUF) 형식 -> {@link JsonNode} 형식으로 변환
     *
     * @param value // 형식 변환할 값
     * @return
     */
    JsonNodeAndSize toJson(Object value);

    @Getter
    @AllArgsConstructor
    final class JsonNodeAndSize {

        private final JsonNode json; // Object -> JsonNode 로 형식 변환된 메시지 키/값
        private final long size;     // byte[] 형식의 메시지 키/값 사이즈
    }
}
