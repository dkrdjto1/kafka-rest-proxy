package proxy.api;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import proxy.api.headers.ContentType;
import proxy.api.request.ProduceRequest;
import proxy.api.resource.API_URI_RESOURCE;
import proxy.api.response.ProduceResponse;
import proxy.common.format.EmbeddedFormat;
import proxy.domain.ProduceAction;
import jakarta.validation.Valid;

@RestController
public class ProduceToTopic {

    @Autowired
    private ProduceAction produceAction;

    /**
     * (BINARY) 토픽 메시지 프로듀싱
     * @param produceRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.TOPICS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON_BINARY)
    public CompletableFuture<ResponseEntity<ProduceResponse>> produceBinary(
            @PathVariable(value = "topic_name") String topicName,
            @RequestBody @Valid ProduceRequest produceRequest) {
        
        var response = this.produceAction.produceWithoutSchema(EmbeddedFormat.BINARY, topicName, /* partitionId= */ Optional.empty(), produceRequest);
    
        return response.thenApply(produceResponse -> ResponseEntity.ok().body(produceResponse));
    }

    /**
     * (JSON) 토픽 메시지 프로듀싱
     * @param produceRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.TOPICS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON_JSON)
    public CompletableFuture<ResponseEntity<ProduceResponse>> produceJson(
            @PathVariable(value = "topic_name") String topicName,
            @RequestBody @Valid ProduceRequest produceRequest) {
        
        var response = this.produceAction.produceWithoutSchema(EmbeddedFormat.JSON, topicName, /* partitionId= */ Optional.empty(), produceRequest);
    
        return response.thenApply(produceResponse -> ResponseEntity.ok().body(produceResponse));
    }

    /**
     * (AVRO) 토픽 메시지 프로듀싱
     * @param produceRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.TOPICS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON_AVRO)
    public CompletableFuture<ResponseEntity<ProduceResponse>> produceAvro(
            @PathVariable(value = "topic_name") String topicName,
            @RequestBody @Valid ProduceRequest produceRequest) {
        
        var response = this.produceAction.produceWithSchema(EmbeddedFormat.AVRO, topicName, /* partitionId= */ Optional.empty(), produceRequest);
    
        return response.thenApply(produceResponse -> ResponseEntity.ok().body(produceResponse));
    }

    /**
     * (JSON SCHEMA) 토픽 메시지 프로듀싱
     * @param produceRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.TOPICS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON_JSON_SCHEMA)
    public CompletableFuture<ResponseEntity<ProduceResponse>> produceJsonSchema(
            @PathVariable(value = "topic_name") String topicName,
            @RequestBody @Valid ProduceRequest produceRequest) {
        
        var response = this.produceAction.produceWithSchema(EmbeddedFormat.JSONSCHEMA, topicName, /* partitionId= */ Optional.empty(), produceRequest);
    
        return response.thenApply(produceResponse -> ResponseEntity.ok().body(produceResponse));
    }

    /**
     * (PROTOBUF) 토픽 메시지 프로듀싱
     * @param produceRequest
     * @return
     */
    @PostMapping(path = API_URI_RESOURCE.TOPICS_PARAM, produces = ContentType.KAFKA_V2_JSON, consumes = ContentType.KAFKA_V2_JSON_PROTOBUF)
    public CompletableFuture<ResponseEntity<ProduceResponse>> produceProtobuf(
            @PathVariable(value = "topic_name") String topicName,
            @RequestBody @Valid ProduceRequest produceRequest) {
        
        var response = this.produceAction.produceWithSchema(EmbeddedFormat.PROTOBUF, topicName, /* partitionId= */ Optional.empty(), produceRequest);
    
        return response.thenApply(produceResponse -> ResponseEntity.ok().body(produceResponse));
    }
}
