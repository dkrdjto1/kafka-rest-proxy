package proxy.infra.kafka.worker;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.fasterxml.jackson.databind.JsonNode;

import proxy.common.config.ConsumerInstanceConfig;
import proxy.common.converter.SchemaConverter;
import proxy.infra.kafka.result.ConsumerRecordAndSize;

public class SchemaConsumerWorker extends ConsumerWorker<Object, Object, JsonNode, JsonNode> {

    private final SchemaConverter schemaConverter;

    /**
     * Default constructor
     */
    public SchemaConsumerWorker(
            ConsumerInstanceConfig instanceConfig,
            ConsumerInstanceId instanceId,
            Consumer<Object, Object> consumer,
            SchemaConverter schemaConverter) {
        super(instanceConfig, instanceId, consumer);
        this.schemaConverter = schemaConverter;
    }

    @Override
    public ConsumerRecordAndSize<JsonNode, JsonNode> createConsumerRecord(ConsumerRecord<Object, Object> record) {
        // JsonNode로 converting 및 메시지 사이즈 조회
        SchemaConverter.JsonNodeAndSize keyNode = schemaConverter.toJson(record.key());
        SchemaConverter.JsonNodeAndSize valueNode = schemaConverter.toJson(record.value());
        
        // 컨슈머 레코드, 메시지 사이즈 맵핑
        return new ConsumerRecordAndSize<>(
            new proxy.infra.kafka.result.ConsumerRecord<>(
                record.topic(),
                keyNode.getJson(),
                valueNode.getJson(),
                record.partition(),
                record.offset()),
            keyNode.getSize() + valueNode.getSize()
        );
    }
}
