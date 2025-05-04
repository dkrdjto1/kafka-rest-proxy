package proxy.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import proxy.api.request.ConsumerAssignmentRequest;
import proxy.api.request.ConsumerCommittedRequest;
import proxy.api.request.ConsumerOffsetCommitRequest;
import proxy.api.request.ConsumerSeekRequest;
import proxy.api.request.ConsumerSeekToRequest;
import proxy.api.request.ConsumerSubscriptionRequest;
import proxy.api.response.ConsumerAssignmentResponse;
import proxy.api.response.ConsumerCommittedResponse;
import proxy.api.response.ConsumerSubscriptionResponse;
import proxy.common.callback.ConsumerReadCallback;
import proxy.common.config.ConsumerInstanceConfig;
import proxy.common.config.KafkaClientConfig;
import proxy.common.converter.AvroConverter;
import proxy.common.converter.JsonSchemaConverter;
import proxy.common.converter.ProtobufConverter;
import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.infra.kafka.ConsumerManager;
import proxy.infra.kafka.task.ConsumerReadTask;
import proxy.infra.kafka.worker.BinaryConsumerWorker;
import proxy.infra.kafka.worker.ConsumerInstanceId;
import proxy.infra.kafka.worker.ConsumerWorker;
import proxy.infra.kafka.worker.JsonConsumerWorker;
import proxy.infra.kafka.worker.SchemaConsumerWorker;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConsumeAction {

    @Autowired
    private KafkaClientConfig kafkaClientConfig;

    @Autowired
    private ConsumerManager consumerManager;

    public static final String CONSUMER_ID_PREFIX = "kafka-rest-consumer-";

    /**
     * 컨슈머 인스턴스 생성
     * @param groupName      // 컨슈머 그룹명
     * @param instanceConfig // 컨슈머 인스턴스 설정값
     * @return
     */
    public String createConsumer(String groupName, ConsumerInstanceConfig instanceConfig) {
        // 컨슈머 인스턴스 고유ID 생성
        String instanceName = getConsumerInstanceName(instanceConfig);
        ConsumerInstanceId cid = new ConsumerInstanceId(groupName, instanceName);

        // consumer configs
        var props = this.kafkaClientConfig.getConsumerConfig(groupName, instanceConfig);
        props = this.kafkaClientConfig.addSecurityConfig(props);

        // 컨슈머 생성
        Consumer<?, ?> consumer = new KafkaConsumer<>(props);

        // 컨슈머 인스턴스 설정값 정보 저장
        instanceConfig = instanceConfig.setInstanceConfig(props);

        // 메시지 포맷에 따라 컨슈머 워커 생성
        ConsumerWorker<?, ?, ?, ?> worker = createConsumerWorker(instanceConfig, cid, consumer);
        synchronized (this) {
            // 컨슈머 풀에 인스턴스 추가
            this.consumerManager.addConsumer(cid, worker);
        }

        // 인스턴스 고유ID 반환
        return instanceName;
    }

    /**
     * 컨슈머 워커 생성
     * @param format   // 메시지 포맷
     * @param cid      // 컨슈머 인스턴스ID
     * @param consumer // 컨슈머
     * @return
     */
    @SuppressWarnings({"unchecked"})
    private ConsumerWorker<?, ?, ?, ?> createConsumerWorker(ConsumerInstanceConfig instanceConfig, ConsumerInstanceId cid, Consumer<?, ?> consumer) {
        // 메시지 포맷에 따라 구분
        switch (instanceConfig.getFormat()) {
            case BINARY:
                return new BinaryConsumerWorker(instanceConfig, cid, (Consumer<byte[], byte[]>) consumer);
            case AVRO:
                return new SchemaConsumerWorker(instanceConfig, cid, (Consumer<Object, Object>) consumer, new AvroConverter());
            case JSONSCHEMA:
                return new SchemaConsumerWorker(instanceConfig, cid, (Consumer<Object, Object>) consumer, new JsonSchemaConverter());
            case PROTOBUF:
                return new SchemaConsumerWorker(instanceConfig, cid, (Consumer<Object, Object>) consumer, new ProtobufConverter());
            case JSON:
                return new JsonConsumerWorker(instanceConfig, cid, (Consumer<byte[], byte[]>) consumer);
            default:
                throw new ProxyException(Errors.INVALID_EMBEDDED_FORMAT, String.format(
                    "Invalid embedded format %s for new consumer.", instanceConfig.getFormat()));
        }
    }

    /**
     * 컨슈머 인스턴스 고유ID 생성
     * @param instanceConfig // 컨슈머 인스턴스 설정값
     * @return
     */
    private String getConsumerInstanceName(ConsumerInstanceConfig instanceConfig) {
        // 요청된 고유ID가 있는 경우, 이름보다 우선 적용
        if (instanceConfig.getId() != null) return instanceConfig.getId();

        // 요청된 인스턴스명 적용
        if (instanceConfig.getName() != null) return instanceConfig.getName();

        // 요청이 없는 경우, 랜덤 인스턴스명 생성
        StringBuilder name = new StringBuilder(CONSUMER_ID_PREFIX);
        name.append(UUID.randomUUID().toString());

        // 생성된 고유ID 반환
        return name.toString();
    }

    /**
     * 컨슈머 인스턴스 조회
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param toRemove  // 컨슈머 인스턴스 삭제 여부
     * @return
     */
    private synchronized ConsumerWorker<?, ?, ?, ?> getConsumerInstance(String groupName, String instance, boolean toRemove) {
        // 컨슈머 인스턴스ID 조회
        ConsumerInstanceId cid = new ConsumerInstanceId(groupName, instance);

        // 컨슈머 인스턴스 삭제인 경우 : 컨슈머 풀에서 인스턴스 제거 및 제거한 인스턴스 반환
        // 컨슈머 인스턴스 조회인 경우 : 인스턴스 반환
        final ConsumerWorker<?, ?, ?, ?> consumer = toRemove ? this.consumerManager.removeConsumer(cid) : this.consumerManager.getConsumer(cid);
        
        // 조회된 인스턴스가 없는 경우, 예외 발생
        if (consumer == null) {
            throw new ProxyException(Errors.CONSUMER_INSTANCE_NOT_FOUND);
        }
        
        // 컨슈머 인스턴스 만료 시간 증가 (컨슈머 작업 도중 만료되지 않게 하기 위함)
        consumer.updateExpiration();
        
        return consumer;
    }

    /**
     * 컨슈머 인스턴스 조회
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @return
     */
    private ConsumerWorker<?, ?, ?, ?> getConsumerInstance(String groupName, String instance) {
        return getConsumerInstance(groupName, instance, false);
    }

    /**
     * 컨슈머 인스턴스 삭제
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     */
    public void deleteConsumer(String groupName, String instance) {
        log.info("Destroying consumer [" + instance + "] in group [" + groupName + "] start...");
        final ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance, true);
        consumer.close(); // 컨슈머 인스턴스 종료
    }

    /**
     * 컨슈머 토픽 구독
     * @param groupName    // 컨슈머 그룹명
     * @param instance     // 컨슈머 인스턴스명
     * @param subscription // 컨슈머 구독 요청
     */
    public void subscribe(String groupName, String instance, ConsumerSubscriptionRequest subscription) {
        log.info("Subscribing consumer [" + instance + "] in group [" + groupName + "] start...");
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.subscribe(subscription); // 요청 토픽 구독
        }
    }

    /**
     * 컨슈머 토픽 구독 조회
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @return
     */
    public ConsumerSubscriptionResponse subscription(String groupName, String instance) {
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우, 구독 중인 토픽 목록 반환
        if (consumer != null) {
            return new ConsumerSubscriptionResponse(new ArrayList<>(consumer.subscription()));
        
        // 조회된 인스턴스가 없는 경우, 빈 배열 반환
        } else {
            return new ConsumerSubscriptionResponse(new ArrayList<>());
        }
    }

    /**
     * 컨슈머 토픽 구독 취소
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     */
    public void unsubscribe(String groupName, String instance) {
        log.info("Unsubcribing consumer [" + instance + "] in group [" + groupName + "] start...");
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.unsubscribe(); // 토픽 구독 취소
        }
    }

    /**
     * 컨슈머 오프셋 목록 커밋
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param request   // 오프셋 커밋 요청
     */
    public void commitOffsets(String groupName, String instance, final ConsumerOffsetCommitRequest request) {
        log.info("Commit offsets for consumer [" + instance + "] in group [" + groupName + "] start...");
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            try {
                // 오프셋 목록 커밋
                consumer.commitOffsets(request);
            } catch (Exception e) {
                log.error("Failed to commit offsets for consumer id: " + consumer.getInstanceId(), e);
                throw new ProxyException(Errors.COMMIT_OFFSETS_FAILED);
            } finally {
                // 컨슈머 인스턴스 만료 시간 증가
                consumer.updateExpiration();
            }
        }
    }

    /**
     * 컨슈머 오프셋 커밋 목록 조회
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param request   // 마지막으로 커밋된 오프셋 조회 요청
     * @return
     */
    public ConsumerCommittedResponse committed(String groupName, String instance, ConsumerCommittedRequest request) {
        log.info("Committed offsets for consumer " + instance + " in group " + groupName);
        ConsumerCommittedResponse response;
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우, 오프셋 커밋 목록 조회
        if (consumer != null) {
            response = consumer.committed(request);
        
        // 조회된 인스턴스가 없는 경우, 빈 배열 반환
        } else {
            response = new ConsumerCommittedResponse(new ArrayList<>());
        }
        
        return response;
    }

    /**
     * 컨슈머 파티션 수동 할당
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param assignmentRequest // 파티션 수동 할당 요청
     */
    public void assign(String groupName, String instance, ConsumerAssignmentRequest assignmentRequest) {
        log.info("Assign partitions for " + instance + " in group " + groupName);
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.assign(assignmentRequest); // 요청 파티션 수동 할당
        }
    }

    /**
     * 컨슈머 파티션 수동 할당 조회
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @return
     */
    public ConsumerAssignmentResponse assignment(String groupName, String instance) {
        log.info("Getting assignment for " + instance + " in group " + groupName);
        Vector<proxy.infra.kafka.result.TopicPartition> partitions = new Vector<>();
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            // 수동 할당된 파티션 목록 조회
            Set<TopicPartition> topicPartitions = consumer.assignment();
            
            // 토픽-파티션별 응답값 세팅
            for (TopicPartition t : topicPartitions) {
                partitions.add(new proxy.infra.kafka.result.TopicPartition(t.topic(), t.partition()));
            }
        }
        
        return new ConsumerAssignmentResponse(partitions);
    }

    /**
     * 컨슈머 패치 오프셋 업데이트
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param request   // 패치 오프셋 업데이트 요청
     */
    public void seek(String groupName, String instance, ConsumerSeekRequest request) {
        log.info("Seeking to offset " + instance + " in group " + groupName);
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.seek(request); // 패치 오프셋 업데이트
        }
    }

    /**
     * 컨슈머 초기 오프셋으로 이동
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param request   // 오프셋을 이동시킬 토픽-파티션 목록 요청
     */
    public void seekToBeginning(String groupName, String instance, ConsumerSeekToRequest request) {
        log.info("Seeking to beginning " + instance + " in group " + groupName);
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.seekToBeginning(request);
        }
    }

    /**
     * 컨슈머 최근 오프셋으로 이동
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param request   // 오프셋을 이동시킬 토픽-파티션 목록 요청
     */
    public void seekToEnd(String groupName, String instance, ConsumerSeekToRequest request) {
        log.info("Seeking to end " + instance + " in group " + groupName);
        ConsumerWorker<?, ?, ?, ?> consumer = getConsumerInstance(groupName, instance);
        // 조회된 인스턴스가 있는 경우
        if (consumer != null) {
            consumer.seekToEnd(request);
        }
    }

    /**
     * 컨슈머 읽기 작업 생성
     * @param groupName // 컨슈머 그룹명
     * @param instance  // 컨슈머 인스턴스명
     * @param consumerWorkerType // 메시지 타입별 컨슈머 워커
     * // timeoutMs: 프록시 서버가 응답에 소요하는 최대 시간
     * // -> 컨슈머 인스턴스 설정값 중 request.timeout.ms 보다 값이 작은 경우에만 적용됨
     * @param timeoutMs
     * // maxBytes: 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수
     * // -> 컨슈머 인스턴스 설정값 중 fetch.max.bytes 보다 값이 작은 경우에만 적용됨
     * @param maxBytes
     * @param callback // 메시지 컨슈밍 작업 중 상태 반환
     */
    public <KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> void readRecords(
            final String groupName,
            final String instance,
            Class<? extends ConsumerWorker<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT>> consumerWorkerType,
            final Duration timeoutMs,
            final long maxBytes,
            final ConsumerReadCallback<ClientKeyT, ClientValueT> callback) {
        
        final ConsumerWorker consumer;
        try {
            // 컨슈머 인스턴스 조회
            consumer = getConsumerInstance(groupName, instance);

        // 조회된 인스턴스가 없는 경우, 예외 발생
        } catch (ProxyException e) {
            callback.onCompletion(null, e);
            return;
        }

        // 주어진 컨슈머 워커 타입이 컨슈머 인스턴스와 호환 가능한지 여부 확인
        if (!consumerWorkerType.isInstance(consumer)) {
            callback.onCompletion(null, new ProxyException(Errors.CONSUMER_FORMAT_MISMATCH));
            return;
        }

        // 컨슈머 읽기 작업 구성
        final ConsumerReadTask<?, ?, ?, ?> task =
            new ConsumerReadTask<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT>(
                consumer, timeoutMs, maxBytes, callback);

        // 컨슈머 읽기 작업 제출
        this.consumerManager.getExecutor().submit(task);
    }
}
