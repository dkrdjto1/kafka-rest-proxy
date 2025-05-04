package proxy.schedular;

import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import proxy.infra.kafka.ConsumerManager;
import proxy.infra.kafka.worker.ConsumerWorker;
import lombok.extern.slf4j.Slf4j;

/**
 * 컨슈머 인스턴스 만료 체크 스케쥴러
 */
@Component
@Slf4j
public class ExpirationSchedular {

    @Autowired
    private ConsumerManager consumerManager;

    private final Clock clock = Clock.systemUTC();

    // 1초마다 만료된 컨슈머 인스턴스가 있는지 확인
	@Scheduled(fixedDelay = 1000)
	public void watch() {
        // 현재 시각
        Instant now = clock.instant();

        // 컨슈머 풀 조회
        Iterator<ConsumerWorker<?, ?, ?, ?>> itr = this.consumerManager.getConsumerPool().values().iterator();

        // 컨슈머 인스턴스마다 확인
        while (itr.hasNext()) {
            // 컨슈머 인스턴스 조회
            final ConsumerWorker<?, ?, ?, ?> consumer = itr.next();

            // 컨슈머 인스턴스가 존재하고, 만료된 경우
            if (consumer != null && consumer.expired(now)) {
                log.info("Removing the expired consumer [{}]", consumer.getInstanceId()); // debug
                
                // 컨슈머 풀에서 만료된 인스턴스 제거
                this.consumerManager.removeConsumer(consumer.getInstanceId());

                // 컨슈머 인스턴스 종료 작업 제출
                this.consumerManager.getExecutor().submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            consumer.close();
                        }
                    });
            }
        }
    }
}
