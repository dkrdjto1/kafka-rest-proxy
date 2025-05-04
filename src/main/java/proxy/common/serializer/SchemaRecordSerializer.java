package proxy.common.serializer;

import java.io.IOException;
import java.util.Optional;

import org.apache.avro.AvroTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Message;

import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;
import proxy.common.util.AvroSchemaUtil;
import proxy.common.util.JsonSchemaUtil;
import proxy.common.util.ProtobufSchemaUtil;
import proxy.infra.schemaregistry.result.RegisteredSchema;

/**
 * 스키마가 필요한 경우, 메시지 직렬화
 */
@Component
public class SchemaRecordSerializer {
    
    @Autowired
    private AvroSerializer avroSerializer;

    @Autowired
    private JsonSchemaSerializer jsonSchemaSerializer;

    @Autowired
    private ProtobufSerializer protobufSerializer;

    public Optional<ByteString> serialize(
            EmbeddedFormat format,
            Optional<RegisteredSchema> schema,
            JsonNode data,
            boolean isKey) {
            
        // 메시지가 null인 경우 return
        if (data.isNull()) return Optional.empty();

        // 스키마가 주어지지 않은 경우, 예외 발생
        if (!schema.isPresent()) {
            throw isKey ? new ProxyException(Errors.KEY_SCHEMA_MISSING) : new ProxyException(Errors.VALUE_SCHEMA_MISSING);
        }

        switch (format) {
            case AVRO:
                return Optional.of(serializeAvro(schema.get(), data));

            case JSONSCHEMA:
                return Optional.of(serializeJsonschema(schema.get(), data));

            case PROTOBUF:
                return Optional.of(serializeProtobuf(schema.get(), data));

            default:
                throw new AssertionError(String.format("Unexpected enum constant: %s", format));
        }
    }

    /**
     * AVRO 타입 직렬화
     * @param schema
     * @param data
     * @return
     */
    private ByteString serializeAvro(RegisteredSchema schema, JsonNode data) {
        Object record = null;
        try {
            record = AvroSchemaUtil.toObject(schema.getSchema(), data); // data -> Object
        } catch (AvroTypeException | IOException e) {
            throw new ProxyException(Errors.JSON_TO_OBJECT_FAILED, e.getMessage());
        }
        return ByteString.copyFrom(avroSerializer.serialize(schema, record));
    }

    /**
     * JSONSCHEMA 타입 직렬화
     * @param schema
     * @param data
     * @return
     */
    private ByteString serializeJsonschema(RegisteredSchema schema, JsonNode data) {
        Object record = null;
        try {
            record = JsonSchemaUtil.toObject(schema.getSchema(), data); // data -> Object
        } catch (ProcessingException | IOException e) {
            throw new ProxyException(Errors.JSON_TO_OBJECT_FAILED, e.getMessage());
        }
        return ByteString.copyFrom(jsonSchemaSerializer.serialize(schema, record));
    }

    /**
     * PROTOBUF 타입 직렬화
     * @param schema
     * @param data
     * @return
     */
    private ByteString serializeProtobuf(RegisteredSchema schema, JsonNode data) {
        Message record = null;
        try {
            record = (Message) ProtobufSchemaUtil.toObject(schema.getSchema(), data); // data -> Object
        } catch (DescriptorValidationException | IOException e) {
            throw new ProxyException(Errors.JSON_TO_OBJECT_FAILED, e.getMessage());
        }
        return ByteString.copyFrom(protobufSerializer.serialize(schema, record));
    }
}
