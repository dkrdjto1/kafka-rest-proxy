package io.spitha.felice.common.config;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import io.spitha.felice.common.deserializer.AvroDeserializer;
import io.spitha.felice.common.deserializer.JsonSchemaDeserializer;
import io.spitha.felice.common.deserializer.ProtobufDeserializer;

@Component
public class KafkaClientConfig {
    
    @Autowired
    private KafkaRestConfig kafkaRestConfig;

    public static final String PRODUCER_ID_PREFIX = "felice-rest-producer-";

    /**
     * producer config
     * @param producerId
     * @return
     */
    public Properties getProducerConfig(String producerId) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, PRODUCER_ID_PREFIX + producerId);
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaRestConfig.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        // 프로듀서 ACKS 설정 - replica에 정상적으로 복제가 이루어졌는지 확인하기 위한 설정값
        // * 0: 전송한 메시지에 대해 카프카 브로커로부터 아무런 응답을 받지 않음. 이 경우 응답에 포함되는 메시지의 오프셋은 항상 [-1]로 반환되며, [retries]를 설정할 필요가 없음.
        // * 1: 전송한 메시지를 카프카 리더 파티션이 정상적으로 받아서 로그로 기록한 후 응답함. (다른 팔로워들의 복제상태는 확인하지 않음)
        // * all: (= -1) 전송한 메시지에 대해 리더와 모든 팔로워들이 받았는지 확인 후 응답함.
        producerProps.put(ProducerConfig.ACKS_CONFIG, "1");
        
        return producerProps;
    }

    /**
     * consumer config
     * @param groupName
     * @param instanceConfig
     * @return
     */
    public Properties getConsumerConfig(String groupName, ConsumerInstanceConfig instanceConfig) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupName);
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaRestConfig.getBootstrapServers());
        
        /* 요청값이 있는 경우, 요청값 우선 적용 */

        // true: 컨슈머 오프셋이 주기적으로 백그라운드에서 자동 커밋됨.
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, 
            instanceConfig.getEnableAutoCommit() != null ? instanceConfig.getEnableAutoCommit() : false);
        // 카프카에 초기 오프셋이 없거나 현재 오프셋이 서버에 더 이상 존재하지 않는 경우 수행 가능
        // earliest: 오프셋을 가장 처음 오프셋으로 자동 재설정
        // latest: 오프셋을 최신 오프셋으로 자동 재설정
        // none: 소비자 그룹에 대한 이전 오프셋을 찾을 수 없는 경우 소비자에게 예외를 던짐.
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, 
            instanceConfig.getAutoOffsetReset() != null ? instanceConfig.getAutoOffsetReset() : "earliest");
        // 최대 요청 크기에 도달하지 않은 경우, 클라이언트가 요청의 응답을 기다리는 최대 메시지 대기 시간
        // 제한 시간이 경과하기 전에 응답을 받지 못하면 클라이언트는 필요한 경우 요청을 다시 보내거나 재시도가 모두 소진되면 요청을 실패로 처리
        // (Default value: 30000)
        consumerProps.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 
            instanceConfig.getRequestTimeoutMs() != null ? instanceConfig.getRequestTimeoutMs() : 30000);
        // fetch 요청에 대해 서버가 응답할 최소 데이터 크기
        // fetch 설정 값만큼 데이터가 충분하지 않은 경우, 해당 데이터가 누적될 때까지 기다림.
        // 1byte로 설정하면 fetch 요청이 타임아웃되는 즉시 fetch 수행
        // (Unit: bytes, Default value: 1)
        consumerProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 
            instanceConfig.getFetchMinBytes() != null ? instanceConfig.getFetchMinBytes() : 1);
        // fetch 요청에 의해 받을 수 있는 최대 데이터 크기
        // fetch message 단건의 크기가 이 설정값보다 큰 경우, [RecordTooLargeException] 발생
        // (Unit: bytes, Default value: 5000000 (=5MiB))
        consumerProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 64 * 1024 * 1024);
        // 한 번의 fetch 요청으로 가져올 수 있는 최대 메시지 개수 (Default value: 500)
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);
        // fetch 요청에 대해 서버에서 응답할 데이터가 [fetch.min.bytes]에 미치지 못한 경우 응답을 기다리는 최대 시간
        // [fetch.min.bytes] 지정한 크기만큼 데이터가 충분하지 않은 경우, 서버는 [fetch.max.wait.ms]에서 설정한 시간 동안 fetch 요청을 차단함.
        // (Unit: milliseconds, Default value: 500)
        consumerProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        // 이 설정값 이내에 poll() 메서드가 호출되지 않으면, broker는 해당 consumer가 죽었다고 판단하고 해당 consumer의 파티션 할당을 다른 consumer에게 재할당함.
        // 처리 시간이 너무 오래걸리는 경우, MAX_POLL_RECORDS_CONFIG 값을 줄이거나, MAX_POLL_INTERVAL_MS_CONFIG 시간을 늘리는 것이 좋음.
        // (Unit: milliseconds, Default value: 300000 (=5m))
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000);

        // 메시지 포맷에 따라 설정값 구분
        switch (instanceConfig.getFormat()) {
            case AVRO:
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class);
                break;
            case JSONSCHEMA:
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, JsonSchemaDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSchemaDeserializer.class);
                break;
            case PROTOBUF:
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ProtobufDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtobufDeserializer.class);
                break;
            case JSON:
            case BINARY:
            default:
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        }

        return consumerProps;
    }

    /**
     * sasl config (username, password)
     * @param props
     * @param protocol
     * @param saslMechanism
     * @param username
     * @param password
     * @return
     */
    private Properties addSaslConfig(Properties props, String protocol, String saslMechanism, String username, String password) {
        // protocol=SASL_PLAINTEXT/SASL_SSL 인 경우
        if (KafkaProtocol.fromString(protocol).equals(KafkaProtocol.SASL_PLAINTEXT)
                || KafkaProtocol.fromString(protocol).equals(KafkaProtocol.SASL_SSL)) {
            
            // sasl.mechanism=PLAIN 인 경우
            if (SaslMechanism.fromString(saslMechanism).equals(SaslMechanism.PLAIN)) {
                props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required\n username=\"" + username + "\"\n password=\"" + password + "\"; ");

            // sasl.mechanism=SCRAM-SHA-256/SCRAM-SHA-512 인 경우
            } else if (SaslMechanism.fromString(saslMechanism).equals(SaslMechanism.SCRAM_SHA_256)
                    || SaslMechanism.fromString(saslMechanism).equals(SaslMechanism.SCRAM_SHA_512)) {
                props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.scram.ScramLoginModule required\n username=\"" + username + "\"\n password=\"" + password + "\"; ");
            }
        }

        return props;
    }

    /**
     * ssl config
     * @param props
     * @param truststoreLocation
     * @param truststorePassword
     * @param truststoreType
     * @param keystoreLocation
     * @param keystorePassword
     * @param keystoreType
     * @param keyPassword
     * @param endpointIdentificationAlgorithm
     * @return
     */
    private Properties addSslConfig(Properties props, String truststoreLocation, String truststorePassword, String truststoreType,
            String keystoreLocation, String keystorePassword, String keystoreType, String keyPassword, String endpointIdentificationAlgorithm) {
        
        // truststore config
        if (StringUtils.hasText(truststoreLocation) && StringUtils.hasText(truststorePassword) && StringUtils.hasText(truststoreType)) {
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);
        }

        // keystore config
        if (StringUtils.hasText(keystoreLocation) && StringUtils.hasText(keystorePassword) && StringUtils.hasText(keystoreType) && StringUtils.hasText(keyPassword)) {
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keystoreType);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        }

        // endpoint identification algorithm config
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);

        return props;
    }

    /**
     * security config
     * @param props
     * @return
     */
    public Properties addSecurityConfig(Properties props) {
        // add protocol config
        if (StringUtils.hasText(this.kafkaRestConfig.getProtocol())) {
            props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, this.kafkaRestConfig.getProtocol());
        }

        // add sasl mechanism config
        if (StringUtils.hasText(this.kafkaRestConfig.getSaslMechanism())) {
            props.put(SaslConfigs.SASL_MECHANISM, this.kafkaRestConfig.getSaslMechanism());
        }

        // sasl configs - username, password
        if (!ObjectUtils.isEmpty(this.kafkaRestConfig.getUsername()) && !ObjectUtils.isEmpty(this.kafkaRestConfig.getPassword())) {
            props = this.addSaslConfig(
                props, 
                this.kafkaRestConfig.getProtocol(), 
                this.kafkaRestConfig.getSaslMechanism(), 
                this.kafkaRestConfig.getUsername(), 
                this.kafkaRestConfig.getPassword());
        }

        // TODO: sasl.mechanism=OAUTHBEARER

        // TODO: sasl.mechanism=GSSAPI

        // ssl configs
        if (this.kafkaRestConfig.getProtocol().equals(KafkaProtocol.SSL.name()) || this.kafkaRestConfig.getProtocol().equals(KafkaProtocol.SASL_SSL.name())) {
            props = this.addSslConfig(
                props,
                this.kafkaRestConfig.getTruststoreLocation(),
                this.kafkaRestConfig.getTruststorePassword(),
                this.kafkaRestConfig.getTruststoreType(),
                this.kafkaRestConfig.getKeystoreLocation(),
                this.kafkaRestConfig.getKeyPassword(),
                this.kafkaRestConfig.getKeystoreType(),
                this.kafkaRestConfig.getKeyPassword(),
                this.kafkaRestConfig.getEndpointIdentificationAlgorithm());
        }

        return props;
    }
}

