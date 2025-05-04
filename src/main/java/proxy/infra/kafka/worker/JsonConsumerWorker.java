package proxy.infra.kafka.worker;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;

import com.fasterxml.jackson.databind.ObjectMapper;

import proxy.common.config.ConsumerInstanceConfig;
import proxy.infra.kafka.result.ConsumerRecordAndSize;

public class JsonConsumerWorker extends ConsumerWorker<byte[], byte[], Object, Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Default constructor
     */
    public JsonConsumerWorker(
            ConsumerInstanceConfig instanceConfig,
            ConsumerInstanceId instanceId,
            Consumer<byte[], byte[]> consumer) {
        super(instanceConfig, instanceId, consumer);
    }

    @Override
    public ConsumerRecordAndSize<Object, Object> createConsumerRecord(ConsumerRecord<byte[], byte[]> record) {
        long approxSize = 0; // byte[] 형식의 메시지 키, 값 바이트 합

        Object key = null;
        Object value = null;

        // 메시지 키가 있는 경우
        if (record.key() != null) {
            approxSize += record.key().length; // byte[] 형식의 메시지 키 사이즈 더함.
            key = deserialize(record.key());   // byte[] -> Object
        }

        // 메시지 값이 있는 경우
        if (record.value() != null) {
            approxSize += record.value().length; // byte[] 형식의 메시지 값 사이즈 더함.
            value = deserialize(record.value()); // byte[] -> Object
        }

        // 컨슈머 레코드, byte[] 형식의 메시지 키, 값 사이즈 맵핑
        return new ConsumerRecordAndSize<>(
            new proxy.infra.kafka.result.ConsumerRecord<>(
                record.topic(), key, value, record.partition(), record.offset()),
            approxSize
        );
    }

    private Object deserialize(byte[] data) {
        try {
            return objectMapper.readValue(data, Object.class);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
