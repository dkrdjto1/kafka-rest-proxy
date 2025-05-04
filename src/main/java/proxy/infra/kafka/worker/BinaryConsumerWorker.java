package proxy.infra.kafka.worker;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.google.protobuf.ByteString;

import proxy.common.config.ConsumerInstanceConfig;
import proxy.infra.kafka.result.ConsumerRecordAndSize;

public class BinaryConsumerWorker extends ConsumerWorker<byte[], byte[], ByteString, ByteString> {

    /**
     * Default constructor
     */
    public BinaryConsumerWorker(
            ConsumerInstanceConfig instanceConfig,
            ConsumerInstanceId instanceId,
            Consumer<byte[], byte[]> consumer) {
        super(instanceConfig, instanceId, consumer);
    }

    @Override
    public ConsumerRecordAndSize<ByteString, ByteString> createConsumerRecord(ConsumerRecord<byte[], byte[]> record) {
        // byte[] 형식의 메시지 키, 값 바이트 합 계산
        long approxSize =
            (record.key() != null ? record.key().length : 0)
                + (record.value() != null ? record.value().length : 0);

        // 컨슈머 레코드, byte[] 형식의 메시지 키, 값 사이즈 맵핑
        return new ConsumerRecordAndSize<>(
            new proxy.infra.kafka.result.ConsumerRecord<>(
                record.topic(),
                record.key() != null ? ByteString.copyFrom(record.key()) : null,     // byte[] -> ByteString
                record.value() != null ? ByteString.copyFrom(record.value()) : null, // byte[] -> ByteString
                record.partition(),
                record.offset()),
            approxSize
        );
    }
}
