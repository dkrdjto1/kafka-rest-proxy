package proxy.common.converter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.common.util.JacksonMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Conversion of JSONSCHEMA -> JSON
 */
@Component
@Slf4j
public class JsonSchemaConverter implements SchemaConverter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper jsonMapper = JacksonMapper.INSTANCE;

    @Override
    public JsonNodeAndSize toJson(Object value) {
        try {
            byte[] bytes = convert(value);
            // 바이트 값이 없는 경우, 빈 객체 반환
            if (bytes == null) {
                return new JsonNodeAndSize(NullNode.getInstance(), 0);
            }
            // JsonNode로 표현된 데이터 & 바이트 사이즈 맵핑
            return new JsonNodeAndSize(objectMapper.readTree(bytes), bytes.length);

        } catch (IOException e) {
            log.error("Jackson failed to deserialize JSON: ", e);
            throw new ProxyException(Errors.JSONSCHEMA_TO_JSON_FAILED, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected exception converting JSON Schema to JSON: ", e);
            throw new ProxyException(Errors.JSONSCHEMA_TO_JSON_FAILED, e.getMessage());
        }
    }
    
    /**
     * convert Object -> byte[]
     * @param value
     * @return
     * @throws IOException
     */
    private byte[] convert(Object value) throws IOException {
        if (value == null) return null;

        // data -> byte[]
        StringWriter output = new StringWriter();
        jsonMapper.writeValue(output, value);
        String jsonString = output.toString();

        return jsonString.getBytes(StandardCharsets.UTF_8);
    }
}
