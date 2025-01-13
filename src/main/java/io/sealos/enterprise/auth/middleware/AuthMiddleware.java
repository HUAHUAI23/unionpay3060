package io.sealos.enterprise.auth.middleware;

import java.util.Set;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.sealos.enterprise.auth.model.AppTokenPayload;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.utils.JwtUtilsHmacSHA256;

public class AuthMiddleware {

    // API 文档路径 - 精确匹配
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/redoc",
            "/openapi",
            "/swagger");

    // 静态资源 - 严格版本前缀
    private static final String SWAGGER_VERSION = "5.17.14";
    private static final String REDOC_VERSION = "2.1.4";

    // 允许的静态资源路径模式 - 精确定义文件类型
    private static final Set<String> ALLOWED_STATIC_PATHS = Set.of(
            "/webjars/swagger-ui/" + SWAGGER_VERSION + "/swagger-ui.css",
            "/webjars/swagger-ui/" + SWAGGER_VERSION + "/swagger-ui-bundle.js",
            "/webjars/swagger-ui/" + SWAGGER_VERSION + "/swagger-ui-standalone-preset.js",
            "/webjars/swagger-ui/" + SWAGGER_VERSION + "/favicon-32x32.png",
            "/webjars/redoc/" + REDOC_VERSION + "/bundles/redoc.standalone.js");

    public static void authenticate(Context ctx) {
        String path = normalizePath(ctx.path());

        // 检查文档入口
        if (isExcludedPath(path)) {
            return;
        }

        // 检查静态资源
        if (isAllowedStaticPath(path)) {
            return;
        }

        // 认证处理
        String token = ctx.header("Authorization");
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedResponse("Missing authorization token");
        }

        UserDTO userDTO = JwtUtilsHmacSHA256.parseToken(token, AppTokenPayload.class)
                .map(payload -> new UserDTO(
                        payload.getUserId(),
                        payload.getWorkspaceId(),
                        payload.getRegionUid()))
                .orElseThrow(() -> new UnauthorizedResponse("Invalid or expired token"));

        ctx.attribute("user", userDTO);
    }

    private static String normalizePath(String path) {
        // 标准化处理，移除末尾斜杠
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static boolean isExcludedPath(String path) {
        // 文档入口精确匹配
        return EXCLUDED_PATHS.contains(path);
    }

    private static boolean isAllowedStaticPath(String path) {
        // 静态资源精确匹配
        return ALLOWED_STATIC_PATHS.contains(path);
    }
}
