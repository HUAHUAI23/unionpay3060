package io.sealos.enterprise.auth.exception;

public enum ErrorCode {
    RESOURCE_NOT_FOUND("404", "Resource not found"),
    BUSINESS_ERROR("BIZ-400", "Business error"),
    VALIDATION_ERROR("VAL-400", "Validation error"),
    SYSTEM_ERROR("SYS-500", "System error");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
} 