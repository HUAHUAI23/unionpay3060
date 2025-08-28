package io.sealos.enterprise.auth.routes;

import io.javalin.Javalin;
import io.sealos.enterprise.auth.handler.EnterpriseAuthHandler;
import io.sealos.enterprise.auth.handler.BankHandler;
import io.sealos.enterprise.auth.middleware.AuthMiddleware;
import io.sealos.enterprise.auth.constants.ApiVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Routes {
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static void register(Javalin app) {
        logger.info("Register global before matched middleware...");
        app.beforeMatched(AuthMiddleware::authenticate);

        logger.info("Register routes...");

        app.get("/test", ctx -> {
            ctx.result("Test response");
            ctx.status(200);
        });

        // 注册 v1 版本的 API 端点
        app.post(ApiVersion.getDefaultVersion() + "/enterprise-auth",
                EnterpriseAuthHandler::handleEnterpriseAuth);

        // 银行列表
        app.get(ApiVersion.getDefaultVersion() + "/banks", BankHandler::getBanks);

        // 如果有更多端点，继续添加
        // app.get(API_VERSION + "/other-endpoint", OtherHandler::handle);

        // Add more routes here as needed
        // route with role example
        // app.post(API_VERSION + "/enterprise-auth",
        // EnterpriseAuthHandler::handleEnterpriseAuth, Role.USER_WRITE);
    }
}