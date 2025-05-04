package proxy.infra.kafka.worker;

import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;

import proxy.infra.kafka.result.ProduceResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Apache kafka producer works
 */
@Slf4j
public class ProducerWorker {
    private final Properties props;
    
    private Producer<byte[], byte[]> producer;

    private CountDownLatch latch;

    /**
     * 프로듀서 워커 시작
     * @param producerProps
     */
    public ProducerWorker(Properties props) {
        this.props = props;

        // Producer 클라이언트 구성
        producer = new KafkaProducer<byte[], byte[]>(this.props);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            producer.close();
            log.warn("Proxy data producer shutdown complete.");
        }));
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
     * @return
     */
    public CompletableFuture<ProduceResult> produce(
            String clusterId,
            String topicName,
            Optional<Integer> partitionId,
            Multimap<String, Optional<ByteString>> headers,
            Optional<ByteString> key,
            Optional<ByteString> value,
            Instant timestamp) {
        
        CompletableFuture<ProduceResult> result = new CompletableFuture<>();
        log.debug("Producing to kafka start...");
        // 메시지 전송
        producer.send(
            new ProducerRecord<>(
                topicName,
                partitionId.orElse(null),
                timestamp.toEpochMilli(),
                key.map(ByteString::toByteArray).orElse(null),
                value.map(ByteString::toByteArray).orElse(null),
                headers.entries().stream()
                    .map(
                        header ->
                            new RecordHeader(
                                header.getKey(),
                                header.getValue().map(ByteString::toByteArray).orElse(null)))
                    .collect(Collectors.toList())),
            (metadata, exception) -> {
                // 메시지 전송 실패 시
                if (exception != null) {
                    log.debug("Received exception from kafka", exception);
                    result.completeExceptionally(exception);
                // 메시지 전송 성공 시
                } else {
                    log.debug("Received response from kafka");
                    result.complete(ProduceResult.fromRecordMetadata(metadata, Instant.now()));
                }
            });
        
        return result;
    }

    /**
     * 데이터 워커 종료
     */
    public void stop() {
        try {
            // 처리 중인 데이터가 있는지 확인 후 soft closing
            if (latch != null) {
                latch.await(10, TimeUnit.SECONDS);
            }

            if (producer != null) {
                producer.close();
                producer.notify();
            }

            log.info("Proxy producer finish.");

        } catch (InterruptedException e) {
            log.error("Produce worker stop()- [{}]", e);
            Thread.currentThread().interrupt();
        } catch (IllegalMonitorStateException e) {
            log.warn("Producer worker stopped.");
        } catch (Exception e) {
            log.error("Produce worker stop()- [{}]", e);
        }
    }

}
