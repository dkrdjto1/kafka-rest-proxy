package io.spitha.felice.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * {@code kafka-rest.properties} 설정값 목록
 */
@Getter
@Component
public class KafkaRestConfig {
    
    @Value("${bootstrap.servers}")
    private String bootstrapServers;

    @Value("${client.protocol:}")
    private String protocol;

    @Value("${client.sasl-mechanism:}")
    private String saslMechanism;

    @Value("${client.username:}")
    private String username;

    @Value("${client.password:}")
    private String password;

    @Value("${client.ssl.truststore-location:}")
    private String truststoreLocation;

    @Value("${client.ssl.truststore-password:}")
    private String truststorePassword;

    @Value("${client.ssl.truststore-type:}")
    private String truststoreType;

    @Value("${client.ssl.keystore-location:}")
    private String keystoreLocation;

    @Value("${client.ssl.keystore-password:}")
    private String keystorePassword;

    @Value("${client.ssl.keystore-type:}")
    private String keystoreType;

    @Value("${client.ssl.key-password:}")
    private String keyPassword;

    @Value("${client.ssl.endpoint-identification-algorithm:}")
    private String endpointIdentificationAlgorithm;

    @Value("${schema.registry.url:}")
    private String schemaRegistryUrl;

    @Value("${schema.registry.username:}")
    private String schemaRegistryUsername;

    @Value("${schema.registry.password:}")
    private String schemaRegistryPassword;

    @Value("${ignore-ssl-validation:false}")
    private boolean ignoreSslValidation;
}

