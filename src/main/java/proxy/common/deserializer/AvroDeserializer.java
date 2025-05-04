package proxy.common.deserializer;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import proxy.common.context.DeserializationContext;

@Component
public class AvroDeserializer implements Deserializer<Object> {

    private boolean isKey;

    /**
     * Default constructor
     */
    public AvroDeserializer() {

    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        this.isKey = isKey;
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        return deserialize(topic, isKey, data);
    }
    
    private Object deserialize(String topic, Boolean isKey, byte[] payload) {
        if (payload == null) return null;

        try {
            // payload 분리 (magic byte + schemaId + avro content)
            DeserializationContext context = new DeserializationContext(topic, isKey, payload);
            
            // read avro content with avro schema
            return context.readAvro(context.getRawSchemaFromRegistry());

        } catch (Exception e) {
            throw new SerializationException("Error deserializing AVRO message", e);
        }
    }

}
