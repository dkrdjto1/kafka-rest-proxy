package proxy.common.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * Json Schema utility 클래스
 */
public class JsonSchemaUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();

    /**
     * Convert schema type String -> Json
     * @param rawSchema // 스키마(내용)
     * @return
     * @throws JsonProcessingException
     * @throws ProcessingException
     */
    public static JsonSchema toJsonSchema(String rawSchema)
            throws JsonProcessingException, ProcessingException {
        
        JsonNode schemaNode = objectMapper.readTree(rawSchema); // String -> JsonNode
        JsonSchema jsonSchema = jsonSchemaFactory.getJsonSchema(schemaNode); // JsonNode -> JsonSchema

        return jsonSchema;
    }

    /**
     * Convert data type JsonNode -> Object
     * @param data   // json content
     * @param schema // json schema
     * @return
     * @throws ProcessingException
     * @throws IOException
     */
    public static Object toObject(String rawSchema, JsonNode data)
            throws ProcessingException, IOException {
        
        // get json schema
        JsonSchema jsonSchema = toJsonSchema(rawSchema);

        // 스키마 검증
        ProcessingReport report = jsonSchema.validate(data);

        // 검증이 통과된 경우
        if (report.isSuccess()) {
            // JsonNode -> Object
            return objectMapper.treeToValue(data, Object.class);
        }

        return null;
    }
}
