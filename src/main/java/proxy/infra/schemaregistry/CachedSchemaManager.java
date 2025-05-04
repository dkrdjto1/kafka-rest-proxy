package proxy.infra.schemaregistry;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import proxy.common.cache.CacheConfig;
import proxy.common.cache.ProxyCacheManager;
import proxy.common.exception.ProxyException;
import proxy.common.format.EmbeddedFormat;
import proxy.infra.schemaregistry.result.RegisteredSchema;

/**
 * Cached schema works
 */
@Component
public class CachedSchemaManager {

    @Autowired
    private SchemaRegistryManager schemaRegistryManager;

    @Autowired
    private ProxyCacheManager cacheManager;

    private static final String NO_SUBJECT = "";

    /**
     * 스키마ID로 스키마(내용) 조회
     * @param subject
     * @param schemaId
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public String getRawSchemaById(String subject, int schemaId) throws IOException {
        if (subject == null) subject = NO_SUBJECT;

        // subject로 <key: schemaId, value: schema> 캐시 데이터 조회
        Map<Integer, String> idSchemaMap = this.cacheManager.get(CacheConfig.ID_TO_SCHEMA, subject, Map.class);
        
        // 조회된 map이 없을 경우, 빈 map 생성
        if (idSchemaMap == null) idSchemaMap = new ConcurrentHashMap<>();

        synchronized (this) {
            // 스키마ID로 map에 저장된 스키마 조회
            String cachedSchema = idSchemaMap.get(schemaId);
            
            // 조회된 스키마가 있을 경우, 반환
            if (cachedSchema != null) return cachedSchema;

            // 조회된 스키마가 없는 경우, 연결된 스키마 레지스트리에서 스키마ID로 스키마 검색
            final String retrievedRawSchema = this.schemaRegistryManager.getRawSchemaById(schemaId, subject);
            
            // 캐시 저장 -> key: subject, value: Map<key: schemaId, value: schema>
            idSchemaMap.put(schemaId, retrievedRawSchema);
            this.cacheManager.put(CacheConfig.ID_TO_SCHEMA, subject, idSchemaMap);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaId>
            Map<String, Integer> schemaIdMap = Map.of(retrievedRawSchema, schemaId);
            this.cacheManager.put(CacheConfig.SCHEMA_TO_ID, subject, schemaIdMap);

            return retrievedRawSchema;
        }
    }

    /**
     * 스키마(내용)로 스키마ID 조회
     * @param format
     * @param subject
     * @param schema
     * @return
     * @throws IOException
     * @throws ProxyException
     */
    @SuppressWarnings("unchecked")
    public int getIdByRawSchema(EmbeddedFormat format, String subject, String schema) throws IOException, ProxyException {
        if (subject == null) subject = NO_SUBJECT;

        // subject로 <key: schema, value: schemaId> 캐시 데이터 조회
        Map<String, Integer> schemaIdMap = this.cacheManager.get(CacheConfig.SCHEMA_TO_ID, subject, Map.class);

        // 조회된 map이 없을 경우, 빈 map 생성
        if (schemaIdMap == null) schemaIdMap = new ConcurrentHashMap<>();

        synchronized (this) {
            // 스키마(내용)로 map에 저장된 스키마ID 조회
            Integer cachedId = schemaIdMap.get(schema);

            // 조회된 스키마ID가 있을 경우, 반환
            if (cachedId != null) return cachedId;

            // 조회된 스키마ID가 없는 경우, 연결된 스키마 레지스트리에서 스키마(내용)로 스키마 검색
            final RegisteredSchema retrievedSchema = this.schemaRegistryManager.getSchemaByRawSchema(format, subject, schema);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaId>
            schemaIdMap.put(schema, retrievedSchema.getSchemaId());
            this.cacheManager.put(CacheConfig.SCHEMA_TO_ID, subject, schemaIdMap);

            // 캐시 저장 -> key: subject, value: Map<key: schemaId, value: schema>
            Map<Integer, String> idSchemaMap = Map.of(retrievedSchema.getSchemaId(), schema);
            this.cacheManager.put(CacheConfig.ID_TO_SCHEMA, subject, idSchemaMap);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaVersion>
            Map<String, Integer> schemaVersionMap = Map.of(schema, retrievedSchema.getSchemaVersion());
            this.cacheManager.put(CacheConfig.SCHEMA_TO_VERSION, subject, schemaVersionMap);

            return retrievedSchema.getSchemaId();
        }
    }

    /**
     * 새로운 스키마 등록 및 스키마ID 반환
     * @param format
     * @param subject
     * @param schema
     * @return
     * @throws IOException
     * @throws ProxyException
     */
    @SuppressWarnings("unchecked")
    public int registerAndGetId(EmbeddedFormat format, String subject, String schema) throws IOException, ProxyException {
        if (subject == null) subject = NO_SUBJECT;

        // subject로 <key: schema, value: schemaId> 캐시 데이터 조회
        Map<String, Integer> schemaIdMap = this.cacheManager.get(CacheConfig.SCHEMA_TO_ID, subject, Map.class);

        // 조회된 map이 없을 경우, 빈 map 생성
        if (schemaIdMap == null) schemaIdMap = new ConcurrentHashMap<>();

        synchronized (this) {
            // 스키마(내용)로 map에 저장된 스키마ID 조회
            Integer cachedId = schemaIdMap.get(schema);

            // 조회된 스키마ID가 있을 경우, 반환
            if (cachedId != null) return cachedId;

            // 조회된 스키마ID가 없는 경우, 연결된 스키마 레지스트리에 새로운 스키마 등록 및 스키마ID 반환
            final int retrievedId = this.schemaRegistryManager.registerAndGetId(format, subject, schema);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaId>
            schemaIdMap.put(schema, retrievedId);
            this.cacheManager.put(CacheConfig.SCHEMA_TO_ID, subject, schemaIdMap);

            // 캐시 저장 -> key: subject, value: Map<key: schemaId, value: schema>
            Map<Integer, String> idSchemaMap = Map.of(retrievedId, schema);
            this.cacheManager.put(CacheConfig.ID_TO_SCHEMA, subject, idSchemaMap);

            return retrievedId;
        }
    }

    /**
     * 최신 스키마 조회
     * @param subject
     * @return
     * @throws IOException
     */
    public RegisteredSchema getLatestSchema(String subject) throws IOException {
        if (subject == null) subject = NO_SUBJECT;

        synchronized (this) {
            // 연결된 스키마 레지스트리에서 최신 버전의 스키마 검색
            final RegisteredSchema retrievedSchema = this.schemaRegistryManager.getLatestSchema(subject);

            return retrievedSchema;
        }
    }

    /**
     * 스키마(내용)로 스키마 버전 조회
     * @param format
     * @param subject
     * @param schema
     * @return
     * @throws IOException
     * @throws ProxyException 
     */
    @SuppressWarnings("unchecked")
    public int getVersionByRawSchema(EmbeddedFormat format, String subject, String schema) throws IOException, ProxyException {
        if (subject == null) subject = NO_SUBJECT;

        // subject로 <key: schema, value: schemaVersion> 캐시 데이터 조회
        Map<String, Integer> schemaVersionMap = this.cacheManager.get(CacheConfig.SCHEMA_TO_VERSION, subject, Map.class);

        // 조회된 map이 없을 경우, 빈 map 생성
        if (schemaVersionMap == null) schemaVersionMap = new ConcurrentHashMap<>();

        synchronized (this) {
            // 스키마 버전 조회
            Integer cachedVersion = schemaVersionMap.get(schema);

            // 조회된 스키마 버전이 있을 경우, 반환
            if (cachedVersion != null) return cachedVersion;

            // 조회된 스키마 버전이 없는 경우, 연결된 스키마 레지스트리에서 스키마(내용)로 스키마 검색
            final RegisteredSchema retrievedSchema = this.schemaRegistryManager.getSchemaByRawSchema(format, subject, schema);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaVersion>
            schemaVersionMap.put(schema, retrievedSchema.getSchemaVersion());
            this.cacheManager.put(CacheConfig.SCHEMA_TO_VERSION, subject, schemaVersionMap);

            // 캐시 저장 -> key: subject, value: Map<key: schemaId, value: schema>
            Map<Integer, String> idSchemaMap = Map.of(retrievedSchema.getSchemaId(), schema);
            this.cacheManager.put(CacheConfig.ID_TO_SCHEMA, subject, idSchemaMap);

            // 캐시 저장 -> key: subject, value: Map<key: schema, value: schemaId>
            Map<String, Integer> schemaIdMap = Map.of(schema, retrievedSchema.getSchemaId());
            this.cacheManager.put(CacheConfig.SCHEMA_TO_ID, subject, schemaIdMap);

            return retrievedSchema.getSchemaVersion();
        }
    }
}
