package proxy.common.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import proxy.infra.schemaregistry.result.RegisteredSchema;

@Component
public class JsonSchemaSerializer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final byte MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4;

    public byte[] serialize(RegisteredSchema schema, Object record) {
        if (record == null) return null;
    
        try {
            // 스키마ID 조회
            int schemaId = schema.getSchemaId();

            // output 생성
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // 1. write magic byte :: deserialize 할 때, magic byte가 존재하는 경우에만 payload에서 schemaId 조회
            output.write(MAGIC_BYTE);

            // 2. write schemaId
            output.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

            // 3. write jsonschema content
            output.write(objectMapper.writeValueAsBytes(record));

            byte[] bytes = output.toByteArray();
            output.close();
            
            return bytes;

        } catch (InterruptedIOException e) {
            throw new TimeoutException("Error serializing JSON message", e);
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}
