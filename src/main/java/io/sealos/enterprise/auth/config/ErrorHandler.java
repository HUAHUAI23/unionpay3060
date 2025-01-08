package io.sealos.enterprise.auth.config;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.sealos.enterprise.auth.exception.BusinessException;
import io.sealos.enterprise.auth.exception.ErrorCode;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.model.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    public static void configure(Javalin app) {
        // 404 处理
        app.error(404, ctx -> {
            String errorId = generateErrorId();
            logRequestInfo(errorId, ctx);
            ctx.json(ApiResponse.error(
                    ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "Resource not found: " + ctx.path(),
                    errorId));
        });

        // 业务异常处理
        app.<BusinessException>exception(BusinessException.class, (e, ctx) -> {
            String errorId = generateErrorId();
            logError(errorId, e, ctx);
            ctx.status(e.getStatusCode() != null ? e.getStatusCode() : 400);
            ctx.json(ApiResponse.error(e.getErrorCode(), e.getMessage(), errorId));
        });

        // 参数验证异常处理
        app.<IllegalArgumentException>exception(IllegalArgumentException.class, (e, ctx) -> {
            String errorId = generateErrorId();
            logError(errorId, e, ctx);
            ctx.status(400);
            ctx.json(ApiResponse.error(
                    ErrorCode.VALIDATION_ERROR.getCode(),
                    e.getMessage(),
                    errorId));
        });

        // 未知异常处理
        app.<Exception>exception(Exception.class, (e, ctx) -> {
            String errorId = generateErrorId();
            logError(errorId, e, ctx);
            ctx.status(500);
            ctx.json(buildErrorResponse(e, errorId));
        });
    }

    private static void logError(String errorId, Exception e, Context ctx) {
        String requestInfo = buildRequestInfo(ctx);
        logger.error("USER ID: {}, RegionUid: {}, Error occurred - ID: {}, Request: {}",
                ((UserDTO) ctx.attribute("user")).getUserId(),
                ((UserDTO) ctx.attribute("user")).getRegionUid(),
                errorId,
                requestInfo,
                e);
    }

    private static void logRequestInfo(String errorId, Context ctx) {
        String requestInfo = buildRequestInfo(ctx);
        logger.warn("USER ID: {}, RegionUid: {}, Request failed - ID: {}, Request: {}",
                ((UserDTO) ctx.attribute("user")).getUserId(),
                ((UserDTO) ctx.attribute("user")).getRegionUid(),
                errorId,
                requestInfo);
    }

    private static String buildRequestInfo(Context ctx) {
        return String.format(
                "Method: %s, Path: %s, IP: %s, Headers: %s, Payload: %s",
                ctx.method(),
                ctx.path(),
                ctx.ip(),
                ctx.headerMap(),
                getRequestBody(ctx));
    }

    private static String getRequestBody(Context ctx) {
        StringBuilder requestInfo = new StringBuilder();

        if (EnvConfig.isDevelopment()) {
            // Add query parameters if present
            if (!ctx.queryParamMap().isEmpty()) {
                requestInfo.append("Query: ").append(ctx.queryParamMap()).append(", ");
            }

            // Add body
            try {
                String body = ctx.body();
                requestInfo.append("Body: ");
                requestInfo.append(body.length() > 1000 ? body.substring(0, 1000) + "..." : body);
            } catch (Exception e) {
                requestInfo.append("[Unable to read body]");
            }
        } else {
            // Redact both query and body in production
            requestInfo.append("Query: [REDACTED IN PRODUCTION], ");
            requestInfo.append("Body: [REDACTED IN PRODUCTION]");
        }

        return requestInfo.toString();
    }

    private static ApiResponse<?> buildErrorResponse(Exception e, String errorId) {
        String message = EnvConfig.isDevelopment() ? e.getMessage()
                : "An unexpected error occurred. Please contact support with Error ID: " + errorId;

        return ApiResponse.error(
                ErrorCode.SYSTEM_ERROR.getCode(),
                message,
                errorId,
                EnvConfig.isDevelopment() ? e.getStackTrace() : null);
    }

    private static String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

// example
// 抛出业务异常
// throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid account
// number");

// 带状态码的业务异常
// throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Access denied", 403);