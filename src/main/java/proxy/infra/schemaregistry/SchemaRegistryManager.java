package proxy.infra.schemaregistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import proxy.common.cache.CacheConfig;
import proxy.common.cache.ProxyCacheManager;
import proxy.common.config.KafkaRestConfig;
import proxy.common.exception.ProxyException;
import proxy.common.exception.SchemaRegistryException;
import proxy.common.exception.resource.Errors;
import proxy.common.format.EmbeddedFormat;
import proxy.common.format.SchemaType;
import proxy.common.util.HttpReqUtil;
import proxy.common.util.JsonUtil;
import proxy.infra.schemaregistry.response.SchemaRegistryResponse;
import proxy.infra.schemaregistry.result.RegisteredSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Access layer for sending requests to the schema registry
 */
@Component
@Slf4j
public class SchemaRegistryManager extends SchemaRegistryException {

    @Autowired
    private KafkaRestConfig kafkaRestConfig;

    @Autowired
    private ProxyCacheManager cacheManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final int MAX_MISSING_CACHE_SIZE = 10000;
    public static final long MISSING_ID_CACHE_TTL = 60;
    public static final long MISSING_VERSION_CACHE_TTL = 60;
    public static final long MISSING_SCHEMA_CACHE_TTL = 60;

    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/vnd.schemaregistry.v1+json";

    /**
     * 스키마ID로 스키마(내용) 조회
     * @param schemaId // 스키마ID
     * @param subject  // 스키마 주제
     * @return
     */
    public String getRawSchemaById(int schemaId, String subject) {
        // 1분 이내에 스키마 레지스트리로부터 NOT_FOUND 결과를 받았던 경우
        if (this.cacheManager.get(CacheConfig.MISSING_SCHEMA_ID, new SubjectAndId(subject, schemaId), Long.class) != null) {
            throw new ProxyException(Errors.SCHEMA_NOT_FOUND, "Schema id: " + schemaId + " not found");
        }

        RegisteredSchema restSchema;

        try {
            // GET 요청
            var response = this.reqGet("/schemas/ids/" + schemaId, "");
            restSchema = this.readValue(response);
        } catch (ProxyException e) {
            // 스키마를 찾을 수 없음 오류
            if (Errors.isSchemaNotFoundException(e)) {
                // 캐시 저장 -> key: SubjectAndId, value: currentTime
                this.cacheManager.put(CacheConfig.MISSING_SCHEMA_ID, new SubjectAndId(subject, schemaId), System.currentTimeMillis());
            }
            throw e;
        }

        return restSchema.getSchema();
    }

    /**
     * 스키마(내용)로 스키마 조회
     * @param subject   // 스키마 주제
     * @param rawSchema // 스키마(내용)
     * @return
     * @throws ProxyException
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    public RegisteredSchema getSchemaByRawSchema(EmbeddedFormat format, String subject, String rawSchema) throws ProxyException, JsonMappingException, JsonProcessingException {
        // 1분 이내에 스키마 레지스트리로부터 NOT_FOUND 결과를 받았던 경우
        if (this.cacheManager.get(CacheConfig.MISSING_SCHEMA, new SubjectAndSchema(subject, rawSchema), Long.class) != null) {
            throw new ProxyException(Errors.SCHEMA_NOT_FOUND, "Schema not found");
        }

        RegisteredSchema restSchema;

        // 스키마(내용)을 JSON 문자열로 변환
        String requestBody = this.makeJsonString(SchemaType.getSchemaType(format), rawSchema);

        try {
            // POST 요청
            var response = this.reqPost("/subjects/" + subject, "", requestBody);
            restSchema = this.readValue(response);
        } catch (ProxyException e) {
            // 스키마를 찾을 수 없음 오류
            if (Errors.isSchemaNotFoundException(e)) {
                // 캐시 저장 -> key: SubjectAndSchema, value: currentTime
                this.cacheManager.put(CacheConfig.MISSING_SCHEMA, new SubjectAndSchema(subject, rawSchema), System.currentTimeMillis());
            }
            throw e;
        }

        return restSchema;
    }

    /**
     * 새로운 스키마 등록 및 스키마ID 반환
     * @param format  // 메시지 포맷
     * @param subject // 스키마 주제
     * @param schema  // 스키마(내용)
     * @return
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    public int registerAndGetId(EmbeddedFormat format, String subject, String rawSchema) throws ProxyException, JsonMappingException, JsonProcessingException {
        // 스키마(내용)을 JSON 문자열로 변환
        String requestBody = this.makeJsonString(SchemaType.getSchemaType(format), rawSchema);

        // POST 요청
        var response = this.reqPost("/subjects/" + subject + "/versions", "", requestBody);
        var restSchema = this.readValue(response);

        return restSchema.getSchemaId();
    }

    /**
     * 최신 스키마 조회
     * @param subject // 스키마 주제
     * @return
     * @throws IOException
     */
    public RegisteredSchema getLatestSchema(String subject) throws IOException {
        // GET 요청
        var response = this.reqGet("/subjects/" + subject + "/versions/latest", ""); // TODO: url, response 확인
        var restSchema = this.readValue(response);

        return restSchema;
    }

    // ####################################################################################
    // ################################### http request ###################################
    // ####################################################################################

    /**
     * http GET request
     * @param resourceUrl // should start-with '/'
     * @param additionalQueryParam // should start-with '?' optinal
     * @return
     */
    private SchemaRegistryResponse reqGet(String resourceUrl, String additionalQueryParam) {
        Integer statusCode = 200; // 응답 상태코드
        String msg = ""; // 응답값

        // 요청 URL
        var stringUrl = new StringBuilder().append(kafkaRestConfig.getSchemaRegistryUrl()).append(resourceUrl).append(additionalQueryParam);
        var encodedUrl = UriUtils.encodePath(stringUrl.toString(), StandardCharsets.UTF_8);
        try {
            log.debug(">> request={}", encodedUrl);

            // 응답 반환
            var response = HttpReqUtil.make(encodedUrl)
                    .setMethod(HttpReqUtil.HTTP_METHOD.GET)
                    .applyBasicAuth(kafkaRestConfig.getSchemaRegistryUsername(), kafkaRestConfig.getSchemaRegistryPassword())
                    .send();
            
            // 응답값 맵핑
            statusCode = response.getStatusCode();
            msg = response.getResponseOrErr();

        } catch (IOException e) {
            log.error("",e);
            statusCode = 502; // BAD_GATEWAY
            msg = e.getMessage();
            
        } finally {
            log.debug("<< response code={}, message={}", statusCode, msg);

            // 정상 응답이 아닌 경우, 상태 코드에 따라 예외 반환
            if (statusCode != 200) {
                parseErrors(statusCode, msg);
            }
        }

        return new SchemaRegistryResponse(statusCode, msg);
    }

    /**
     * http POST request
     * @param resourceUrl // should start-with '/'
     * @param additionalQueryParam // should start-with '?' optinal
     * @param body // data
     * @return
     */
    private SchemaRegistryResponse reqPost(String resourceUrl, String additionalQueryParam, String body) throws ProxyException {
        Integer statusCode = 200; // 응답 상태코드
        String msg = ""; // 응답값

        // 요청 URL
        var stringUrl = new StringBuilder().append(kafkaRestConfig.getSchemaRegistryUrl()).append(resourceUrl).append(additionalQueryParam);
        var encodedUrl = UriUtils.encodePath(stringUrl.toString(), StandardCharsets.UTF_8);
        try {
            log.debug(">> request url={}, body={}", encodedUrl, body);
            
            // 응답 반환
            var response =HttpReqUtil.make(encodedUrl)
                    .setMethod(HttpReqUtil.HTTP_METHOD.POST)
                    .addHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE)
                    .applyBasicAuth(kafkaRestConfig.getSchemaRegistryUsername(), kafkaRestConfig.getSchemaRegistryPassword())
                    .sendBody(body);

            // 응답값 맵핑
            statusCode = response.getStatusCode();
            msg = response.getResponseOrErr();

        } catch (IOException e) {
            log.error("",e);
            msg = e.getMessage();

        } finally {
            log.debug("<< response code={}, message={}", statusCode, msg);

            // 정상 응답이 아닌 경우, 상태 코드에 따라 예외 반환
            if (statusCode != 200) {
                parseErrors(statusCode, msg);
            }
        }

        return new SchemaRegistryResponse(statusCode, msg);
    }

    /**
     * json 응답 문자열을 {@link RegisteredSchema} 형태로 변환하여 반환
     * @param response
     * @return
     */
    private RegisteredSchema readValue(SchemaRegistryResponse response) {
        RegisteredSchema result = null;

        // 응답값이 있는 경우, 응답값 맵핑
        if (!ObjectUtils.isEmpty(response) && !ObjectUtils.isEmpty(response.getMessage())) {
            try {
                result = new ObjectMapper().readValue(response.getMessage(), RegisteredSchema.class);
            } catch (JsonProcessingException e) {
                log.error("", e);
                log.debug("Occured an error while parse [{}]", response.toString());
            }
        }

        // 맵핑된 응답값이 없는 경우, 빈 객체 반환
        if (result == null) {
            result = new RegisteredSchema();
        }

        return result;
    }

    /**
     * 요청 스키마(내용)을 JSON 문자열 형태로 변환하여 반환
     * @param rawSchema
     * @return
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    private String makeJsonString(String schemaType, String rawSchema) throws JsonMappingException, JsonProcessingException {
        // JSON 객체 생성
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("schema", rawSchema);
        if (schemaType != null) {
            jsonNode.put("schemaType", schemaType);
        }

        // JSON 문자열로 변환
        var requestBody = JsonUtil.writeValueAsString(jsonNode);

        return requestBody;
    }

    // ####################################################################################
    // ############################## for cached missing map ##############################
    // ####################################################################################

    @Getter
    @AllArgsConstructor
    private class SubjectAndId {
        private final String subject;
        private final int schemaId;
    }

    @Getter
    @AllArgsConstructor
    private class SubjectAndSchema {
        private final String subject;
        private final String schema;
    }
}
