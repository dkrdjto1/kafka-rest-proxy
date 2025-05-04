package proxy.common.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.stereotype.Component;

import proxy.common.util.AvroSchemaUtil;
import proxy.infra.schemaregistry.result.RegisteredSchema;

@Component
public class AvroSerializer {
    private final EncoderFactory encoderFactory = EncoderFactory.get();

    private static final byte MAGIC_BYTE = 0x0;
    private static final int SCHEMA_ID_SIZE = 4;

    public byte[] serialize(RegisteredSchema schema, Object record) {
        if (record == null) return null;

        // get avro schema
        Schema avroSchema = AvroSchemaUtil.toAvroSchema(schema.getSchema());

        try {
            // 스키마ID 조회
            int schemaId = schema.getSchemaId();

            // output 생성
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // 1. write magic byte :: deserialize 할 때, magic byte가 존재하는 경우에만 payload에서 schemaId 조회
            output.write(MAGIC_BYTE);

            // 2. write schemaId
            output.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

            // 3. write avro content
            writeDatum(output, record, avroSchema);

            byte[] bytes = output.toByteArray();
            output.close();

            return bytes;

        } catch (InterruptedIOException e) {
            throw new TimeoutException("Error serializing Avro message", e);
        } catch (IOException | RuntimeException e) {
            // avro serialization can throw AvroRuntimeException, NullPointerException, ClassCastException, etc
            throw new SerializationException("Error serializing Avro message", e);
        }
    }

    /**
     * Add Avro Content
     * @param output // output stream
     * @param record // avro content
     * @param schema // avro schema
     */
    private void writeDatum(ByteArrayOutputStream output, Object record, Schema schema) throws IOException {
        // encoder, writer 생성
        BinaryEncoder encoder = encoderFactory.directBinaryEncoder(output, null);
        GenericDatumWriter<Object> writer = new GenericDatumWriter<>(schema);

        // write avro content
        writer.write(record, encoder);
        encoder.flush();
    }
}
