package proxy.infra.schemaregistry.result;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 등록된 스키마 정보
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisteredSchema {

    private String subject;    // 스키마 주제
    
    @JsonProperty(value = "id")
    private int schemaId;      // 스키마ID

    @JsonProperty(value = "version")
    private int schemaVersion; // 스키마 버전

    private String schema;     // 스키마

    private String schemaType; // 스키마 타입
}
