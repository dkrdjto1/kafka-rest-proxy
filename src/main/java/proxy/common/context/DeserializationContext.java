package proxy.common.context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.errors.SerializationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import proxy.common.util.AvroSchemaUtil;
import proxy.common.util.BeanUtil;
import proxy.common.util.JacksonMapper;
import proxy.common.util.JsonSchemaUtil;
import proxy.common.util.ProtobufSchemaUtil;
import proxy.common.util.SchemaUtil;
import proxy.infra.schemaregistry.CachedSchemaManager;

/**
 * 카프카로부터 응답받은 payload를 활용하기 위한 클래스
 */
public class DeserializationContext {

    private final CachedSchemaManager cachedSchemaManager;

    private final ObjectMapper jsonMapper = JacksonMapper.INSTANCE;
    private final DecoderFactory decoderFactory = DecoderFactory.get();

    protected static final byte MAGIC_BYTE = 0x0;
    protected static final int idSize = 4;

    private final String topic;      // 토픽명
    private final Boolean isKey;     // 키 여부
    private final byte[] payload;    // 카프카로부터의 응답 페이로드 전문
    private final ByteBuffer buffer; // 버퍼 (페이로드 분리 용도)
    private final int schemaId;      // 스키마ID

    public DeserializationContext(final String topic, final Boolean isKey, final byte[] payload) {
        this.topic = topic;
        this.isKey = isKey;
        this.payload = payload;
        this.buffer = getByteBuffer(payload); // discard the magic byte
        this.schemaId = buffer.getInt();      // get schemaId

        this.cachedSchemaManager = (CachedSchemaManager) BeanUtil.getBean("cachedSchemaManager");
    }

    /**
     * Magic byte 확인
     * @param payload
     * @return
     */
    private ByteBuffer getByteBuffer(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        if (buffer.get() != MAGIC_BYTE) {
            throw new SerializationException("Unknown magic byte!");
        }
        return buffer;
    }

    /**
     * payload의 schemaId로 스키마(내용) 조회
     * @return
     */
    public String getRawSchemaFromRegistry() {
        try {
            // subject 생성 (Naming 방식 고정)
            String subject = SchemaUtil.getSubjectName(topic, isKey);

            // 스키마ID로 스키마(내용) 조회
            return this.cachedSchemaManager.getRawSchemaById(subject, schemaId);

        } catch (IOException e) {
            throw new SerializationException("Error retrieving Avro " + SchemaUtil.getSchemaType(isKey) + " schema for id " + schemaId, e);
        }
    }

    /**
     * Read Avro content with Avro Schema
     * @param rawSchema
     * @return
     */
    public Object readAvro(String rawSchema) {
        try {
            // 1. buffer에서 avro content 읽어옴.
            // get avro content length
            int length = buffer.limit() - 1 - idSize;

            // get start position of avro content
            int start = buffer.position() + buffer.arrayOffset();

            // get avro schema
            Schema avroSchema = AvroSchemaUtil.toAvroSchema(rawSchema);

            // reader 생성
            DatumReader<Object> reader = new GenericDatumReader<>(avroSchema);

            // get avro content
            Object result = reader.read(null, decoderFactory.binaryDecoder(buffer.array(), start, length, null)); // byte[] -> Object

            // JsonNode로 형변환 가능한 경우
            if (result instanceof JsonNode) {
                // 2. convert JsonNode -> Object
                result = AvroSchemaUtil.toObject(rawSchema, (JsonNode) result);
            }

            // 3. avro content 반환 (Object)
            return result;

        } catch (IOException | RuntimeException e) {
            // avro deserialization may throw AvroRuntimeException, NullPointerException, etc
            throw new SerializationException("Error deserializing Avro message for schemaId " + schemaId, e);
        }
    }

    /**
     * Read Json content with Json Schema
     * @param rawSchema
     * @return
     */
    public Object readJsonSchema(String rawSchema) {
        JsonNode jsonNode = null;
        Object result;
        try {
            // 1. buffer에서 json content 읽어옴.
            // get json content length
            int length = buffer.limit() - 1 - idSize;

            // get start position of json content
            int start = buffer.position() + buffer.arrayOffset();

            // get json content
            jsonNode = jsonMapper.readValue(buffer.array(), start, length, JsonNode.class); // byte[] -> JsonNode

            // 2. convert JsonNode -> Object
            result = JsonSchemaUtil.toObject(rawSchema, jsonNode);

            // 3. json content 반환 (Object)
            return result;

        } catch (ProcessingException | IOException e) {
            throw new SerializationException("JSON " + jsonNode + " does not match schema " + rawSchema, e);
        } catch (RuntimeException e) {
            throw new SerializationException("Error deserializing JSON message for schemaId " + schemaId, e);
        }
    }

    /**
     * Read Protobuf content with Protobuf Schema
     * @param rawSchema
     * @return
     */
    public Object readProtobuf(String rawSchema) {
        Object result;
        try {
            // 1. buffer에서 protobuf content 읽어옴.
            // get protobuf content length
            int length = buffer.limit() - 1 - idSize;

            // get start position of protobuf content
            int start = buffer.position() + buffer.arrayOffset();

            // get protobuf descriptor from raw schema
            Descriptor descriptor = ProtobufSchemaUtil.getDescriptor(rawSchema);
            if (descriptor == null) {
                throw new SerializationException("Could not find descriptor with schemaId " + schemaId);
            }

            // 2. convert byte[] -> Object (Message)
            result = DynamicMessage.parseFrom(descriptor,
                new ByteArrayInputStream(buffer.array(), start, length)
            );

            // 3. protobuf content 반환 (Object)
            return result;
        } catch (InvalidProtocolBufferException | DescriptorValidationException e) {
            throw new SerializationException("Protobuf descriptor does not match schema " + rawSchema, e);
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error deserializing Protobuf message for schemaId " + schemaId, e);
        }
    }
}
