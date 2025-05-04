package proxy.infra.schemaregistry.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * schema registry 에러 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SchemaRegistryErrorResponse {
    // schema registry error code
    @JsonProperty(value = "error_code")
    private int errorCode;

    // schema registry error message
    private String message;
}