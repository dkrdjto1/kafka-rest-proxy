package proxy.common.exception;

import org.springframework.http.HttpStatus;

import proxy.common.exception.resource.Errors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder
@AllArgsConstructor
public class ProxyException extends RuntimeException {
    private HttpStatus status;    // http 상태
    private int code;             // 에러 코드
    private String message;       // 에러 메시지
    private String detailMessage; // 에러 상세 메시지
    private Errors errors;        // 프록시 에러

    /**
     * Proxy exception
     * @param errors
     */
    public ProxyException(Errors errors) {
        this.status = errors.status();
        this.code = errors.code();
        this.message = errors.message();
        this.errors = errors;
    }

    /**
     * Proxy exception with detail message
     * @param errors
     * @param detailMessage
     */
    public ProxyException(Errors errors, String detailMessage) {
        this.status = errors.status();
        this.code = errors.code();
        this.message = errors.message();
        this.detailMessage = detailMessage;
        this.errors = errors;
    }
}
