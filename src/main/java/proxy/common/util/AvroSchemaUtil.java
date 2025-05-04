package proxy.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Avro Schema utility 클래스
 */
public class AvroSchemaUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DecoderFactory decoderFactory = DecoderFactory.get();

    /**
     * Convert schema type String -> Avro
     * @param rawSchema // 스키마(내용)
     * @return
     */
    public static Schema toAvroSchema(String rawSchema) {
        return new Schema.Parser().parse(rawSchema);
    }

    /**
     * Convert data type JsonNode -> Object
     * @param data   // avro content
     * @param schema // avro schema
     * @return
     * @throws IOException
     */
    public static Object toObject(String rawSchema, JsonNode data) throws IOException {
        // get avro schema
        Schema avroSchema = toAvroSchema(rawSchema);

        // reader 생성
        DatumReader<Object> reader = new GenericDatumReader<>(avroSchema);

        // JsonNode -> Object
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, data);
            Object object = reader.read(null,
                decoderFactory.jsonDecoder(avroSchema, new ByteArrayInputStream(output.toByteArray()))
            );

            return object;
        }
    }
}
