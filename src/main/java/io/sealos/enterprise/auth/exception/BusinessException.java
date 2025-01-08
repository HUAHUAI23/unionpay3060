package io.sealos.enterprise.auth.exception;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Integer statusCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.statusCode = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Integer statusCode) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}