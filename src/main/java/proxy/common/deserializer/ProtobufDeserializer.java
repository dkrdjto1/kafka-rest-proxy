package proxy.common.deserializer;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import com.google.protobuf.Message;

import proxy.common.context.DeserializationContext;

@Component
public class ProtobufDeserializer<T extends Message> implements Deserializer<T> {

    private boolean isKey;

    /**
     * Default constructor
     */
    public ProtobufDeserializer() {

    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        this.isKey = isKey;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        return deserialize(topic, isKey, data);
    }

    @SuppressWarnings({"unchecked"})
    private T deserialize(String topic, Boolean isKey, byte[] payload) {
        if (payload == null) return null;

        try {
            // payload 분리 (magic byte + schemaId + protobuf content)
            DeserializationContext context = new DeserializationContext(topic, isKey, payload);
            
            // read protobuf content with protobuf schema
            return (T) context.readProtobuf(context.getRawSchemaFromRegistry());

        } catch (Exception e) {
            throw new SerializationException("Error deserializing PROTOBUF message", e);
        }
    }
    
}
