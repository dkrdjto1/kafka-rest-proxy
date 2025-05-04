package proxy.infra.schemaregistry;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import proxy.common.exception.ProxyException;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;
import proxy.common.format.SchemaType;
import proxy.common.util.SchemaUtil;
import proxy.infra.schemaregistry.result.RegisteredSchema;
import lombok.extern.slf4j.Slf4j;

/**
 * schema works
 */
@Component
@Slf4j
public class SchemaManager {
    
    @Autowired
    private CachedSchemaManager cachedSchemaManager;

    /**
     * 스키마 조회
     * @param topicName // 토픽명
     * @param format    // 메시지 포맷
     * @param schemaId  // 스키마ID
     * @param rawSchema // 스키마(내용)
     * @param isKey     // 키 여부
     * @return
     */
    public RegisteredSchema getSchema(
            String topicName,
            EmbeddedFormat format,
            Optional<Integer> schemaId,
            Optional<String> rawSchema,
            boolean isKey) {

        // subject 생성 (Naming 방식 고정)
        String subject = SchemaUtil.getSubjectName(topicName, isKey);

        // 스키마ID로 스키마 조회
        if (schemaId != null) {
            return getSchemaFromSchemaId(format, subject, schemaId.get());
        }

        // 스키마(내용)로 스키마 조회
        if (rawSchema != null) {
            return getSchemaFromRawSchema(format, subject, rawSchema.get());
        }

        // 가장 최근 저장된 스키마 조회
        return findLatestSchema(format, subject);
    }

    /**
     * 스키마ID로 스키마 조회
     * @param format   // 메시지 포맷
     * @param subject  // 스키마 주제
     * @param schemaId // 스키마ID
     * @return
     */
    private RegisteredSchema getSchemaFromSchemaId(EmbeddedFormat format, String subject, int schemaId) {
        String rawSchema = "";
        int schemaVersion = -1;

        try {
            // 스키마ID로 스키마(내용) 조회
            rawSchema = this.cachedSchemaManager.getRawSchemaById(subject, schemaId);

            // 스키마(내용)로 스키마 특정 버전 조회
            schemaVersion = this.cachedSchemaManager.getVersionByRawSchema(format, subject, rawSchema);
        
        } catch (IOException e) {
            log.error("Error when fetching schema by id. schemaId = {}", schemaId);
        } catch (ProxyException e) {
            log.error("Error when fetching schema by id. schemaId = {}", schemaId);
            throw e;
        }

        return new RegisteredSchema(subject, schemaId, schemaVersion, rawSchema, SchemaType.getSchemaType(format));
    }

    /**
     * 스키마(내용)로 스키마 조회
     * @param format    // 메시지 포맷
     * @param subject   // 스키마 주제
     * @param rawSchema // 스키마(내용)
     * @return
     */
    private RegisteredSchema getSchemaFromRawSchema(EmbeddedFormat format, String subject, String rawSchema) {
        int schemaId = -1;
        int schemaVersion = -1;

        try {
            try {
                // 스키마(내용)로 스키마ID 조회
                schemaId = this.cachedSchemaManager.getIdByRawSchema(format, subject, rawSchema);
                
            } catch (ProxyException e) {
                // 스키마를 찾을 수 없음 오류
                if (Errors.isSchemaNotFoundException(e)) {
                    // 스키마ID를 조회할 수 없는 경우, 스키마 새로 등록
                    schemaId = this.cachedSchemaManager.registerAndGetId(format, subject, rawSchema);
                } else {
                    throw e;
                }
            }

            // 스키마(내용)로 스키마 특정 버전 조회
            schemaVersion = this.cachedSchemaManager.getVersionByRawSchema(format, subject, rawSchema);

        } catch (IOException e) {
            log.error("Error when fetching schema by raw schema. schema = {}", rawSchema);
        } catch (ProxyException e) {
            log.error("Error when fetching schema by raw schema. schema = {}", rawSchema);
            throw e;
        }

        return new RegisteredSchema(subject, schemaId, schemaVersion, rawSchema, SchemaType.getSchemaType(format));
    }

    /**
     * 최신 스키마 조회
     * @param subject // 스키마 주제
     * @return
     */
    private RegisteredSchema findLatestSchema(EmbeddedFormat format, String subject) {
        RegisteredSchema schema = new RegisteredSchema();

        try {
            // 최신 스키마 조회
            schema = this.cachedSchemaManager.getLatestSchema(subject);
        
        } catch (IOException e) {
            log.error("Error when fetching latest schema by subject. subject = {}", subject);
        }

        return new RegisteredSchema(subject, schema.getSchemaId(), schema.getSchemaVersion(), schema.getSchema(), SchemaType.getSchemaType(format));
    }
}
