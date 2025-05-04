package proxy.common.serializer;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;

import proxy.common.format.EmbeddedFormat;
import proxy.infra.schemaregistry.result.RegisteredSchema;
import lombok.NonNull;

@Component
public class RecordSerializer {

    @NonNull
    @Autowired
    private NoSchemaRecordSerializer noSchemaRecordSerializer;

    @NonNull
    @Autowired
    private SchemaRecordSerializer schemaRecordSerializer;

    /**
     * 메시지 직렬화
     * @param format    // 메시지 포맷
     * @param topicName // 토픽명
     * @param schema    // 스키마
     * @param data      // 메시지
     * @param isKey     // 메시지 키 여부
     * @return
     */
    public Optional<ByteString> serialize(
            EmbeddedFormat format,
            String topicName,
            Optional<RegisteredSchema> schema,
            JsonNode data,
            boolean isKey) {
        // 스키마가 필요한 메시지 포맷인 경우 // AVRO, PROTOBUF, JSONSCHEMA
        if (format.requiresSchema()) {
            return schemaRecordSerializer.serialize(format, schema, data, isKey);
        // 스키마가 필요하지 않은 메시지 포맷인 경우 // JSON, BINARY, STRING
        } else {
            return noSchemaRecordSerializer.serialize(format, data);
        }
    }
}
