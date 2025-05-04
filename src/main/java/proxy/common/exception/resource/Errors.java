package proxy.common.exception.resource;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.http.HttpStatus;

import proxy.common.exception.ProxyException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Errors {

    // ##############################################################################
    // 400:BAD_REQUEST
    // ##############################################################################
    // message serialization exception
    INVALID_DATA_FORMAT(11001, "Invalid data format"),

    // consumer instance exception
    INVALID_FORMAT_TYPE(11051, "Invalid format type"),
    INVALID_EMBEDDED_FORMAT(11052, "Invalid embedded format"),

    // schema registry exception
    KEY_SCHEMA_MISSING(11101, "Request includes keys but does not include key schema"),
    VALUE_SCHEMA_MISSING(11102, "Request includes values but does not include value schema"),

    // ##############################################################################
    // 401:UNAUTHORIZED
    // ##############################################################################
    // producer exception
    KAFKA_AUTHENTICATION_FAILED(40101, "Kafka authentication failed"),

    // ##############################################################################
    // 403:FORBIDDEN
    // ##############################################################################
    // producer exception
    KAFKA_AUTHORIZATION_FAILED(40301, "Kafka authorization failed"),

    // ##############################################################################
    // 404:NOT_FOUND
    // ##############################################################################
    // consumer exception
    PARTITION_NOT_FOUND(40401, "Partition not found"),
    CONSUMER_INSTANCE_NOT_FOUND(40402, "Consumer instance not found"),

    // schema registry exception
    SCHEMA_NOT_FOUND(40451, "Schema not found"),
    SUBJECT_NOT_FOUND(40452, "Subject not found"),
    SCHEMA_VERSION_NOT_FOUND(40453, "Schema version not found"),

    // ##############################################################################
    // 406:NOT_ACCEPTABLE
    // ##############################################################################
    // consumer exception
    CONSUMER_FORMAT_MISMATCH(40601, "The requested embedded data format does not match the deserializer for this consumer instance"),

    // ##############################################################################
    // 409:CONFLICT
    // ##############################################################################
    // consumer exception
    CONSUMER_ALREADY_EXISTS(40901, "Consumer with specified consumer ID already exists in the specified consumer group."),

    // ##############################################################################
    // 500:INTERNAL_SERVER_ERROR
    // ##############################################################################
    // producer exception
    KAFKA_RETRIABLE_FAILED(2001, "Kafka retriable failed"),
    KAFKA_ERROR(2002, "Kafka error"),
    UNEXPECTED_NON_KAFKA_ERROR(2003, "Unexpected non-Kafka exception returned by Kafka"),

    // consumer instance exception
    NO_CONSUMER_POOL_RESOURCES(3001, "No Consumer pool resources"),
    COMMIT_OFFSETS_FAILED(3002, "Consumer commit offsets failed"),

    // schema registry exception
    INVALID_SCHEMA(4001, "Invalid schema"),
    INVALID_SCHEMA_VERSION(4002, "Invalid schema version"),
    INVALID_COMPATIBILITY(4003, "Invalid compatibility level"),
    MODE_CONFLICT(4004, "Request subject is in read-only mode"),
    UNPROCESSABLE_SCHEMA(4005, "Unprocessable schema"),
    SCHEMA_REGISTRY_BACKEND_ERROR(4006, "Schema Registry error in the backend data store"),
    SCHEMA_REGISTRY_FORWARDING_ERROR(4007, "Schema Registry error while forwarding the request to the master"),
    SCHEMA_REGISTRY_TIMED_OUT(4008, "Schema Registry operation timed out"),
    SCHEMA_REGISTRY_INTERNAL_ERROR(4009, "Schema Registry internal server error"),

    // conversion exception
    JSON_TO_OBJECT_FAILED(5001, "Failed to convert JSON to Object"),
    AVRO_TO_JSON_FAILED(5002, "Failed to convert Avro to JSON"),
    JSONSCHEMA_TO_JSON_FAILED(5003, "Failed to convert JSON Schema to JSON"),
    PROTOBUF_TO_JSON_FAILED(5004, "Failed to convert Protobuf to JSON"),
    ;

    private int code;
    private String message;

    public int code() {
        return this.code;
    }

    public String message() {
        return this.message;
    }

    public HttpStatus status() {
        HttpStatus status = null;
        switch (this.code / 100) {
            
            case 110:
            case 111:
                status = HttpStatus.BAD_REQUEST;                
                break;
            case 401:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case 403:
                status = HttpStatus.FORBIDDEN;
                break;
            case 404:
                status = HttpStatus.NOT_FOUND;
                break;
            case 406:
                status = HttpStatus.NOT_ACCEPTABLE;
                break;
            case 409:
                status = HttpStatus.CONFLICT;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }

        return status;
    }

    /**
     * 프로듀서 오류 처리
     * @param e
     * @return
     */
    public static int errorCodeFromProducerException(Throwable e) {
        // 인증 오류
        if (e instanceof AuthenticationException) {
            return KAFKA_AUTHENTICATION_FAILED.code();
        // 인가 오류
        } else if (e instanceof AuthorizationException) {
            return KAFKA_AUTHORIZATION_FAILED.code();
        // 재시도가 필요한 경우
        } else if (e instanceof RetriableException) {
            return KAFKA_RETRIABLE_FAILED.code();
        // 기타 카프카 오류
        } else if (e instanceof KafkaException) {
            return KAFKA_ERROR.code(); 
        // 카프카 오류가 아닌 경우
        } else {
            throw new ProxyException(Errors.UNEXPECTED_NON_KAFKA_ERROR);
        }
    }

    /**
     * 스키마 레지스트리 - 404 찾을 수 없음 오류
     * @param e
     * @return
     */
    public static boolean isSchemaNotFoundException(ProxyException e) {
        return e.getStatus() == HttpStatus.NOT_FOUND;
    }
}
