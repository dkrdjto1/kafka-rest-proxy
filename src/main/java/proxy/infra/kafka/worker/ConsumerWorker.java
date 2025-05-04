package proxy.infra.kafka.worker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import proxy.api.request.ConsumerAssignmentRequest;
import proxy.api.request.ConsumerCommittedRequest;
import proxy.api.request.ConsumerOffsetCommitRequest;
import proxy.api.request.ConsumerSeekRequest;
import proxy.api.request.ConsumerSeekToRequest;
import proxy.api.request.ConsumerSubscriptionRequest;
import proxy.api.response.ConsumerCommittedResponse;
import proxy.common.config.ConsumerInstanceConfig;
import proxy.infra.kafka.result.ConsumerRecordAndSize;
import proxy.infra.kafka.result.TopicPartitionOffset;
import lombok.Getter;

/**
 * Apache kafka consumer works
 */
@Getter
public abstract class ConsumerWorker<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> {

    private ConsumerInstanceId instanceId; // 컨슈머 인스턴스ID
    private Consumer<KafkaKeyT, KafkaValueT> consumer; // 컨슈머 인스턴스
    private final ConsumerInstanceConfig consumerInstanceConfig; // 컨슈머 인스턴스 설정값

    private final Clock clock = Clock.systemUTC();
    private final Duration consumerInstanceTimeout = Duration.ofMillis(300000); // 만료 시간 증가 (5m)

    // 컨슈머가 읽어온 레코드 목록 (byte[], byte[], Object 형식)
    private final Queue<ConsumerRecord<KafkaKeyT, KafkaValueT>> consumerRecords = new ArrayDeque<>();

    volatile Instant expiration; // 컨슈머 인스턴스 만료 시각
    private final Object expirationLock = new Object(); // 데드락 방지

    public ConsumerWorker(
            ConsumerInstanceConfig instanceConfig,
            ConsumerInstanceId instanceId,
            Consumer<KafkaKeyT, KafkaValueT> consumer) {
        this.consumerInstanceConfig = instanceConfig;
        this.instanceId = instanceId;
        this.consumer = consumer;
        this.expiration = clock.instant().plus(consumerInstanceTimeout); // 인스턴스 만료 시간 증가
    }

    /**
     * 여러 메시지 포맷에 따라, 컨슈머가 읽어온 레코드 형식 변환 (JSON, BINARY, SCHEMA)
     * @param record
     * @return
     */
    public abstract ConsumerRecordAndSize<ClientKeyT, ClientValueT> createConsumerRecord(ConsumerRecord<KafkaKeyT, KafkaValueT> record);

    /**
     * 컨슈머 인스턴스 만료 여부 반환 (true: 만료됨)
     * @param now
     * @return
     */
    public boolean expired(Instant now) {
        synchronized (this.expirationLock) {
            return !expiration.isAfter(now);
        }
    }

    /**
     * 컨슈머 인스턴스 만료 시간 증가 (현재 시각 + 5분)
     */
    public void updateExpiration() {
        synchronized (this.expirationLock) {
            this.expiration = clock.instant().plus(consumerInstanceTimeout);
        }
    }

    /**
     * 컨슈머 인스턴스 종료
     */
    public synchronized void close() {
        if (consumer != null) {
            consumer.close();
        }
        consumer = null;
    }

    /**
     * 요청 토픽 구독 (파티션 동적 할당)
     * @param subscription
     */
    public synchronized void subscribe(ConsumerSubscriptionRequest subscription) {
        // 요청 토픽이 없는 경우 종료
        if (subscription == null) {
            return;
        }

        // 컨슈머 인스턴스가 존재하는 경우
        if (consumer != null) {
            // 토픽 목록이 주어진 경우
            if (subscription.getTopics() != null) {
                // 토픽 구독
                consumer.subscribe(subscription.getTopics());

            // 토픽 패턴이 주어진 경우
            } else if (subscription.getTopicPattern() != null) {
                // 토픽 패턴 분석
                Pattern topicPattern = Pattern.compile(subscription.getTopicPattern());
                // 토픽 구독
                consumer.subscribe(topicPattern);
            }
        }
    }

    /**
     * 구독 중인 토픽 목록 조회
     * @return
     */
    public synchronized Set<String> subscription() {
        Set<String> currSubscription = null;
        
        // 컨슈머 인스턴스가 존재하는 경우
        if (consumer != null) {
            // 현재 구독 중인 토픽 목록 조회
            currSubscription = consumer.subscription();
        }

        return currSubscription;
    }

    /**
     * 현재 구독 중인 모든 토픽 목록에 대해 구독 취소
     */
    public synchronized void unsubscribe() {
        // 컨슈머 인스턴스가 존재하는 경우
        if (consumer != null) {
            // 모든 토픽 구독 취소
            consumer.unsubscribe();
        }
    }

    /**
     * <pre>
     * 컨슈머가 읽어온 레코드 목록 존재 여부 반환 (true: 존재함)
     * - 레코드 목록이 비어있는 경우, 메시지 컨슈밍
     * </pre>
     * @return
     */
    public synchronized boolean hasNext() {
        // 읽어온 레코드 목록에 레코드가 존재하는 경우, true 반환
        if (hasNextCached()) {
            return true;
        }

        // 읽어온 레코드 목록이 비어있는 경우, 메시지 컨슈밍
        addConsumerRecords();

        // 읽어온 레코드 목록 다시 확인
        return hasNextCached();
    }

    /**
     * 컨슈머가 읽어온 레코드 목록 존재 여부 반환 (true: 존재함)
     * @return
     */
    private synchronized boolean hasNextCached() {
        return !consumerRecords.isEmpty();
    }

    /**
     * 메시지 컨슈밍
     */
    private synchronized void addConsumerRecords() {
        // 메시지 컨슈밍
        ConsumerRecords<KafkaKeyT, KafkaValueT> polledRecords = consumer.poll(Duration.ofMillis(0)); // 100
        
        // 컨슈머가 읽어온 레코드 목록 (byte[], byte[], Object 형식) 에 추가
        for (ConsumerRecord<KafkaKeyT, KafkaValueT> consumerRecord : polledRecords) {
            consumerRecords.add(consumerRecord);
        }
    }

    /**
     * 컨슈머가 읽어온 레코드 목록에서 하나 조회 및 제거
     * @return
     */
    public synchronized ConsumerRecord<KafkaKeyT, KafkaValueT> next() {
        return consumerRecords.poll();
    }

    /**
     * 컨슈머가 읽어온 레코드 목록에서 하나 조회
     * @return
     */
    public synchronized ConsumerRecord<KafkaKeyT, KafkaValueT> peek() {
        return consumerRecords.peek();
    }

    /**
     * 컨슈머 오프셋 목록 커밋
     * @param offsetCommitRequest // 오프셋 커밋 요청
     * @return
     */
    public synchronized void commitOffsets(ConsumerOffsetCommitRequest offsetCommitRequest) {
        // 요청 오프셋 정보가 없는 경우, 컨슈머가 지금까지 읽은 모든 레코드를 커밋
        if (offsetCommitRequest == null) consumer.commitAsync();
        // 요청 오프셋 정보가 있는 경우
        else {
            Map<TopicPartition, OffsetAndMetadata> offsetMap = new HashMap<TopicPartition, OffsetAndMetadata>();

            // 커밋할 오프셋 목록 세팅
            for (TopicPartitionOffset t : offsetCommitRequest.getOffsets()) {
                // 토픽명, 파티션ID, 오프셋 번호 세팅
                offsetMap.put(
                    new TopicPartition(t.getTopic(), t.getPartition()),
                    new OffsetAndMetadata(t.getOffset() + 1));
            }

            // 오프셋 목록 커밋
            consumer.commitSync(offsetMap);
        }
    }

    /**
     * 컨슈머 오프셋 커밋 목록 조회
     * @param request // 커밋된 오프셋 조회 요청
     * @return
     */
    public synchronized ConsumerCommittedResponse committed(ConsumerCommittedRequest request) {
        Vector<TopicPartitionOffset> offsets = new Vector<>();
        Set<TopicPartition> partitions = new HashSet<>();

        // 컨슈머 인스턴스가 존재하는 경우
        if (consumer != null) {
            // 요청 토픽-파티션 목록 List -> Set 변경
            for (var t : request.getPartitions()) {
                TopicPartition partition = new TopicPartition(t.getTopic(), t.getPartition());
                partitions.add(partition);
            }

            // 오프셋 커밋 목록 조회
            Map<TopicPartition, OffsetAndMetadata> result = consumer.committed(partitions);

            // 응답값 세팅
            if (result != null) {
                for (TopicPartition key : result.keySet()) {
                    // 토픽명, 파티션ID, 오프셋 번호 세팅
                    offsets.add(
                        new TopicPartitionOffset(
                            key.topic(),
                            key.partition(),
                            result.get(key).offset()));
                }
            }
        }

        return new ConsumerCommittedResponse(offsets);
    }

    /**
     * 컨슈머 파티션 수동 할당
     * @param assignmentRequest // 파티션 수동 할당 요청
     */
    public synchronized void assign(ConsumerAssignmentRequest assignmentRequest) {
        // 요청 파티션이 존재하는 경우
        if (assignmentRequest != null) {
            Vector<TopicPartition> topicPartitions = new Vector<TopicPartition>();

            // 수동 할당할 파티션 목록 세팅
            for (proxy.infra.kafka.result.TopicPartition t : assignmentRequest.getPartitions()) {
                // 토픽명, 파티션ID 세팅
                topicPartitions.add(new TopicPartition(t.getTopic(), t.getPartition()));
            }
            
            // 파티션 수동 할당
            consumer.assign(topicPartitions);
        }
    }

    /**
     * 컨슈머 파티션 수동 할당 조회
     * @return
     */
    public synchronized Set<TopicPartition> assignment() {
        Set<TopicPartition> currAssignment = null;

        // 컨슈머 인스턴스가 존재하는 경우
        if (consumer != null) {
            // 파티션 수동 할당 조회
            currAssignment = consumer.assignment();
        }
        
        return currAssignment;
    }

    /**
     * 컨슈머가 다음 메시지 폴링 시 사용할 패치 오프셋 업데이트
     * @param request
     */
    public synchronized void seek(ConsumerSeekRequest request) {
        // 요청이 없는 경우, 종료
        if (request == null) return;

        // 패치 오프셋 목록 업데이트
        for (TopicPartitionOffset partition : request.getOffsets()) {
            consumer.seek(new TopicPartition(partition.getTopic(), partition.getPartition()), partition.getOffset());
        }
    }

    /**
     * 요청 토픽-파티션 목록에 대해 초기 오프셋으로 이동
     * @param request
     */
    public synchronized void seekToBeginning(ConsumerSeekToRequest request) {
        // 요청 토픽-파티션 목록이 있는 경우
        if (request != null) {
            Vector<TopicPartition> topicPartitions = new Vector<TopicPartition>();

            // 오프셋을 이동시킬 토픽-파티션 목록 세팅
            for (proxy.infra.kafka.result.TopicPartition t : request.getPartitions()) {
                topicPartitions.add(new TopicPartition(t.getTopic(), t.getPartition()));
            }

            // 초기 오프셋으로 이동
            consumer.seekToBeginning(topicPartitions);
        }
    }

    /**
     * 요청 토픽-파티션 목록에 대해 최근 오프셋으로 이동
     * @param request
     */
    public synchronized void seekToEnd(ConsumerSeekToRequest request) {
        // 요청 토픽-파티션 목록이 있는 경우
        if (request != null) {
            Vector<TopicPartition> topicPartitions = new Vector<TopicPartition>();

            // 오프셋을 이동시킬 토픽-파티션 목록 세팅
            for (proxy.infra.kafka.result.TopicPartition t : request.getPartitions()) {
                topicPartitions.add(new TopicPartition(t.getTopic(), t.getPartition()));
            }

            // 최근 오프셋으로 이동
            consumer.seekToEnd(topicPartitions);
        }
    }
}
