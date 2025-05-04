package io.spitha.felice.common.config;

import org.springframework.util.ObjectUtils;

/**
 * kafka protocol enum
 */
public enum KafkaProtocol {
    PLAINTEXT,
    SASL_PLAINTEXT,
    SASL_SSL,
    SSL,

    UNKNOWN,
    NULL
    ;

    public static KafkaProtocol fromString(String protocol) {
        if (ObjectUtils.isEmpty(protocol)) return NULL;
        try {
            return KafkaProtocol.valueOf(protocol.toUpperCase());
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
