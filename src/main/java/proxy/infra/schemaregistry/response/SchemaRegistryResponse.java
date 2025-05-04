package proxy.infra.schemaregistry.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * schema registry 응답
 */
@Getter
@ToString
@AllArgsConstructor
public class SchemaRegistryResponse {
    // http status code
    private final int statusCode;
    
    // http response message
    private final String message;
}
