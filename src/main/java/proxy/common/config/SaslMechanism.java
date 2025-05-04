package io.spitha.felice.common.config;

import org.springframework.util.ObjectUtils;

/**
 * sasl mechanism enum
 */
public enum SaslMechanism {
    PLAIN,
    SCRAM_SHA_256,
    SCRAM_SHA_512,
    OAUTHBEARER,
    GSSAPI,

    UNKNOWN,
    NULL
    ;

    public static SaslMechanism fromString(String protocol) {
        if (ObjectUtils.isEmpty(protocol)) return NULL;
        try {
            return SaslMechanism.valueOf(protocol.toUpperCase().replace("-", "_"));
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
