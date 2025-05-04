package proxy.common.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.stereotype.Component;

import com.google.protobuf.Message;

import proxy.infra.schemaregistry.result.RegisteredSchema;

@Component
public class ProtobufSerializer {
    private static final byte MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4;

    public byte[] serialize(RegisteredSchema schema, Message record) {
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

            // 3. write protobuf content
            // === 스키마에 여러 메시지 타입이 존재하는 경우 ===
            // 1) MessageTypeName에 해당하는 개별 스키마ID (schemaId)
            // 혹은 2) 프로듀싱할 protobuf content에 맞는 MessageType의 스키마 (schemaId) 내에서의 인덱스
            // -> payload에 포함 필요
            // -> 2)인 경우에만 추가 로직 필요
            record.writeTo(output);

            byte[] bytes = output.toByteArray();
            output.close();
            
            return bytes;

        }  catch (InterruptedIOException e) {
            throw new TimeoutException("Error serializing Protobuf message", e);
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error serializing Protobuf message", e);
        }
    }
}
