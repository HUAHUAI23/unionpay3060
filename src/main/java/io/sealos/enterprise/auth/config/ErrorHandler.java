package io.sealos.enterprise.auth.config;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
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
            try {
                String errorId = generateErrorId();
                logRequestInfo(errorId, ctx);
                ctx.json(ApiResponse.error(
                        ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                        "Resource not found: " + ctx.path(),
                        errorId));
            } catch (Exception e) {
                logger.error("Odd error occurred while handling request: {}", e.getMessage());
                throw new InternalServerErrorResponse("Error occurred ");
            }
        });

        // 业务异常处理
        app.<BusinessException>exception(BusinessException.class, (e, ctx) -> {
            try {
                String errorId = generateErrorId();
                logError(errorId, e, ctx);
                ctx.status(e.getStatusCode() != null ? e.getStatusCode() : 400);
                ctx.json(ApiResponse.error(e.getErrorCode(), e.getMessage(), errorId));
            } catch (Exception error) {
                logger.error("Odd error occurred while handling request: {}", error.getMessage());
                throw new InternalServerErrorResponse("Error occurred ");
            }
        });

        // 参数验证异常处理
        app.<IllegalArgumentException>exception(IllegalArgumentException.class, (e, ctx) -> {
            try {
                String errorId = generateErrorId();
                logError(errorId, e, ctx);
                ctx.status(400);
                ctx.json(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        e.getMessage(),
                        errorId));
            } catch (Exception error) {
                logger.error("Odd error occurred while handling request: {}", error.getMessage());
                throw new InternalServerErrorResponse("Error occurred ");
            }
        });

        // 未知异常处理
        app.<Exception>exception(Exception.class, (e, ctx) -> {
            try {
                String errorId = generateErrorId();
                logError(errorId, e, ctx);
                ctx.status(500);
                ctx.json(buildErrorResponse(e, errorId));
            } catch (Exception error) {
                logger.error("Odd error occurred while handling request: {}", error.getMessage());
                throw new InternalServerErrorResponse("Error occurred ");
            }
        });
    }

    private static void logError(String errorId, Exception e, Context ctx) {
        try {
            String requestInfo = buildRequestInfo(ctx);
            UserDTO user = ctx.attribute("user");
            String userId = user != null ? user.getUserId() : "unknown";
            String regionUid = user != null ? user.getRegionUid() : "unknown";

            logger.error("USER ID: {}, RegionUid: {}, Error occurred - ID: {}, Request: {}",
                    userId,
                    regionUid,
                    errorId,
                    requestInfo,
                    e);
        } catch (Exception loggingError) {
            // 如果记录日志时发生异常，使用简化的日志记录方式
            logger.error("Failed to log detailed error info - Error ID: {}, Original error: {}, Logging error: {}",
                    errorId,
                    e.getMessage(),
                    loggingError.getMessage());
        }
    }

    private static void logRequestInfo(String errorId, Context ctx) {
        try {
            String requestInfo = buildRequestInfo(ctx);
            UserDTO user = ctx.attribute("user");
            String userId = user != null ? user.getUserId() : "unknown";
            String regionUid = user != null ? user.getRegionUid() : "unknown";

            logger.warn("USER ID: {}, RegionUid: {}, Request failed - ID: {}, Request: {}",
                    userId,
                    regionUid,
                    errorId,
                    requestInfo);
        } catch (Exception loggingError) {
            logger.warn("Failed to log detailed request info - Error ID: {}, Error: {}",
                    errorId,
                    loggingError.getMessage());
        }
    }

    private static String buildRequestInfo(Context ctx) {
        StringBuilder requestInfo = new StringBuilder();

        try {
            requestInfo.append("Method: ").append(ctx.method()).append(", ");
            requestInfo.append("Path: ").append(ctx.path()).append(", ");
            requestInfo.append("IP: ").append(ctx.ip()).append(", ");
            requestInfo.append("Headers: ").append(ctx.headerMap()).append(", ");
            requestInfo.append(getRequestBody(ctx));
        } catch (Exception e) {
            return "Unable to build complete request info: " + e.getMessage();
        }

        return requestInfo.toString();
    }

    private static String getRequestBody(Context ctx) {
        StringBuilder requestInfo = new StringBuilder();

        if (EnvConfig.isDevelopment()) {
            try {
                // Add query parameters if present
                if (!ctx.queryParamMap().isEmpty()) {
                    requestInfo.append("Query: ").append(ctx.queryParamMap()).append(", ");
                }

                // Add body
                String body = ctx.body();
                requestInfo.append("Body: ");
                requestInfo.append(body.length() > 1000 ? body.substring(0, 1000) + "..." : body);
            } catch (Exception e) {
                requestInfo.append("[Unable to read request data: ").append(e.getMessage()).append("]");
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