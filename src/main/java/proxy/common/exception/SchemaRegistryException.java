package proxy.common.exception;

import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import proxy.common.exception.resource.Errors;
import proxy.infra.schemaregistry.response.SchemaRegistryErrorResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaRegistryException {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 스키마 레지스트리 오류 처리
     * @param statusCode
     * @param message
     */
    public void parseErrors(int statusCode, String message) {
        if (!ObjectUtils.isEmpty(message)) {
            SchemaRegistryErrorResponse errorRes = null;
            try {
                // 스키마 레지스트리로부터 받은 에러 코드 및 에러 메시지 맵핑
                errorRes = objectMapper.readValue(message, SchemaRegistryErrorResponse.class);
            } catch (JsonProcessingException e) {
                log.error("", e);
                throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
            }

            if (!ObjectUtils.isEmpty(errorRes)) {
                switch (statusCode) {
                    // 찾을 수 없음 오류
                    case 404:
                        this.throwNotFoundError(errorRes);
                        break;
                    // 유효성 오류
                    case 409:
                        throw new ProxyException(Errors.INVALID_SCHEMA, message);
                    // 처리 불가능한 개체 오류
                    case 422:
                        this.throwUnprocessableEntityError(errorRes);
                    // 스키마 레지스트리 서버 오류
                    case 500:
                        this.throwInternalServerError(errorRes);
                
                    default:
                        throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
                }
    
            }
        }

        throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
    }

    /**
     * 404: 찾을 수 없음 오류
     * @param errorRes
     */
    private void throwNotFoundError(SchemaRegistryErrorResponse errorRes) {
        if (ObjectUtils.isEmpty(errorRes) || ObjectUtils.isEmpty(errorRes.getErrorCode())) {
            throw new ProxyException(Errors.SUBJECT_NOT_FOUND);
        }

        switch (errorRes.getErrorCode()) {
            case 40401:
                throw new ProxyException(Errors.SUBJECT_NOT_FOUND);
            case 40402:
                throw new ProxyException(Errors.SCHEMA_VERSION_NOT_FOUND);
            case 40403:
                throw new ProxyException(Errors.SCHEMA_NOT_FOUND);
            default:
                throw new ProxyException(Errors.SUBJECT_NOT_FOUND);
        }
    }

    /**
     * 422: 처리 불가능한 개체 오류
     * @param errorRes
     */
    private void throwUnprocessableEntityError(SchemaRegistryErrorResponse errorRes) {
        if (ObjectUtils.isEmpty(errorRes) || ObjectUtils.isEmpty(errorRes.getErrorCode())) {
            throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
        }

        switch (errorRes.getErrorCode()) {
            case 42201:
                throw new ProxyException(Errors.INVALID_SCHEMA);
            case 42202:
                throw new ProxyException(Errors.INVALID_SCHEMA_VERSION);
            case 42203:
                throw new ProxyException(Errors.INVALID_COMPATIBILITY);
            case 42205:
                throw new ProxyException(Errors.MODE_CONFLICT);
            default:
                throw new ProxyException(Errors.UNPROCESSABLE_SCHEMA);
        }
    }

    /**
     * 500: 스키마 레지스트리 서버 오류
     * @param errorRes
     */
    private void throwInternalServerError(SchemaRegistryErrorResponse errorRes) {
        if (ObjectUtils.isEmpty(errorRes) || ObjectUtils.isEmpty(errorRes.getErrorCode())) {
            throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
        }

        switch (errorRes.getErrorCode()) {
            case 50001:
                throw new ProxyException(Errors.SCHEMA_REGISTRY_BACKEND_ERROR);
            case 50002:
                throw new ProxyException(Errors.SCHEMA_REGISTRY_TIMED_OUT);
            case 50003:
                throw new ProxyException(Errors.SCHEMA_REGISTRY_FORWARDING_ERROR);
            default:
                throw new ProxyException(Errors.SCHEMA_REGISTRY_INTERNAL_ERROR);
        }
    }
}
