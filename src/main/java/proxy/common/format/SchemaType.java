package proxy.common.format;

/**
 * (스키마 레지스트리에 스키마 등록 시 필요) 스키마 타입 목록
 */
public enum SchemaType {
    AVRO("AVRO"),
    PROTOBUF("PROTOBUF"),
    JSON("JSON")
    ;

    private String schemaType;

    SchemaType(String schemaType) {
        this.schemaType = schemaType;
    }

    public String getSchemaType() {
        return this.schemaType;
    }

    /**
     * EmbeddedFormat -> SchemaType
     * @param format // 메시지 포맷
     * @return
     */
    public static String getSchemaType(EmbeddedFormat format) {
        if (format == null) return null;

        String schemaType = null;
        switch (format) {
            case AVRO:
                schemaType = AVRO.getSchemaType();
                break;
            case JSONSCHEMA:
                schemaType = JSON.getSchemaType();
                break;
            case PROTOBUF:
                schemaType = PROTOBUF.getSchemaType();
                break;
            default:
                schemaType = getDefaultSchemaType().getSchemaType();
                break;
        }
        return schemaType;
    }

    // 기본 스키마 포맷 : AVRO
    public static SchemaType getDefaultSchemaType() {
        return AVRO;
    }
}
