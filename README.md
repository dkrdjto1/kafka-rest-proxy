# Kafka REST Proxy

Producer / Consumer API 기능 제공

# Environment

- Java 17
- Spring Boot 3.0.0, kafka-clients 3.5.1

# Tree

```bash
.
├── README.md
├── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
├── config
│   ├── kafka-rest.yml  # 프록시 서버 외부 설정
│   └── ssl  # ssl 인증 파일 경로
│       ├── truststore.jks
│       └── keystore.jks
└── src
    └── main
        ├── java
        │   └── proxy
        │       ├── ProxyApplication.java  # main
        │       ├── api
        │       │   ├── Consume.java             # 컨슈머 API
        │       │   ├── ProduceToTopic.java      # 프로듀서 API (토픽)
        │       │   ├── ProduceToPartition.java  # 프로듀서 API (토픽-파티션)
        │       │   ├── headers
        │       │   │   └── ContentType.java  # 프록시 Content-Type 헤더 목록
        │       │   ├── request  # 프로듀서/컨슈머 요청
        │       │   │   └── ...
        │       │   ├── resource
        │       │   │   └── API_URI_RESOURCE.java  # API uri 목록
        │       │   └── response  # 프로듀서/컨슈머 응답
        │       │       └── ...
        │       ├── common
        │       │   ├── CompletableFutures.java  # FutureTask 처리용
        │       │   ├── cache
        │       │   │   ├── CacheConfig.java        # 캐시 설정값
        │       │   │   └── ProxyCacheManager.java  # 캐시 작업 관리
        │       │   ├── callback
        │       │   │   └── ConsumerReadCallback.java  # 컨슈밍 작업 결과 반환 callback
        │       │   ├── config
        │       │   │   ├── ConfigLoader.java            # 프록시 외부 설정 파일 읽는 용도
        │       │   │   ├── ConsumerInstanceConfig.java  # 컨슈머 인스턴스 설정값 담는 객체
        │       │   │   ├── KafkaClientConfig.java       # 카프카 클라이언트 설정
        │       │   │   ├── KafkaProtocol.java           # 카프카 보안 프로토콜 목록
        │       │   │   ├── KafkaRestConfig.java         # spring 환경 변수에 등록된 외부 설정값 담는 객체
        │       │   │   └── SaslMechanism.java           # 카프카 보안 매커니즘 목록
        │       │   ├── context
        │       │   │   ├── ApplicationContextProvider.java  # bean 정보를 활용하기 위한 싱글톤 클래스
        │       │   │   └── DeserializationContext.java      # 카프카로부터 응답받은 payload를 활용하기 위한 클래스
        │       │   ├── exception  # 예외 처리
        │       │   │   ├── resource
        │       │   │   │   └── Errors.java               # 프록시 예외 목록
        │       │   │   ├── ProxyException.java           # 프록시 예외 반환 객체
        │       │   │   └── SchemaRegistryException.java  # 스키마 레지스트리 오류 처리
        │       │   ├── serializer  # avro, jsonschema, protobuf 처리하기 위한 custom serializer
        │       │   │   └── ...
        │       │   ├── deserializer  # avro, jsonschema, protobuf 처리하기 위한 custom deserializer
        │       │   │   └── ...
        │       │   ├── converter  # avro, jsonschema, protobuf -> json node 형태로 응답하기 위한 converter
        │       │   │   └── ...
        │       │   ├── format
        │       │   │   ├── EmbeddedFormat.java  # 프록시에서 지원하는 메시지 타입 목록
        │       │   │   └── SchemaType.java      # 스키마 타입 목록
        │       │   ├── util  # 유틸 클래스
        │       │   │   └── ...
        │       ├── domain
        │       │   ├── ConsumeAction.java  # 컨슈머 API 처리
        │       │   └── ProduceAction.java  # 프로듀서 API 처리
        │       ├── infra
        │       │   ├── kafka
        │       │   │   ├── ConsumerManager.java  # 컨슈머 풀 관리
        │       │   │   ├── ProducerManager.java  # 프로듀서 풀 관리
        │       │   │   ├── task
        │       │   │   │   └── ConsumerReadTask.java  # 컨슈머 읽기 작업 (Runnable)
        │       │   │   ├── result  # 카프카 응답값 맵핑 클래스
        │       │   │   │   └── ...
        │       │   │   └── worker  # 프로듀서/컨슈머 작업
        │       │   │       └── ...
        │       │   ├── schemaregistry
        │       │   │   ├── SchemaManager.java          # 스키마 조회
        │       │   │   ├── CachedSchemaManager.java    # 캐시 데이터에서 스키마 조회
        │       │   │   ├── SchemaRegistryManager.java  # 스키마 레지스트리에서 스키마 조회
        │       │   │   ├── response  # 스키마 레지스트리 HTTP 응답
        │       │   │   │   └── ...
        │       │   │   └── result
        │       │   │       └── RegisteredSchema.java  # 스키마 정보 맵핑 클래스
        │       └── schedular
        │           └── ExpirationSchedular.java  # 컨슈머 인스턴스 만료 체크 스케줄러
        └── resources
            ├── application.yml  # 프록시 서버 내부 설정
            └── META-INF
                └── spring.factories  # spring 환경 변수 설정 용도
```

# Configuration Options

| Option | Type | Required | Description | Valid values | Notice |
| --- | --- | --- | --- | --- | --- |
| bootstrap.servers | String | Y | 프록시와 연결할 카프카 브로커 목록 | - | - |
| client.protocol | String | Y | 브로커와의 통신에 사용할 프로토콜 | `PLAINTEXT`, `SSL`, `SASL_PLAINTEXT`, `SASL_SSL` | - |
| client.sasl-mechanism | String | N | SASL 매커니즘 | `PLAIN`, `SCRAM_SHA_256`, `SCRAM_SHA_512` | SASL 프로토콜 사용 시 필수 입력 |
| client.username | String | N | SCRAM 사용자명 | - | SCRAM 매커니즘 사용 시 필수 입력 |
| client.password | String | N | SCRAM 사용자 비밀번호 | - | SCRAM 매커니즘 사용 시 필수 입력 |
| client.ssl.truststore-location | String | N | truststore 인증서 파일 경로 | `./config/ssl/truststore.jks` | SSL 프로토콜 사용 시 필수 입력 |
| client.ssl.truststore-password | String | N | truststore 인증서 비밀번호 | - | SSL 프로토콜 사용 시 필수 입력 |
| client.ssl.truststore-type | String | N | truststore 인증서 유형 | `PKCS12` | SSL 프로토콜 사용 시 필수 입력 |
| client.ssl.keystore-location | String | N | keystore 인증서 파일 경로 | `./config/ssl/keystore.jks` | SSL 프로토콜 사용 시 필수 입력 (mTLS) |
| client.ssl.keystore-password | String | N | keystore 인증서 비밀번호 | - | SSL 프로토콜 사용 시 필수 입력 (mTLS) |
| client.ssl.keystore-type | String | N | keystore 인증서 유형 | `PKCS12` | SSL 프로토콜 사용 시 필수 입력 (mTLS) |
| client.ssl.endpoint-identification-algorithm | String | N | 클라이언트가 브로커에 접속할 때, 서버의 인증서와 호스트 일치 여부 확인에 사용 | - | SSL 프로토콜 사용 시 필수 입력, 값이 없을 경우에도 필드 삭제 금지 |
| schema.registry.url | String | N | 스키마 레지스트리 주소 | - | 스키마가 필요한 데이터 유형 사용 시 필수 입력 |
| schema.registry.username | String | N | 스키마 레지스트리 사용자명 | - | 스키마 레지스트리에 인증이 걸려있을 경우 필수 입력 |
| schema.registry.password | String | N | 스키마 레지스트리 사용자 비밀번호 | - | 스키마 레지스트리에 인증이 걸려있을 경우 필수 입력 |
| ignore-ssl-validation | Boolean | Y | 보안 설정이 적용된 url에 통신 시, ssl 인증 무시 여부 | `TRUE`, `FALSE`(df) | 현재 프록시에서는 스키마 레지스트리와의 연결에만 http 통신을 시도하므로, 스키마 레지스트리 url에 보안 설정이 적용되어 있지 않은 경우, `FALSE` 여도 이용에 문제가 되지는 않음. |

# API

## Notice

* 토픽당 하나의 데이터 타입을 사용해야 함.
* 메시지 키가 있을 경우, 메시지 키 타입 = 메시지 값 타입
* `PROTOBUF`의 경우, 한 스키마당 단일 메시지 타입만 지원
* 다중 토픽 구독 시, 구독 중인 모든 토픽의 데이터 타입이 동일해야 함.
* (1) 토픽 구독 (2) 토픽-파티션 구독 (수동 할당) (3) 토픽 패턴 구독 → 상호배타적 (택1)
* 패턴으로 토픽을 구독한 경우, 메시지 조회를 1회 실행한 후에 구독 중인 토픽 목록 확인 가능
* 수동 할당한 파티션 목록은 수동 할당 조회로만 확인 가능
* 컨슈머 인스턴스가 다중 토픽 구독 중일 시, 초기 메시지 조회 시에만 랜덤으로 목록 중 한 토픽씩 연결됨
    * 최초 1회씩 메시지 조회 후, 구독할 토픽이 새로 추가되거나, 프로듀싱되는 메시지들에 대해서는, 메시지 조회 시 모든 토픽에 대해 응답 출력됨.

## API Index

### Produce

1. [토픽 메시지 프로듀싱](#1-토픽-메시지-프로듀싱)
2. [토픽-파티션 메시지 프로듀싱](#2-토픽-파티션-메시지-프로듀싱)

### Consume

1. [컨슈머 인스턴스 생성](#1-컨슈머-인스턴스-생성)
2. [컨슈머 인스턴스 삭제](#2-컨슈머-인스턴스-삭제)
3. [컨슈머 오프셋 목록 커밋](#3-컨슈머-오프셋-목록-커밋)
4. [컨슈머 마지막 오프셋 조회](#4-컨슈머-마지막-오프셋-조회)
5. [컨슈머 토픽 구독](#5-컨슈머-토픽-구독)
6. [컨슈머 토픽 구독 조회](#6-컨슈머-토픽-구독-조회)
7. [컨슈머 토픽 구독 취소](#7-컨슈머-토픽-구독-취소)
8. [컨슈머 파티션 수동 할당](#8-컨슈머-파티션-수동-할당)
9. [컨슈머 파티션 수동 할당 조회](#9-컨슈머-파티션-수동-할당-조회)
10. [컨슈머 패치 오프셋 업데이트](#10-컨슈머-패치-오프셋-업데이트)
11. [컨슈머 오프셋 이동 : BEGINNING](#11-컨슈머-오프셋-이동--BEGINNING)
12. [컨슈머 오프셋 이동 : END](#12-컨슈머-오프셋-이동--END)
13. [토픽-파티션 메시지 조회](#13-토픽-파티션-메시지-조회)

## Produce

## 1. 토픽 메시지 프로듀싱

* 메시지를 특정 토픽으로 보낼 때 사용
* 메시지 전송 시 키(key)나 파티션(partition) 지정 가능 (optional)

> 파티션 지정: 파티션이 지정되지 않으면, 각 메시지는 키의 해시(hash)를 기반으로 선택된 파티션으로 전송된다. 파티션을 직접 지정할 수도 있다.

> 키 지정: 키가 제공되지 않으면, 각 메시지의 파티션은 라운드 로빈 방식으로 선택된다. 키를 제공하면 해당 키에 대한 해시를 기반으로 파티션을 선택한다.

* `AVRO`, `JSONSCHEMA`, `PROTOBUF` 포맷을 사용할 경우, 스키마 정보를 함께 제공해야 함.
* 프록시 외부 설정 파일에 스키마 레지스트리에 액세스할 수 있는 URL 정보(`schema.registry.url`)가 설정되어 있어야 함.
* 스키마 정보는 1) 전체 스키마를 문자열로 인코딩하여 제공하거나, 2) 첫 번째 응답으로 반환된 스키마 ID를 통해 제공 가능

### 1-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/topics/:topic_name`
- Method: `POST`
- Content-Type: `application/vnd.kafka.binary.v2+json`, `application/vnd.kafka.json.v2+json`, `application/vnd.kafka.avro.v2+json`, `application/vnd.kafka.jsonschema.v2+json`, `application/vnd.kafka.protobuf.v2+json`

### 1-2. Request parameters

Query parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| topic_name | String | Y | 전송할 토픽명 |

### 1-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| key_schema | String | N | 키를 생성에 사용할 스키마. 키가 없으면 생략 가능 |
| key_schema_id | Integer | N | 키를 생성에 사용할 스키마의 ID. 키가 없으면 생략 가능 |
| value_schema | String | N | 값 생성에 사용된 스키마 |
| value_schema_id | Integer | N | 값 생성에 사용된 스키마의 ID |
| records | - | Y | 토픽에 전송할 레코드 목록 |
| key | JsonNode | N | 메시지 키. 내장된 형식에 따른 키. 키가 없으면(null) 생략 가능 |
| value | JsonNode | Y | 메시지 값. 내장된 형식에 따른 값 |
| partition | Integer | N | 메시지를 저장할 파티션 |

### 1-4. Response body

| Name | Type | Description |
| --- | --- | --- |
| key_schema_id | Integer | 키를 생성하는 데 사용된 스키마의 ID. 키를 사용하지 않은 경우 null |
| value_schema_id | Integer | 값 생성에 사용된 스키마의 ID |
| offsets | - | 메시지가 발행된 파티션 및 오프셋 목록 |
| partition | Integer | 메시지가 발행된 파티션. 메시지 발행에 실패한 경우 null |
| offset | Long | 메시지의 오프셋. 메시지 발행에 실패한 경우 null |
| error_code | Integer | 이 작업이 실패한 이유를 분류하는 오류 코드. 성공한 경우 null <br /> `1` : 재시도 할 수 없는 Kafka 예외 <br /> `2` : 재시도 가능한 Kafka 예외. 재시도하면 메시지가 성공적으로 전송될 수 있음 |
| error | String | 작업이 실패한 이유를 설명하는 오류 메시지. 성공한 경우 null |

### 1-5. Example

*Binary Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-binary-test' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.binary.v2+json' \
  -d '{"records": [{"key": "a2V5", "value": "bG9ncw=="}]}'
```

*Binary Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 3,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": null
}
```

*Json Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-json-test' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.json.v2+json' \
  -d '{"records": [{"key": "somekey", "value": {"foo": "bar"}}, {"value": [ "foo", "bar" ], "partition": 0}, {"value": 53.5}]}'
```

*Json Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 3,
      "error_code": null,
      "error": null
    },
    {
      "partition": 0,
      "offset": 4,
      "error_code": null,
      "error": null
    },
    {
      "partition": 0,
      "offset": 5,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": null
}
```

*Avro Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-avro-test' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.avro.v2+json' \
  -d '{"value_schema": "{\"type\":\"record\",\"name\":\"Example\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"}]}", "records": [{"value": {"id": 42}}, {"value": {"id": 56}, "partition": 0}]}'
```

*Avro Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 4,
      "error_code": null,
      "error": null
    },
    {
      "partition": 0,
      "offset": 5,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": 8
}
```

*Json Schema Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-jsonschema-test' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.jsonschema.v2+json' \
  -d '{"key_schema": "{\"type\": \"object\",\"properties\": {\"foo\": {\"type\": \"boolean\"},\"bar\": {\"type\": \"integer\"}}}", "value_schema": "{\"type\": \"object\",\"properties\": {\"foo\": {\"type\": \"string\"},\"bar\": {\"type\": \"number\"}}}", "records": [{"key": {"foo": true, "bar": 92}, "value": {"foo": "string", "bar": 10}}]}'
```

*Json Schema Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 3,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": 11,
  "value_schema_id": 5
}
```

*Protobuf Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-protobuf-test' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.protobuf.v2+json' \
  -d '{"value_schema": "syntax=\"proto2\"; message Person { required string name = 1; optional int32 age = 2; repeated string email = 3; }", "records": [{"value": {"name": "Kim", "age": 33, "email": ["kim1@example.com", "kim2@example.com", "kim3@example.com"]}}]}'
```

*Protobuf Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 5,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": 29
}
```

### 1-6. Schema Example

*Avro Schema ex.*

```json
{
  "type": "record",
  "name": "Example",
  "fields": [
	{
      "name": "id",
      "type": "int"
    }
  ]
}
```

*Json Schema ex.*

```json
{
  "type": "object",
  "properties": {
      "foo": {
        "type": "boolean"
      },
      "bar": {
        "type": "integer"
      }
  }
}
```

*Protobuf Schema ex.*

- proto2

```
syntax = "proto2";

message Person {
  required string name = 1;
  optional int32 age = 2;
  repeated string email = 3;
}
```

- proto3

```
syntax = "proto3";

message MyRecord {
  string id = 1;
  float amount = 2;
  string customer_id = 3;
}
```

## 2. 토픽-파티션 메시지 프로듀싱

* 특정 토픽의 특정 파티션으로 메시지를 전송할 때 사용
* `AVRO`, `JSONSCHEMA`, `PROTOBUF` 포맷을 사용할 경우, 스키마 정보를 함께 제공해야 함.
* 프록시 외부 설정 파일에 스키마 레지스트리에 액세스할 수 있는 URL 정보(`schema.registry.url`)가 설정되어 있어야 함.
* 스키마 정보는 1) 전체 스키마를 문자열로 인코딩하여 제공하거나, 2) 첫 번째 응답으로 반환된 스키마 ID를 통해 제공 가능

### 2-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/topics/:topic_name/partitions/:partition_id`
- Method: `POST`
- Content-Type: `application/vnd.kafka.binary.v2+json`, `application/vnd.kafka.json.v2+json`, `application/vnd.kafka.avro.v2+json`, `application/vnd.kafka.jsonschema.v2+json`, `application/vnd.kafka.protobuf.v2+json`

### 2-2. Request parameters

Query parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| topic_name | String | Y | 전송할 토픽명 |
| partition_id | Integer | Y | 전송할 파티션 ID |

### 2-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| key_schema | String | N | 키를 생성에 사용할 스키마. 키가 없으면 생략 가능 |
| key_schema_id | Integer | N | 키를 생성에 사용할 스키마의 ID. 키가 없으면 생략 가능 |
| value_schema | String | N | 값 생성에 사용된 스키마 |
| value_schema_id | Integer | N | 값 생성에 사용된 스키마의 ID |
| records | - | Y | 토픽에 전송할 레코드 목록 |
| key | JsonNode | N | 메시지 키. 내장된 형식에 따른 키. 키가 없으면(null) 생략 가능 |
| value | JsonNode | Y | 메시지 값. 내장된 형식에 따른 값 |

### 2-4. Response body

| Name | Type | Description |
| --- | --- | --- |
| key_schema_id | Integer | 키를 생성하는 데 사용된 스키마의 ID. 키를 사용하지 않은 경우 null |
| value_schema_id | Integer | 값 생성에 사용된 스키마의 ID |
| offsets | - | 메시지가 발행된 파티션 및 오프셋 목록 |
| partition | Integer | 메시지가 발행된 파티션. 메시지 발행에 실패한 경우 null |
| offset | Long | 메시지의 오프셋. 메시지 발행에 실패한 경우 null |
| error_code | Integer | 이 작업이 실패한 이유를 분류하는 오류 코드. 성공한 경우 null <br /> `1` : 재시도 할 수 없는 Kafka 예외 <br /> `2` : 재시도 가능한 Kafka 예외. 재시도하면 메시지가 성공적으로 전송될 수 있음 |
| error | String | 작업이 실패한 이유를 설명하는 오류 메시지. 성공한 경우 null |

### 2-5. Example

*Json Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/topics/proxy-json-test/partitions/0' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.json.v2+json' \
  -d '{"records": [{"key": "somekey", "value": {"foo": "bar"}}, {"value": 53.5}]}'
```

*Json Response ex.*

```json
{
  "offsets": [
    {
      "partition": 0,
      "offset": 6,
      "error_code": null,
      "error": null
    },
    {
      "partition": 0,
      "offset": 7,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": null
}
```

## 2. Consume

* 컨슈머 그룹 내에서 컨슈머를 생성하고 토픽 및 파티션으로부터 메시지를 소비하는 기능 제공
* 직렬화된 형태로 카프카에 저장된 데이터를 역직렬화하여 JsonNode 형태로 응답
    * 프록시에서 지원하는 데이터 포맷 : `BINARY`, `JSON`, `AVRO`, `JSONSCHEMA`, `PROTOBUF`
* 프록시 서버 셧다운 시, 생성된 모든 컨슈머 인스턴스가 삭제됨.

## 1. 컨슈머 인스턴스 생성

* 컨슈머 그룹 내에 새로운 컨슈머 인스턴스 생성
* `format` : 카프카에서 데이터를 역직렬화하는 데 사용되며, 데이터 포맷에 따라 이 컨슈머 인스턴스를 통해 수행되는 메시지 조회 API 요청의 `Accept` 헤더가 다름.
  * Ex) 컨슈머 인스턴스 생성 요청 시 `format`으로 `avro`를 지정하면, 이후의 메시지 조회 API 요청은 Accept: `application/vnd.kafka.avro.v2+json`을 사용해야 함.

### 1-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 1-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |

### 1-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| name | String | N | 컨슈머인스턴스명. 컨슈머 관련 URL에 사용되며 고유함. 생략 시 `kafka-rest-consumer-`로 시작하는 랜덤 ID를 사용. |
| format | String | Y | 소비되는 메시지 형식. `binary`, `avro`, `json`, `jsonschema`, `protobuf` 지정 가능, DF) `binary` |
| auto.offset.reset | String | N | 컨슈머의 auto.offset.reset 설정값 지정. DF) `earliest` |
| enable.auto.commit | String | N | 컨슈머의 auto.commit.enable 설정값 지정. DF) `false` |
| fetch.min.bytes | Integer | N | 컨슈머에 대해 fetch.min.bytes 설정값 지정. DF) `1` |
| request.timeout.ms | Integer | N | 컨슈머에 대해 request.timeout.ms 설정값 지정. DF) `30000` |

### 1-4. Response body

| Name | Type | Description |
| --- | --- | --- |
| instance_id | String | 이 그룹 내의 컨슈머 인스턴스에 대한 고유한 ID |
| base_uri | String | 이 컨슈머 인스턴스에 대한 후속 요청에 사용되는 기본 URI. 일반적으로 http://hostname:port/consumers/consumer_group/instances/instance_id와 같은 형식 |

### 1-5. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "name": "ci1",
  "format": "json",
  "auto.offset.reset": "earliest",
  "enable.auto.commit": "false"
}'
```

*Response ex.*

```json
{
  "instance_id": "ci1",
  "base_uri": "http://localhost:8080/consumers/cg1/instances/ci1"
}
```

## 2. 컨슈머 인스턴스 삭제

* 컨슈머 인스턴스 제거
* 요청을 보내면 컨슈머 인스턴스가 제거되고, 이후 해당 인스턴스에 대한 요청은 더 이상 유효하지 않음.

### 2-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance`
- Method: `DELETE`

### 2-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 2-3. Example

*Request ex.*

```bash
curl -X 'DELETE' \
  'http://localhost:8080/consumers/cg1/instances/ci1' \
  -H 'accept: */*'
```

*Response ex.*

```
No Content
```

## 3. 컨슈머 오프셋 목록 커밋

* request body가 없는 경우, 컨슈머 인스턴스가 현재까지 가져온 모든 레코드를 커밋함.
* 요청을 보내면 해당 오프셋들이 커밋되어, 컨슈머 인스턴스의 상태가 업데이트 됨.
> 오프셋 : 컨슈머가 브로커에게 마지막으로 커밋된 오프셋을 알리는 데 사용된다. 이미 소비된 데이터의 위치를 나타낸다.

### 3-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/offsets`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 3-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 3-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| offsets | - | N | 토픽-파티션에 대해 커밋할 오프셋 목록 |
| topic | String | N | 토픽명 |
| partition | Integer | N | 파티션 ID |
| offset | Long | N | 커밋할 오프셋 번호 |

### 3-4. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/offsets' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "offsets": [
    {
      "topic": "proxy-json-test",
      "partition": 0,
      "offset": 4
    },
    {
      "topic": "proxy-json-test2",
      "partition": 0,
      "offset": 2
    }
  ]
}'
```

*Response ex.*

```
No Content
```

## 4. 컨슈머 마지막 오프셋 조회

* 요청 토픽-파티션에 대해 마지막으로 커밋된 오프셋 정보 조회
* 응답의 오프셋들은 [컨슈머 오프셋 목록 커밋 API](#3-컨슈머-오프셋-목록-커밋) 혹은 다른 곳에서 커밋되었을 수 있음.

### 4-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/offsets`
- Method: `GET`
- Content-Type: `application/vnd.kafka.v2+json`

혹은

( 추천 )

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/committed-offsets`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 4-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 4-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| partitions | - | Y | 마지막으로 커밋된 오프셋 정보를 조회할 토픽-파티션 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |

### 4-4. Response body

| Name | Type | Description |
| --- | --- | --- |
| offsets | - | 커밋된 오프셋 목록 |
| topic | String | 오프셋이 커밋된 토픽명 |
| partition | Integer | 오프셋이 커밋된 파티션 ID |
| offset | Long | 커밋된 오프셋 |

### 4-5. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/committed-offsets' \
  -H 'accept: application/vnd.kafka.v2+json' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "partitions": [
    {
      "topic": "proxy-json-test",
      "partition": 0
    },
    {
      "topic": "proxy-json-test2",
      "partition": 0
    }
  ]
}'
```

*Response ex.*

```json
{
  "offsets": [
    {
      "topic": "proxy-json-test2",
      "partition": 0,
      "offset": 3
    },
    {
      "topic": "proxy-json-test",
      "partition": 0,
      "offset": 5
    }
  ]
}
```

## 5. 컨슈머 토픽 구독

* 주어진 토픽 목록 또는 토픽 패턴을 구독하여 동적으로 할당된 파티션을 받아옴.
* 이전의 구독 정보가 이미 존재한다면, 새로운 구독으로 대체됨.
* 컨슈머 인스턴스당 구독하는 방법(토픽 목록 or 토픽 패턴)은 한 가지만 사용 가능
  * 동일한 인스턴스에 대해 다른 방법으로 토픽 구독 실행 시, `java.lang.IllegalStateException: Subscription to topics, partitions and pattern are mutually exclusive` 오류 발생

### 5-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/subscription`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 5-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 5-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| topics | List\<String> | Y | 구독할 토픽명 목록 |
| topic_pattern | String | Y | REGEX(정규표현식) 패턴. `topic_pattern`과 `topics` 필드는 상호 배타적임. |

### 5-4. Example

*Request ex.*

- 토픽 목록

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/subscription' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "topics": [
    "proxy-json-test",
    "proxy-json-test2"
  ]
}'
```

- 토픽 패턴

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/subscription' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "topic_pattern": "proxy-json-.*"
}'
```

*Response ex.*

```
No Content
```

## 6. 컨슈머 토픽 구독 조회

* 컨슈머가 현재 구독 중인 모든 토픽 목록 조회

### 6-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/subscription`
- Method: `GET`

### 6-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 6-3. Response body

| Name | Type | Description |
| --- | --- | --- |
| topics | List\<String> | 구독 중인 토픽명 목록 |

### 6-4. Example

*Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci1/subscription' \
  -H 'accept: application/vnd.kafka.v2+json'
```

*Response ex.*

```json
{
  "topics": [
    "proxy-json-test2",
    "proxy-json-test"
  ]
}
```

## 7. 컨슈머 토픽 구독 취소

* 현재 구독 중인 모든 토픽에 대해 구독 취소

### 7-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/subscription`
- Method: `DELETE`

### 7-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 7-3. Example

*Request ex.*

```bash
curl -X 'DELETE' \
  'http://localhost:8080/consumers/cg1/instances/ci1/subscription' \
  -H 'accept: */*'
```

*Response ex.*

```
No Content
```

## 8. 컨슈머 파티션 수동 할당

* 요청 토픽-파티션 목록 수동 할당
* 이후에 해당 컨슈머 인스턴스가 메시지를 읽을 파티션을 명시적으로 설정

### 8-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/assignments`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 8-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 8-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| partitions | - | Y | 컨슈머에게 할당할 토픽-파티션 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |

### 8-4. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/assignments' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "partitions": [
    {
      "topic": "proxy-json-test",
      "partition": 0
    },
    {
      "topic": "proxy-json-test2",
      "partition": 4
    }
  ]
}'
```

*Response ex.*

```
No Content
```

## 9. 컨슈머 파티션 수동 할당 조회

* 현재 이 컨슈머 인스턴스에 수동으로 할당된 토픽-파티션 목록 조회

### 9-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/assignments`
- Method: `GET`

### 9-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 9-3. Response body

| Name | Type | Description |
| --- | --- | --- |
| partitions | - | Y | 컨슈머에게 수동으로 할당된 토픽-파티션 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |

### 9-4. Example

*Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci1/assignments' \
  -H 'accept: application/vnd.kafka.v2+json'
```

*Response ex.*

```json
{
  "partitions": [
    {
      "topic": "proxy-json-test",
      "partition": 0
    },
    {
      "topic": "proxy-json-test2",
      "partition": 4
    }
  ]
}
```

## 10. 컨슈머 패치 오프셋 업데이트

* 컨슈머 인스턴스가 다음에 레코드를 가져올 때 사용할 패치 오프셋 정보를 덮어씀.
* 컨슈머 인스턴스가 다음에 가져올 레코드의 시작 위치를 지정할 수 있게 함.
* 컨슈머 인스턴스에게 **현재 할당된 토픽-파티션에 대해서만** 패치 오프셋 업데이트가 가능하므로 주의.
> 패치 오프셋 : 다음에 읽을 데이터의 위치. 아직 읽지 않은 다음 데이터의 시작점을 나타낸다. 브로커로부터 데이터를 요청할 때 사용된다.

### 10-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/positions`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 10-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 10-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| offsets | - | Y | 업데이트할 패치 오프셋 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |
| offset | Long | Y | 다음에 가져올 레코드의 오프셋 번호 |

### 10-4. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/positions' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "offsets": [
    {
      "topic": "proxy-json-test",
      "partition": 0,
      "offset": 3
    }
  ]
}'
```

*Response ex.*

```
No Content
```

## 11. 컨슈머 오프셋 이동 : BEGINNING

* 요청 토픽-파티션 목록에 대해 첫 번째 오프셋으로 이동
* 각 파티션에서 가장 초기의 데이터 위치로 이동
* 컨슈머 인스턴스에게 **현재 할당된 토픽-파티션에 대해서만** 패치 오프셋 업데이트가 가능하므로 주의.

### 11-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/positions/beginning`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 11-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 11-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| partitions | - | Y | 토픽-파티션 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |

### 11-4. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/positions/beginning' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "partitions": [
    {
      "topic": "proxy-json-test",
      "partition": 0
    }
  ]
}'
```

*Response ex.*

```
No Content
```

## 12. 컨슈머 오프셋 이동 : END

* 요청 토픽-파티션 목록에 대해 마지막 오프셋으로 이동
* 각 파티션에서 가장 최신의 데이터 위치로 이동
* 컨슈머 인스턴스에게 **현재 할당된 토픽-파티션에 대해서만** 패치 오프셋 업데이트가 가능하므로 주의.

### 12-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/positions/end`
- Method: `POST`
- Content-Type: `application/vnd.kafka.v2+json`

### 12-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

### 12-3. Request body

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| partitions | - | Y | 토픽-파티션 목록 |
| topic | String | Y | 토픽명 |
| partition | Integer | Y | 파티션 ID |

### 12-4. Example

*Request ex.*

```bash
curl -X 'POST' \
  'http://localhost:8080/consumers/cg1/instances/ci1/positions/end' \
  -H 'accept: */*' \
  -H 'Content-Type: application/vnd.kafka.v2+json' \
  -d '{
  "partitions": [
    {
      "topic": "proxy-json-test",
      "partition": 0
    }
  ]
}'
```

*Response ex.*

```
No Content
```

## 13. 토픽-파티션 메시지 조회

* [컨슈머 토픽 구독 API](#5-컨슈머-토픽-구독) 또는 [컨슈머 파티션 수동 할당 API](#8-컨슈머-파티션-수동-할당)를 사용하여 지정된 토픽, 파티션에 대한 메시지 조회
* 이 요청으로 반환되는 메시지 형식은 이전에 컨슈머 인스턴스 생성 요청에서 `format`에 지정한 형식에 따라 결정되며, `accept` 헤더의 메시지 형식과 일치해야 함(일치하지 않으면 에러 코드 `40601` 발생).

### 13-1. URL

- URL: `http://{SERVER_URL}:{SERVER_PORT}/consumers/:group_name/instances/:instance/records`
- Method: `GET`
- accept: `application/vnd.kafka.binary.v2+json`, `application/vnd.kafka.json.v2+json`, `application/vnd.kafka.avro.v2+json`, `application/vnd.kafka.jsonschema.v2+json`, `application/vnd.kafka.protobuf.v2+json`

### 13-2. Request parameters

Path parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| group_name | String | Y | 컨슈머그룹명 |
| instance | String | Y | 컨슈머 인스턴스명 |

Query parameter

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| timeout | Long | N | 프록시 서버가 응답에 소요하는 최대 시간(밀리초). 컨슈머 인스턴스 설정값 중 `request.timeout.ms` 보다 값이 작은 경우에만 적용됨. |
| max_bytes | Long | N | 프록시 서버가 응답할 메시지 키, 값의 최대 바이트 수. 컨슈머 인스턴스 설정값 중 `fetch.max.bytes` 보다 값이 작은 경우에만 적용됨. |

> 프록시 서버가 응답에 소요하는 최대 시간을 제어하는 설정값 : `timeout`, `max_bytes`, `fetch.min.bytes`

### 13-3. Response body

| Name | Type | Description |
| --- | --- | --- |
| topic | String | 토픽명 |
| key | Object(`JSON`) / byte\[](`BINARY`) / JsonNode(`AVRO`, `JSONSCHEMA`, `PROTOBUF`) | 메시지 키(key). 메시지 타입에 따라 응답 형식이 다름. |
| value | Object(`JSON`) / byte\[](`BINARY`) / JsonNode(`AVRO`, `JSONSCHEMA`, `PROTOBUF`) | 메시지 값(value). 메시지 타입에 따라 응답 형식이 다름. |
| partition | Integer | 메시지의 파티션 ID |
| offset | Long | 메시지의 오프셋 번호 |

### 13-4. Example

*Binary Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci2/records?timeout=3000&max_bytes=300000' \
  -H 'accept: application/vnd.kafka.binary.v2+json'
```

*Binary Response ex.*

```json
[
  {
    "topic": "proxy-binary-test",
    "key": null,
    "value": "a2Fma2E=",
    "partition": 0,
    "offset": 0
  },
  {
    "topic": "proxy-binary-test",
    "key": null,
    "value": "bG9ncw==",
    "partition": 0,
    "offset": 1
  }
]
```

*Json Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci1/records?timeout=3000&max_bytes=300000' \
  -H 'accept: application/vnd.kafka.json.v2+json'
```

*Json Response ex.*

```json
[
  {
    "topic": "proxy-json-test",
    "key": null,
    "value": 53.5,
    "partition": 0,
    "offset": 5
  },
  {
    "topic": "proxy-json-test",
    "key": "somekey",
    "value": {
      "foo": "bar"
    },
    "partition": 0,
    "offset": 6
  }
]
```

*Avro Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci3/records?timeout=3000&max_bytes=300000' \
  -H 'accept: application/vnd.kafka.avro.v2+json'
```

*Avro Response ex.*

```json
[
  {
    "topic": "proxy-avro-test",
    "key": null,
    "value": {
      "id": 42
    },
    "partition": 0,
    "offset": 4
  },
  {
    "topic": "proxy-avro-test",
    "key": null,
    "value": {
      "id": 56
    },
    "partition": 0,
    "offset": 5
  }
]
```

*Json Schema Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg1/instances/ci4/records?timeout=3000&max_bytes=300000' \
  -H 'accept: application/vnd.kafka.jsonschema.v2+json'
```

*Json Schema Response ex.*

```json
[
  {
    "topic": "proxy-jsonschema-test",
    "key": {
      "foo": true,
      "bar": 92
    },
    "value": {
      "foo": "string",
      "bar": 10
    },
    "partition": 0,
    "offset": 3
  }
]
```

*Protobuf Request ex.*

```bash
curl -X 'GET' \
  'http://localhost:8080/consumers/cg2/instances/ci5/records?timeout=3000&max_bytes=300000' \
  -H 'accept: application/vnd.kafka.protobuf.v2+json'
```

*Protobuf Response ex.*

```json
[
  {
    "topic": "proxy-protobuf-test",
    "key": {
      "f1": "fookey"
    },
    "value": {
      "f1": "fooval",
      "f2": 55
    },
    "partition": 0,
    "offset": 0
  },
  {
    "topic": "proxy-protobuf-test2",
    "key": {
      "f1": "",
      "f2": 11,
      "f3": true
    },
    "value": {
      "f1": "fooval",
      "f2": 55
    },
    "partition": 0,
    "offset": 0
  }
]
```
