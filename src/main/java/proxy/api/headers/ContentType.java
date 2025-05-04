package proxy.api.headers;

/**
 * kafka rest proxy Headers: Content-Type 목록
 * - confluent rest proxy 헤더값과 동일
 */
public class ContentType {
    // Produces
    public static final String KAFKA_V2_JSON = "application/vnd.kafka.v2+json";

    // Consumes
    public static final String KAFKA_V2_JSON_BINARY = "application/vnd.kafka.binary.v2+json";
    public static final String KAFKA_V2_JSON_JSON = "application/vnd.kafka.json.v2+json";
    public static final String KAFKA_V2_JSON_AVRO = "application/vnd.kafka.avro.v2+json";
    public static final String KAFKA_V2_JSON_JSON_SCHEMA = "application/vnd.kafka.jsonschema.v2+json";
    public static final String KAFKA_V2_JSON_PROTOBUF = "application/vnd.kafka.protobuf.v2+json";

    public static final String JSON = "application/json";
}
