package proxy.infra.kafka.task;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import proxy.common.callback.ConsumerReadCallback;
import proxy.infra.kafka.result.ConsumerRecord;
import proxy.infra.kafka.result.ConsumerRecordAndSize;
import proxy.infra.kafka.worker.ConsumerWorker;
import lombok.extern.slf4j.Slf4j;

/**
 * Apache kafka consumer read task
 */
@Slf4j
public class ConsumerReadTask<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> implements Runnable {

    // 컨슈머 워커 (메시지 타입별)
    private final ConsumerWorker<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> consumer;
    // 프록시 서버가 응답에 소요하는 최대 시간
    private final Duration requestTimeout;
    // 프록시 서버가 응답할 (ByteString, Object, JsonNode 형식으로 변환 이전의) byte[] 형식의 메시지 키, 값의 최소 바이트 수
    private final int minResponseBytes;
    // 프록시 서버가 응답할 (ByteString, Object, JsonNode 형식으로 변환 이전의) byte[] 형식의 메시지 키, 값의 최대 바이트 수
    private final long maxResponseBytes;
    // 메시지 읽기 작업 실행 후 결과/예외 반환 콜백
    private final ConsumerReadCallback<ClientKeyT, ClientValueT> callback;
    // 읽기 작업 완료 여부
    private boolean finished;
    // 읽기 작업 시작 시각
    private final Instant started;
    private final Clock clock = Clock.systemUTC();

    // 컨슈머가 읽어온 레코드 목록 (ByteString, Object, JsonNode 형식) -> 프록시 응답
    private List<ConsumerRecord<ClientKeyT, ClientValueT>> records;

    // 현재까지 읽은 메시지 키, 값의 바이트 수
    private long bytesConsumed = 0;
    // 최소 응답 바이트 수 초과 여부
    private boolean exceededMinResponseBytes = false;
    // 최대 응답 바이트 수 초과 여부
    private boolean exceededMaxResponseBytes = false;

    public ConsumerReadTask(
            ConsumerWorker<KafkaKeyT, KafkaValueT, ClientKeyT, ClientValueT> consumer,
            Duration timeoutMs,
            long maxBytes,
            ConsumerReadCallback<ClientKeyT, ClientValueT> callback) {
        
        this.consumer = consumer;
        // 요청 timeoutMs 값이 0 이하인 경우, 컨슈머 인스턴스 생성 시 추가한 request.timeout.ms 설정값 적용
        // 요청 timeoutMs 값이 0보다 큰 경우, 컨슈머 인스턴스 생성 시 추가한 request.timeout.ms 설정값과 비교하여 작은 값 적용
        this.requestTimeout = 
            timeoutMs.isNegative() || timeoutMs.isZero()
                ? Duration.ofMillis(consumer.getConsumerInstanceConfig().getRequestTimeoutMs())
                : Collections.min(Arrays.asList(timeoutMs, Duration.ofMillis(consumer.getConsumerInstanceConfig().getRequestTimeoutMs())));
        // 요청 maxBytes 값에 대해, 컨슈머 인스턴스 생성 시 추가한 fetch.max.bytes 설정값과 비교하여 작은 값 적용
        this.maxResponseBytes = Math.min(maxBytes, consumer.getConsumerInstanceConfig().getFetchMaxBytes().longValue());
        // 컨슈머 인스턴스 생성 시 추가한 fetch.min.bytes 설정값이 0보다 작은 경우, 제한을 두지 않음.
        // 그외의 경우, 컨슈머 인스턴스 생성 시 추가한 fetch.min.bytes 설정값 적용
        this.minResponseBytes = 
            consumer.getConsumerInstanceConfig().getFetchMinBytes() < 0 
                ? Integer.MAX_VALUE 
                : consumer.getConsumerInstanceConfig().getFetchMinBytes();

        this.callback = callback;
        this.finished = false; // 읽기 작업 완료하지 않음.

        started = clock.instant(); // 현재 시각부터 읽기 작업 시작
    }

    @Override
    public void run() {
        try {
            log.info("Executing consumer read task ({})", this);

            // 1) 프록시 서버가 응답에 소요할 시간이 아직 남았고,
            // 2) 최대 응답 바이트 수에 아직 도달하지 않았고,
            // 3) 최소 응답 바이트 수를 채우지 못한 경우
            // -> 계속 읽기 작업 시도
            while (!isDone()) {
                // 읽기 작업 수행
                doPartialRead();

                // 컨슈머 인스턴스 만료 시간 증가
                consumer.updateExpiration();
            }
            
            log.info("Finished executing consumer read task ({})", this);

        } catch (Exception e) {
            log.info(
                "Failed to read records from consumer {} while executing read task ({}). {}",
                consumer.getInstanceId().toString(),
                this,
                e);
            
            // 예외 발생 시, 오류 반환
            callback.onCompletion(null, e);
        }
    }

    /**
     * 읽기 작업 수행
     */
    private void doPartialRead() {
        try {
            // 프록시 서버가 응답할 레코드 목록 초기화
            if (records == null) records = new Vector<>();

            // 최소/최대 응답 바이트 수 한도 내에서, 응답할 레코드 추가
            addRecords();

            // 컨슈머가 리더 파티션으로부터 레코드를 가져왔을 경우에만 로그 출력
            if (records.size() > 0 && bytesConsumed > 0) {
                log.info(
                    "ConsumerReadTask exiting read with id={} records={} bytes={}, backing off if not complete",
                    this,
                    records.size(),
                    bytesConsumed);
            }

            // 현재 시각
            Instant now = clock.instant();
            // 경과 시간 (읽기 작업 시작 시각 ~ 현재 시각)
            Duration elapsed = Duration.between(started, now);

            // 프록시 서버가 응답에 소요할 최대 시간을 초과한 경우, true
            boolean requestTimedOut = elapsed.compareTo(requestTimeout) >= 0;

            // 1) 프록시 서버가 응답에 소요할 최대 시간을 초과했거나
            // 2) 최대 응답 바이트 수를 초과했거나
            // 3) 최소 응답 바이트 수를 초과한 경우
            // -> 읽기 작업 종료 및 정상 응답
            if (requestTimedOut || exceededMaxResponseBytes || exceededMinResponseBytes) {
                log.info("Finishing ConsumerReadTask id={} requestTimedOut={} exceededMaxResponseBytes={} exceededMinResponseBytes={}",
                    this,
                    requestTimedOut,
                    exceededMaxResponseBytes,
                    exceededMinResponseBytes);
                
                finish(); // 읽기 작업 정상 종료
            }
        } catch (Exception e) {
            finish(e); // 예외 발생 시, 오류 전달
            log.info("Unexpected exception in consumer read task id={} ...", this, e);
        }
    }

    /**
     * 메시지 컨슈밍 (최소/최대 응답 바이트 수 한도 내)
     */
    private void addRecords() {
        // 1) 최대 응답 바이트 수에 아직 도달하지 않았고,
        // 2) 컨슈머가 읽어온 레코드 목록이 존재하는 한
        // -> 계속 프록시 응답에 레코드 추가 시도
        while (!exceededMaxResponseBytes && consumer.hasNext()) {
            // 다른 스레드에서의 동일한 컨슈머 인스턴스에 대한 값 변경 방지
            synchronized (consumer) {
                if (consumer.hasNext()) {
                    // 프록시 응답에 레코드 추가
                    maybeAddRecord();
                }
            }
        }
    }

    /**
     * 읽어온 메시지 목록(프록시 응답)에 레코드 추가
     */
    private void maybeAddRecord() {
        // 컨슈머가 읽어온 레코드 목록에서 레코드 하나 조회 후,
        // 프록시 서버 응답을 위해 레코드 형식 변환 (ex. avro: Object -> JsonNode)
        // 형식 변환된 레코드 & 메시지 키, 값 바이트 사이즈 응답값 맵핑
        ConsumerRecordAndSize<ClientKeyT, ClientValueT> recordAndSize = consumer.createConsumerRecord(consumer.peek());
        
        // 메시지 키, 값 바이트 사이즈 합
        long roughMsgSize = recordAndSize.getSize();

        // 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수를 초과할 경우,
        // 응답에 레코드를 추가하지 않고 종료
        if (bytesConsumed + roughMsgSize >= maxResponseBytes) {
            this.exceededMaxResponseBytes = true;
            return;
        }

        // 읽어온 레코드 목록(프록시 응답)에 레코드 추가
        records.add(recordAndSize.getRecord());

        // 응답에 추가한 레코드는, 컨슈머가 읽어온 레코드 목록에서 제거
        consumer.next();

        // 현재까지 읽은 메시지 키, 값의 바이트 수 + 응답에 추가한 메시지의 키, 값 바이트 사이즈를 더함.
        bytesConsumed += roughMsgSize;

        // 프록시 서버가 응답할 메시지 키, 값의 최소 바이트 수를 초과한 경우, 플래그 값 재정의
        if (!exceededMinResponseBytes && bytesConsumed > minResponseBytes) {
            this.exceededMinResponseBytes = true;
        }
    }

    /**
     * 읽기 작업 완료 여부 반환 (true: 완료됨)
     * @return
     */
    private boolean isDone() {
        return finished;
    }

    /**
     * 읽기 작업 정상 종료
     */
    private void finish() {
        finish(null);
    }

    /**
     * 읽기 작업 종료
     * @param e // 예외 발생 시, 오류 내용
     */
    private void finish(Exception e) {
        log.info("Finishing ConsumerReadTask id={}", this, e);
        
        try {
            // 작업 성공 시, 읽어온 메시지 목록 전달
            // 작업 실패 시, 예외 전달
            callback.onCompletion((e == null) ? records : null, e);
        } catch (Throwable t) {
            log.info("Consumer read callback threw an unhandled exception id={} exception={} ...", this, e);
        }

        // 읽기 작업 완료 처리
        finished = true;
    }
}
