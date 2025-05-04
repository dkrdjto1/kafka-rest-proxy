package proxy.api;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import proxy.api.headers.ContentType;
import proxy.api.request.ConsumerAssignmentRequest;
import proxy.api.request.ConsumerCommittedRequest;
import proxy.api.request.ConsumerOffsetCommitRequest;
import proxy.api.request.ConsumerSeekRequest;
import proxy.api.request.ConsumerSeekToRequest;
import proxy.api.request.ConsumerSubscriptionRequest;
import proxy.api.request.CreateConsumerInstanceRequest;
import proxy.api.resource.API_URI_RESOURCE;
import proxy.api.response.ConsumerAssignmentResponse;
import proxy.api.response.ConsumerCommittedResponse;
import proxy.api.response.ConsumerSubscriptionResponse;
import proxy.api.response.CreateConsumerInstanceResponse;
import proxy.common.callback.ConsumerReadCallback;
import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.domain.ConsumeAction;
import proxy.infra.kafka.result.BinaryConsumerRecord;
import proxy.infra.kafka.result.ConsumerRecord;
import proxy.infra.kafka.result.JsonConsumerRecord;
import proxy.infra.kafka.result.SchemaConsumerRecord;
import proxy.infra.kafka.worker.BinaryConsumerWorker;
import proxy.infra.kafka.worker.ConsumerWorker;
import proxy.infra.kafka.worker.JsonConsumerWorker;
import proxy.infra.kafka.worker.SchemaConsumerWorker;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class Consume {

    @Autowired
    private ConsumeAction consumeAction;

    /**
     * 컨슈머 인스턴스 생성
     * @param config
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON)
    @ResponseBody
    public CreateConsumerInstanceResponse createGroup(
            @PathVariable(value = "group_name") String groupName,
            @RequestBody @Valid CreateConsumerInstanceRequest config) {

        // 컨슈머 인스턴스ID 생성
        String instanceId = this.consumeAction.createConsumer(groupName, config.toConsumerInstanceConfig());

        // 컨슈머 인스턴스 기본 URI 생성
        String baseUri = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String instanceBaseUri = baseUri + "/consumers/" + groupName + "/instances/" + instanceId;

        return new CreateConsumerInstanceResponse(instanceId, instanceBaseUri);
    }

    /**
     * 컨슈머 인스턴스 삭제
     * @return
     */
    @DeleteMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM)
    public void deleteGroup(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance) {

        this.consumeAction.deleteConsumer(groupName, instance);
    }

    /**
     * 컨슈머 토픽 구독
     * @param subscription
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION, consumes = ContentType.KAFKA_V2_JSON)
    public void subscribe(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerSubscriptionRequest subscription) {
        
        this.consumeAction.subscribe(groupName, instance, subscription);
    }

    /**
     * 컨슈머 토픽 구독 조회
     * @return
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION, produces = ContentType.KAFKA_V2_JSON)
    @ResponseBody
    public ConsumerSubscriptionResponse subscription(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance) {
        
        var response = this.consumeAction.subscription(groupName, instance);

        return response;
    }

    /**
     * 컨슈머 토픽 구독 취소
     * @return
     */
    @DeleteMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_SUBSCRIPTION)
    public void unsubscribe(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance) {
        
        this.consumeAction.unsubscribe(groupName, instance);
    }

    /**
     * 컨슈머 오프셋 목록 커밋
     * @param offsetCommitRequest
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_OFFSETS, consumes = ContentType.KAFKA_V2_JSON)
    public void commitOffsets(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerOffsetCommitRequest offsetCommitRequest) {
        
        this.consumeAction.commitOffsets(groupName, instance, offsetCommitRequest);
    }

    /**
     * <pre>
     * 컨슈머 오프셋 커밋 목록 조회
     * *주의: Get Method 사용 시, @RequestBody 로 요청 보내는 것이 지양되는 추세여서, 해당 API를 더 이상 사용할 수 없는 경우 {@code committedOffsets2} 사용 요망.
     * </pre>
     * @param committedRequest
     * @return
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_OFFSETS, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON)
    @ResponseBody
    public ConsumerCommittedResponse committedOffsets(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerCommittedRequest committedRequest) {
        
        // 마지막으로 커밋된 오프셋을 조회할 요청 파티션 목록이 없는 경우, 예외 반환
        if (committedRequest == null) throw new ProxyException(Errors.PARTITION_NOT_FOUND);

        var response = this.consumeAction.committed(groupName, instance, committedRequest);

        return response;
    }

    /**
     * 컨슈머 오프셋 커밋 목록 조회
     * @param committedRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_COMMITTED_OFFSETS, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON)
    @ResponseBody
    public ConsumerCommittedResponse committedOffsets2(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerCommittedRequest committedRequest) {
        
        // 마지막으로 커밋된 오프셋을 조회할 요청 파티션 목록이 없는 경우, 예외 반환
        if (committedRequest == null) throw new ProxyException(Errors.PARTITION_NOT_FOUND);

        var response = this.consumeAction.committed(groupName, instance, committedRequest);

        return response;
    }

    /**
     * 컨슈머 파티션 수동 할당
     * @param assignmentRequest
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_ASSIGNMENTS, consumes = ContentType.KAFKA_V2_JSON)
    public void assign(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerAssignmentRequest assignmentRequest) {
        
        this.consumeAction.assign(groupName, instance, assignmentRequest);
    }

    /**
     * 컨슈머 파티션 수동 할당 조회
     * @return
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_ASSIGNMENTS, produces = ContentType.KAFKA_V2_JSON)
    @ResponseBody
    public ConsumerAssignmentResponse assignment(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance) {
        
        var response = this.consumeAction.assignment(groupName, instance);

        return response;
    }

    /**
     * 컨슈머 패치 오프셋 업데이트
     * @param request
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS, consumes = ContentType.KAFKA_V2_JSON)
    public void seekToOffset(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerSeekRequest request) {
        
        this.consumeAction.seek(groupName, instance, request);
    }

    /**
     * 컨슈머 초기 오프셋으로 이동
     * @param request
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_BEGINNING, consumes = ContentType.KAFKA_V2_JSON)
    public void seekToBeginning(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerSeekToRequest request) {
        
        this.consumeAction.seekToBeginning(groupName, instance, request);
    }

    /**
     * 컨슈머 최근 오프셋으로 이동
     * @param request
     */
    @PostMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_POSITIONS_END, consumes = ContentType.KAFKA_V2_JSON)
    public void seekToEnd(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestBody @Valid ConsumerSeekToRequest request) {
        
        this.consumeAction.seekToEnd(groupName, instance, request);
    }

    /**
     * (BINARY) 컨슈머 읽기 작업 생성
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS, produces = ContentType.KAFKA_V2_JSON_BINARY)
    public CompletableFuture<ResponseEntity<?>> readRecordBinary(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestParam(value = "timeout", required = false, defaultValue = "-1") long timeoutMs,
            @RequestParam(value = "max_bytes", required = false, defaultValue = "-1") long maxBytes) {
        
        // 컨슈머 읽기 작업 생성
        var response = readRecords(
            groupName,
            instance,
            Duration.ofMillis(timeoutMs),
            maxBytes,
            BinaryConsumerWorker.class,
            BinaryConsumerRecord::fromConsumerRecord);

        return response.thenApply(consumeResponse -> ResponseEntity.ok().body(consumeResponse));
    }

    /**
     * (JSON) 컨슈머 읽기 작업 생성
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS, produces = ContentType.KAFKA_V2_JSON_JSON)
    public CompletableFuture<ResponseEntity<?>> readRecordJson(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestParam(value = "timeout", required = false, defaultValue = "-1") long timeoutMs,
            @RequestParam(value = "max_bytes", required = false, defaultValue = "-1") long maxBytes) {

        // 컨슈머 읽기 작업 생성
        var response = readRecords(
            groupName,
            instance,
            Duration.ofMillis(timeoutMs),
            maxBytes,
            JsonConsumerWorker.class,
            JsonConsumerRecord::fromConsumerRecord);

        return response.thenApply(consumeResponse -> ResponseEntity.ok().body(consumeResponse));
    }

    /**
     * (AVRO) 컨슈머 읽기 작업 생성
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS, produces = ContentType.KAFKA_V2_JSON_AVRO)
    public CompletableFuture<ResponseEntity<?>> readRecordAvro(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestParam(value = "timeout", required = false, defaultValue = "-1") long timeoutMs,
            @RequestParam(value = "max_bytes", required = false, defaultValue = "-1") long maxBytes) {

        // 컨슈머 읽기 작업 생성
        var response = readRecords(
            groupName,
            instance,
            Duration.ofMillis(timeoutMs),
            maxBytes,
            SchemaConsumerWorker.class,
            SchemaConsumerRecord::fromConsumerRecord);

        return response.thenApply(consumeResponse -> ResponseEntity.ok().body(consumeResponse));
    }

    /**
     * (JSON SCHEMA) 컨슈머 읽기 작업 생성
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS, produces = ContentType.KAFKA_V2_JSON_JSON_SCHEMA)
    public CompletableFuture<ResponseEntity<?>> readRecordJsonSchema(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestParam(value = "timeout", required = false, defaultValue = "-1") long timeoutMs,
            @RequestParam(value = "max_bytes", required = false, defaultValue = "-1") long maxBytes) {
        
        // 컨슈머 읽기 작업 생성
        var response = readRecords(
            groupName,
            instance,
            Duration.ofMillis(timeoutMs),
            maxBytes,
            SchemaConsumerWorker.class,
            SchemaConsumerRecord::fromConsumerRecord);

        return response.thenApply(consumeResponse -> ResponseEntity.ok().body(consumeResponse));
    }

    /**
     * (PROTOBUF) 컨슈머 읽기 작업 생성
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     */
    @GetMapping(path = API_URI_RESOURCE.CONSUMERS_PARAM_INSTANCES_PARAM_RECORDS, produces = ContentType.KAFKA_V2_JSON_PROTOBUF)
    public CompletableFuture<ResponseEntity<?>> readRecordProtobuf(
            @PathVariable(value = "group_name") String groupName,
            @PathVariable(value = "instance") String instance,
            @RequestParam(value = "timeout", required = false, defaultValue = "-1") long timeoutMs,
            @RequestParam(value = "max_bytes", required = false, defaultValue = "-1") long maxBytes) {
        
        // 컨슈머 읽기 작업 생성
        var response = readRecords(
            groupName,
            instance,
            Duration.ofMillis(timeoutMs),
            maxBytes,
            SchemaConsumerWorker.class,
            SchemaConsumerRecord::fromConsumerRecord);

        return response.thenApply(consumeResponse -> ResponseEntity.ok().body(consumeResponse));
    }

    /**
     * 컨슈머 읽기 작업 생성
     * @param consumerWorkerType // 메시지 타입별 컨슈머 워커
     * @param toJsonWrapper      // 컨슈머 레코드 응답값 맵핑 메소드
     */
    private <KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> CompletableFuture<List<?>> readRecords(
            String groupName,
            String instance,
            Duration timeoutMs,
            long maxBytes,
            Class<? extends ConsumerWorker<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT>> consumerWorkerType,
            Function<ConsumerRecord<ClientKeyT, ClientValueT>, ?> toJsonWrapper) {
        
        // max_bytes 요청값이 0 이하인 경우, 최대 바이트 수에 제한을 두지 않음.
        maxBytes = (maxBytes <= 0) ? Long.MAX_VALUE : maxBytes;

        CompletableFuture<List<?>> result = new CompletableFuture<>();

        // 레코드 읽기 작업 생성
        this.consumeAction.readRecords(
            groupName,
            instance,
            consumerWorkerType,
            timeoutMs,
            maxBytes,
            new ConsumerReadCallback<ClientKeyT, ClientValueT>() {
                @Override
                public void onCompletion(List<ConsumerRecord<ClientKeyT, ClientValueT>> records, Exception e) {
                    // 작업 실패 시
                    if (e != null) {
                        result.completeExceptionally(e);
                        log.error("Received exception during read records...");
                    // 메시지 조회 성공 시
                    } else {
                        // 메시지 포맷별 응답값 맵핑
                        result.complete(records.stream().map(toJsonWrapper).collect(Collectors.toList()));
                        log.info("Received records successfully");
                    }
                }
            });

        return result;
    }
}