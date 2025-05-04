package proxy.common.format;

/**
 * 프록시 서버에서 지원하는 메시지 타입 목록
 */
public enum EmbeddedFormat {
    BINARY {
        @Override
        public boolean requiresSchema() {
            return false;
        }
    },

    JSON {
        @Override
        public boolean requiresSchema() {
            return false;
        }
    },

    STRING {
        @Override
        public boolean requiresSchema() {
            return false;
        }
    },

    AVRO {
        @Override
        public boolean requiresSchema() {
            return true;
        }
    },

    JSONSCHEMA {
        @Override
        public boolean requiresSchema() {
            return true;
        }
    },

    PROTOBUF {
        @Override
        public boolean requiresSchema() {
            return true;
        }
    };

    public abstract boolean requiresSchema(); // 스키마 필요 여부
}
