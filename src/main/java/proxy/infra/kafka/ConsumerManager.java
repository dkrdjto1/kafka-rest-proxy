package proxy.infra.kafka;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import proxy.common.config.KafkaRestConfig;
import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.infra.kafka.worker.ConsumerInstanceId;
import proxy.infra.kafka.worker.ConsumerWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/*
 * Apache kafka consumer pool 관리
 */
@Getter
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class ConsumerManager implements SmartInitializingSingleton {

    @Autowired
    private KafkaRestConfig kafkaRestConfig;

    private ExecutorService executor;

    /* key = instance id, value = consumer instance */
    private ConcurrentHashMap<ConsumerInstanceId, ConsumerWorker<?, ?, ?, ?>> consumerPool;

    private final int CONSUMER_POOL_LIMIT = 25;

    /**
     * Consumer pool 초기화
     * @return
     */
    @Override
    public void afterSingletonsInstantiated() {
        // pool init
        if (ObjectUtils.isEmpty(consumerPool)) {
            consumerPool = new ConcurrentHashMap<>();
        }

        // executor init
        if (ObjectUtils.isEmpty(executor)) {
            executor = Executors.newCachedThreadPool();
        }
    }

    /**
     * add consumer instance to pool
     * @param consumerId // pool key
     * @param consumer   // pool value
     */
    public void addConsumer(ConsumerInstanceId consumerId, ConsumerWorker<?, ?, ?, ?> consumer) {
        synchronized (consumerPool) {
            // 컨슈머 풀이 가득 찬 상태인 경우, 예외 반환
            if (consumerPool.size() >= CONSUMER_POOL_LIMIT) {
                throw new ProxyException(Errors.NO_CONSUMER_POOL_RESOURCES);
            }

            // 컨슈머 풀에 이미 존재하는 인스턴스인 경우, 예외 반환
            if (consumerPool.containsKey(consumerId)) {
                throw new ProxyException(Errors.CONSUMER_ALREADY_EXISTS);
            } else {
                log.info("Add consumer [" + consumerId.getInstance() + "] in group [" + consumerId.getGroup() + "] in consumer pool");
                // 컨슈머 풀에 추가
                consumerPool.put(consumerId, consumer);
            }
        }
    }

    /**
     * get consumer instance from pool
     * @param consumerId // pool key
     * @return
     */
    public ConsumerWorker<?, ?, ?, ?> getConsumer(ConsumerInstanceId consumerId) {
        // 컨슈머 풀에 해당 인스턴스가 없는 경우, null 반환
        return consumerPool.get(consumerId);
    }

    /**
     * remove consumer instance from pool
     * @param consumerId // pool key
     * @return
     */
    public ConsumerWorker<?, ?, ?, ?> removeConsumer(ConsumerInstanceId consumerId) {
        synchronized (consumerPool) {
            log.info("Remove consumer [" + consumerId.getInstance() + "] in group [" + consumerId.getGroup() + "] from consumer pool");
            // 컨슈머 풀에 해당 인스턴스가 없는 경우, null 반환
            return consumerPool.remove(consumerId);
        }
    }
}
