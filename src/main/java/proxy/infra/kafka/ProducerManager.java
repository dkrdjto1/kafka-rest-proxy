package proxy.infra.kafka;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;

import proxy.common.config.KafkaClientConfig;
import proxy.infra.kafka.result.ProduceResult;
import proxy.infra.kafka.worker.ProducerWorker;
import lombok.extern.slf4j.Slf4j;

/*
 * Apache kafka producer pool 관리
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class ProducerManager implements SmartInitializingSingleton {

    @Autowired
    private KafkaClientConfig kafkaClientConfig;

    private final String KAFKA_REST_PROXY_PRODUCER = "producer";

    private static Map<String, ProducerWorker> producerPool = null;

    /**
     * producer pool 초기화
     */
    @Override
    public void afterSingletonsInstantiated() {
        // pool init
        if (ObjectUtils.isEmpty(producerPool)) {
            producerPool = new HashMap<>();
        }

        // producer init
        if (ObjectUtils.isEmpty(producerPool.get(KAFKA_REST_PROXY_PRODUCER))) {
            // producer configs
            var props = this.kafkaClientConfig.getProducerConfig(KAFKA_REST_PROXY_PRODUCER);
            props = this.kafkaClientConfig.addSecurityConfig(props);

            producerPool.put(KAFKA_REST_PROXY_PRODUCER, new ProducerWorker(props));
        }
    }

    /**
     * producer stop
     */
    public void closeAll() {
        for (Entry<String, ProducerWorker> entry : producerPool.entrySet()) {
            if (!ObjectUtils.isEmpty(entry.getValue()))
                entry.getValue().stop();
        }
        
        producerPool.clear();
    }

    /**
     * 데이터 전송
     * @param clusterId   // 클러스터ID
     * @param topicName   // 토픽명
     * @param partitionId // 파티션ID
     * @param headers     // 레코드 헤더값
     * @param key         // 메시지 키
     * @param value       // 메시지 값
     * @param timestamp   // 프로듀싱 시작 시각
     */
    public CompletableFuture<ProduceResult> produce(
            String clusterId,
            String topicName,
            Optional<Integer> partitionId,
            Multimap<String, Optional<ByteString>> headers,
            Optional<ByteString> key,
            Optional<ByteString> value,
            Instant timestamp) {
        
        // 프로듀서 조회
        var client = producerPool.get(KAFKA_REST_PROXY_PRODUCER);
        CompletableFuture<ProduceResult> result = new CompletableFuture<>();
        try {
            // 프로듀서가 존재하지 않는 경우, 재시작
            client = this.restartProducer(client);
            // 데이터 프로듀싱
            result = client.produce(clusterId, topicName, partitionId, headers, key, value, timestamp);
        } catch (InterruptedException e) {
            log.error("", e);
            Thread.currentThread().interrupt();
        }

        return result;
    }

    /**
     * producer=null 인 경우, 재시작
     * @param client
     * @return {@link ProducerWorker}
     * @throws InterruptedException
     */
    private ProducerWorker restartProducer(ProducerWorker client) throws InterruptedException {
        if (client == null) {
            this.removeProducerWorker(KAFKA_REST_PROXY_PRODUCER);
            afterSingletonsInstantiated();
            log.warn("Proxy data producer is restarting");
            Thread.sleep(10000l);
            client = producerPool.get(KAFKA_REST_PROXY_PRODUCER);
        }

        return client;
    }

    /**
     * 프로듀서 워커 정지 및 삭제
     * @param key
     */
    private void removeProducerWorker(String key) {
        var client = producerPool.get(key);
        if (client != null) {
            client.stop();
            producerPool.remove(key);

            try {
                Thread.sleep(10000l);
            } catch (InterruptedException e) {
                log.error("", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}

