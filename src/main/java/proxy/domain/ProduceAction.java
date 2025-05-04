package proxy.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMultimap;
import com.google.protobuf.ByteString;

import proxy.api.request.ProduceRequest;
import proxy.api.request.ProduceRequest.ProduceRecord;
import proxy.api.response.ProduceResponse;
import proxy.api.response.ProduceResponse.PartitionOffset;
import proxy.common.CompletableFutures;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;
import proxy.common.serializer.RecordSerializer;
import proxy.infra.kafka.ProducerManager;
import proxy.infra.kafka.result.ProduceResult;
import proxy.infra.schemaregistry.SchemaManager;
import proxy.infra.schemaregistry.result.RegisteredSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Service
public class ProduceAction {
    
    @Autowired
    private ProducerManager producerManager;

    @Autowired
    private RecordSerializer recordSerializer;

    @Autowired
    private SchemaManager schemaManager;

    /**
     * (스키마 X) 메시지 프로듀싱
     * @param format    // 메시지 포맷
     * @param topicName // 토픽명
     * @param partition // 파티션ID
     * @param request   // 프로듀싱 요청값
     * @return
     */
    public CompletableFuture<ProduceResponse> produceWithoutSchema(
            EmbeddedFormat format,
            String topicName,
            Optional<Integer> partition,
            ProduceRequest request) {
        
        // 메시지 직렬화
        List<SerializedKeyAndValue> serialized =
            serialize(
                format,
                topicName,
                partition,
                /* keySchema= */ Optional.empty(),
                /* valueSchema= */ Optional.empty(),
                request.getRecords());

        // 메시지 프로듀싱
        List<CompletableFuture<ProduceResult>> result = doProduce(topicName, serialized);

        // 프로듀싱 응답값 -> REST Proxy 응답 형태
        return produceResultsToResponse(
            /* keySchema= */ Optional.empty(), /* valueSchema= */ Optional.empty(), result);
    }

    /**
     * (스키마 O) 메시지 프로듀싱
     * @param format    // 메시지 포맷
     * @param topicName // 토픽명
     * @param partition // 파티션ID
     * @param request   // 프로듀싱 요청값
     * @return
     */
    public CompletableFuture<ProduceResponse> produceWithSchema(
            EmbeddedFormat format,
            String topicName,
            Optional<Integer> partition,
            ProduceRequest request) {

        // 키 스키마 조회
        Optional<RegisteredSchema> keySchema =
            getSchema(
                format,
                topicName,
                request.getKeySchemaId(),
                request.getKeySchema(),
                /* isKey= */ true);

        // 값 스키마 조회
        Optional<RegisteredSchema> valueSchema =
            getSchema(
                format,
                topicName,
                request.getValueSchemaId(),
                request.getValueSchema(),
                /* isKey= */ false);

        // 메시지 직렬화
        List<SerializedKeyAndValue> serialized =
            serialize(
                format,
                topicName,
                partition,
                keySchema,
                valueSchema,
                request.getRecords());

        // 메시지 프로듀싱
        List<CompletableFuture<ProduceResult>> result = doProduce(topicName, serialized);

        // 프로듀싱 응답값 -> REST Proxy 응답 형태
        return produceResultsToResponse(keySchema, valueSchema, result);
    }

    /**
     * 스키마 조회
     * @param format    // 메시지 포맷
     * @param topicName // 토픽명
     * @param schemaId  // 스키마ID
     * @param schema    // 스키마
     * @param isKey     // 키 여부
     * @return
     */
    private Optional<RegisteredSchema> getSchema(
            EmbeddedFormat format,
            String topicName,
            Optional<Integer> schemaId,
            Optional<String> schema,
            boolean isKey) {
            
        // 스키마 필수 타입이고, 스키마ID 또는 스키마가 주어진 경우
        if (format.requiresSchema() && (schemaId != null || schema != null)) {
            return Optional.of(
                schemaManager
                    .getSchema(
                        /* topicName= */ topicName,
                        /* format= */ format,
                        /* schemaId= */ schemaId,
                        /* rawSchema= */ schema,
                        /* isKey= */ isKey));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 프로듀싱 응답값 -> REST Proxy 응답 형태
     * @param keySchema   // 키 스키마
     * @param valueSchema // 값 스키마
     * @param results     // 프로듀싱 응답값
     * @return
     */
    private CompletableFuture<ProduceResponse> produceResultsToResponse(
            Optional<RegisteredSchema> keySchema,
            Optional<RegisteredSchema> valueSchema,
            List<CompletableFuture<ProduceResult>> resultFutures) {

        // 프로듀싱 응답값마다 반복
        CompletableFuture<List<PartitionOffset>> offsetsFuture = 
            CompletableFutures.allAsList(
                resultFutures.stream()
                // 메시지 전송 성공 시
                .map(
                    future ->
                        future.thenApply(
                            result ->
                                new PartitionOffset(
                                    result.getPartitionId(),
                                    result.getOffset(),
                                    /* errorCode= */ null,
                                    /* error= */ null)))
                // 메시지 전송 실패 시
                .map(
                    future ->
                        future.exceptionally(
                            throwable ->
                                new PartitionOffset(
                                    /* partition= */ null,
                                    /* offset= */ null,
                                    Errors.errorCodeFromProducerException(throwable.getCause()),
                                    throwable.getCause().getMessage())))
                .collect(Collectors.toList()));

        return offsetsFuture.thenApply(
            offsets ->
                new ProduceResponse(
                    offsets,
                    keySchema.map(RegisteredSchema::getSchemaId).orElse(null),
                    valueSchema.map(RegisteredSchema::getSchemaId).orElse(null)));
    }

    /**
     * 메시지 프로듀싱
     * @param topicName  // 토픽명
     * @param serialized // 직렬화된 메시지 키, 값 목록
     * @return
     */
    private List<CompletableFuture<ProduceResult>> doProduce(
            String topicName, List<SerializedKeyAndValue> serialized) {

        // 레코드마다 반복
        return serialized.stream()
            .map(
                record ->
                    producerManager
                        .produce(
                            /* clusterId= */ "",
                            topicName,
                            record.getPartitionId(),
                            /* headers= */ ImmutableMultimap.of(),
                            record.getKey(),
                            record.getValue(),
                            /* timestamp= */ Instant.now()))
            .collect(Collectors.toList());
    }

    /**
     * 메시지 직렬화
     * @param format      // 메시지 포맷
     * @param topicName   // 토픽명
     * @param partition   // 파티션ID
     * @param keySchema   // 메시지 키 스키마
     * @param valueSchema // 메시지 값 스키마
     * @param records     // 레코드 목록
     * @return
     */
    private List<SerializedKeyAndValue> serialize(
            EmbeddedFormat format,
            String topicName,
            Optional<Integer> partition,
            Optional<RegisteredSchema> keySchema,
            Optional<RegisteredSchema> valueSchema,
            List<ProduceRecord> records) {

        // 레코드마다 반복
        return records.stream()
            .map(
                record ->
                    new SerializedKeyAndValue(
                        // 파티션ID
                        record.getPartition() == null ? partition : record.getPartition(),
                        // 메시지 키 직렬화
                        recordSerializer
                            .serialize(
                                format,
                                topicName,
                                keySchema,
                                record.getKey() == null ? NullNode.getInstance() : record.getKey().get(),
                                /* isKey= */ true),
                        // 메시지 값 직렬화
                        recordSerializer
                            .serialize(
                                format,
                                topicName,
                                valueSchema,
                                record.getValue() == null ? NullNode.getInstance() : record.getValue().get(),
                                /* isKey= */ false)))
            .collect(Collectors.toList());
    }

    /**
     * 직렬화된 메시지 키, 값
     */
    @Getter
    @AllArgsConstructor
    public class SerializedKeyAndValue {

        public Optional<Integer> partitionId;
        public Optional<ByteString> key;
        public Optional<ByteString> value;
    }
}
