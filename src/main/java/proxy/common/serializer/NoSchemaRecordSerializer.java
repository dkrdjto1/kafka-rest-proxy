package proxy.common.serializer;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;

/**
 * 스키마가 필요하지 않은 경우, 메시지 직렬화
 */
@Component
public class NoSchemaRecordSerializer {

    @Autowired
    private JsonSerializer jsonSerializer;

    Optional<ByteString> serialize(EmbeddedFormat format, JsonNode data) {
        // 메시지가 null인 경우 return
        if (data.isNull()) return Optional.empty();

        switch (format) {
            case BINARY:
                return Optional.of(serializeBinary(data));

            case JSON:
                return Optional.of(serializeJson(data));

            case STRING:
                return Optional.of(serializeString(data));

            default:
                throw new AssertionError(String.format("Unexpected enum constant: %s", format));
        }
    }

    /**
     * BINARY 타입 직렬화
     * @param data
     * @return
     */
    private ByteString serializeBinary(JsonNode data) {
        // 메시지가 JSON 형식이 아닌 경우
        if (!data.isTextual()) {
            throw new ProxyException(Errors.INVALID_DATA_FORMAT, String.format("data=%s is not a base64 string.", data));
        }
        
        byte[] serialized = new byte[0];
        
        try {
            serialized = BaseEncoding.base64().decode(data.asText());
        } catch (IllegalArgumentException e) {
            throw new ProxyException(Errors.INVALID_DATA_FORMAT, String.format("data=%s is not a base64 string.", data));
        }
        
        return ByteString.copyFrom(serialized);
    }

    /**
     * JSON 타입 직렬화
     * @param data
     * @return
     */
    private ByteString serializeJson(JsonNode data) {
        return ByteString.copyFrom(jsonSerializer.serialize(/* topic= */ "", data));
    }

    /**
     * STRING 타입 직렬화
     * @param data
     * @return
     */
    private static ByteString serializeString(JsonNode data) {
        // 메시지가 JSON 형식이 아닌 경우
        if (!data.isTextual()) {
            throw new ProxyException(Errors.INVALID_DATA_FORMAT, String.format("data=%s is not a string.", data));
        }

        return ByteString.copyFromUtf8(data.asText());
    }
}
