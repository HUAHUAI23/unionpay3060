package io.sealos.enterprise.auth.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Error error;

    private ApiResponse(boolean success, T data, Error error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String errorId) {
        return error(code, message, errorId, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, String errorId, Object detail) {
        return new ApiResponse<>(false, null, new Error(code, message, errorId, detail));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public Error getError() {
        return error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private final String code;
        private final String message;
        private final String errorId;
        private final long timestamp;
        private final Object detail;

        public Error(String code, String message, String errorId, Object detail) {
            this.code = code;
            this.message = message;
            this.errorId = errorId;
            this.timestamp = Instant.now().toEpochMilli();
            this.detail = detail;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorId() {
            return errorId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Object getDetail() {
            return detail;
        }
    }
} 