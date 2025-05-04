package proxy.common.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

/**
 * Protobuf Schema utility 클래스
 */
public class ProtobufSchemaUtil {
    private static final ObjectMapper jsonMapper = JacksonMapper.INSTANCE;
    private static final String PROTO2 = "proto2";
    private static final String PROTO3 = "proto3";
    private static final String REQUIRED = "required";
    private static final String OPTIONAL = "optional";

    /**
     * 스키마(내용)를 기반으로 디스크립터 생성
     * @param rawSchema // 스키마(내용)
     * @return
     * @throws IOException
     * @throws DescriptorValidationException 
     */
    public static Descriptors.Descriptor getDescriptor(String rawSchema) throws IOException, DescriptorValidationException {
        String syntax = extractSyntax(rawSchema); // 적용된 프로토 문법 버전
        String messageTypeName = extractMessageTypeName(rawSchema); // 메시지 타입명

        // 파일 디스크립터 생성
        Descriptors.FileDescriptor fileDescriptor = parseSchema(rawSchema, syntax, messageTypeName);
        return fileDescriptor.getMessageTypes().get(0); // 단일 메시지 타입
    }

    /**
     * 스키마 메시지 타입 파싱
     * @param rawSchema       // 스키마(내용)
     * @param syntax          // 프로토 문법 (proto2 | proto3)
     * @param messageTypeName // 메시지 타입명
     * @return
     * @throws IOException
     * @throws DescriptorValidationException 
     */
    private static Descriptors.FileDescriptor parseSchema(String rawSchema, String syntax, String messageTypeName) throws IOException, DescriptorValidationException {
        // 프로토버프 스키마
        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder();
        fileDescriptorProtoBuilder.setSyntax(syntax); // 프로토 문법 세팅

        // 메시지 타입
        DescriptorProtos.DescriptorProto.Builder descriptorProtoBuilder = DescriptorProtos.DescriptorProto.newBuilder();
        descriptorProtoBuilder.setName(messageTypeName); // 메시지 타입명 세팅

        // 메시지 필드 목록
        List<String> fieldDefinitions = extractFields(rawSchema);

        // proto3 문법이 적용된 경우
        if (syntax.equals(PROTO3)) {
            for (String fieldDefinition : fieldDefinitions) {
                if (fieldDefinition.trim().isEmpty()) continue;

                String[] parts = fieldDefinition.trim().split("\\s+");
                String fieldType = parts[0].trim(); // 필드 타입
                String fieldName = parts[1].trim(); // 필드명
                int fieldNumber = Integer.parseInt(parts[3].trim()); // 필드 식별자

                // 필드값 세팅
                DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder();
                fieldDescriptorProtoBuilder.setType(getFieldType(fieldType));
                fieldDescriptorProtoBuilder.setName(fieldName);
                fieldDescriptorProtoBuilder.setNumber(fieldNumber);

                // 필드 정보 추가
                descriptorProtoBuilder.addField(fieldDescriptorProtoBuilder);
            }
        
        // proto2 문법이 적용된 경우
        } else if (syntax.equals(PROTO2)) {
            for (String fieldDefinition : fieldDefinitions) {
                if (fieldDefinition.trim().isEmpty()) continue;

                String[] parts = fieldDefinition.trim().split("\\s+");
                String fieldLabel = parts[0].trim(); // 필드 라벨
                String fieldType = parts[1].trim();  // 필드 타입
                String fieldName = parts[2].trim();  // 필드명
                int fieldNumber = Integer.parseInt(parts[4].trim()); // 필드 식별자
    
                // 필드값 세팅
                DescriptorProtos.FieldDescriptorProto.Builder fieldDescriptorProtoBuilder = DescriptorProtos.FieldDescriptorProto.newBuilder();
                fieldDescriptorProtoBuilder.setType(getFieldType(fieldType));
                fieldDescriptorProtoBuilder.setName(fieldName);
                fieldDescriptorProtoBuilder.setNumber(fieldNumber);
                // 필드 라벨 (required | optional | repeated)
                fieldDescriptorProtoBuilder.setLabel(fieldLabel.equals(REQUIRED)
                    ? DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED
                    : fieldLabel.equals(OPTIONAL)
                        ? DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL
                        : DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
                    
                // 필드 정보 추가
                descriptorProtoBuilder.addField(fieldDescriptorProtoBuilder);
            }
        }

        // 메시지 타입 정보 추가
        fileDescriptorProtoBuilder.addMessageType(descriptorProtoBuilder); // 단일 메시지 타입

        // 파일 디스크립터 생성
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = fileDescriptorProtoBuilder.build();
        Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);

        return fileDescriptor;
    }

    /**
     * 프로토 문법 반환
     * @param rawSchema // 스키마(내용)
     * @return
     * @throws IllegalArgumentException // syntax값이 "proto2" 또는 "proto3"이 아닌 경우 예외 반환
     */
    private static String extractSyntax(String rawSchema) {
        Pattern pattern = Pattern.compile("syntax\\s*=\\s*\"(proto[23])\"");
        Matcher matcher = pattern.matcher(rawSchema);

        if (matcher.find()) return matcher.group(1);
        else throw new IllegalArgumentException("Syntax not found in raw schema");
    }

    /**
     * 메시지 타입명 반환
     * @param rawSchema // 스키마(내용)
     * @return
     * @throws IllegalArgumentException // 메시지 타입명을 찾을 수 없는 경우 예외 반환
     */
    private static String extractMessageTypeName(String rawSchema) {
        Pattern pattern = Pattern.compile("message\\s+([\\w.]+)\\s*\\{");
        Matcher matcher = pattern.matcher(rawSchema);

        if (matcher.find()) return matcher.group(1);
        else throw new IllegalArgumentException("Message type name not found in raw schema");
    }

    /**
     * 스키마(내용)로부터 메시지 필드 목록 추출
     * @param rawSchema // protobuf schema
     * @return
     */
    private static List<String> extractFields(String rawSchema) {
        Pattern pattern = Pattern.compile("message\\s+[\\w.]+\\s*\\{([^\\}]+)\\}");
        Matcher matcher = pattern.matcher(rawSchema);

        List<String> fieldDefinitions = new ArrayList<>();
        while (matcher.find()) {
            String fields = matcher.group(1);
            fieldDefinitions.addAll(Arrays.asList(fields.split(";"))); // ";"로 필드 구분
        }
        return fieldDefinitions;
    }

    /**
     * 메시지 필드 타입 조회
     * @param fieldDefinition // 필드 타입
     * @return
     */
    private static DescriptorProtos.FieldDescriptorProto.Type getFieldType(String fieldDefinition) {
        String[] parts = fieldDefinition.trim().split("\\s+");
        String fieldType = parts[0].trim();

        switch (fieldType) {
            case "message":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;
            case "enum":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM;
            case "string":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
            case "bytes":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
            case "int32":
            case "uint32":
            case "fixed32":
            case "sfixed32":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32;
            case "int64":
            case "uint64":
            case "fixed64":
            case "sfixed64":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64;
            case "float":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
            case "double":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
            case "bool":
                return DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
            default:
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * Convert data type JsonNode -> String
     * @param data
     * @return
     * @throws IOException
     */
    private static String toString(JsonNode data) throws IOException {
        StringWriter writer = new StringWriter();
        jsonMapper.writeValue(writer, data);
        return writer.toString();
    }

    /**
     * Convert data type JsonNode -> Object(Message)
     * @param data   // protobuf content
     * @param schema // protobuf schema
     * @return
     * @throws IOException
     * @throws DescriptorValidationException
     */
    public static Object toObject(String rawSchema, JsonNode data)
            throws IOException, DescriptorValidationException {
        
        // get protobuf schema (descriptor)
        Descriptor descriptor = getDescriptor(rawSchema);

        // JsonNode -> String data
        String value = toString(data);

        // String -> Message data
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
        JsonFormat.parser().merge(value, builder);

        return builder.build();
    }
}
