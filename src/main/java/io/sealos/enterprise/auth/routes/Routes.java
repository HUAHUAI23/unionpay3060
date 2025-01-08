package io.sealos.enterprise.auth.routes;

import io.javalin.Javalin;
import io.sealos.enterprise.auth.handler.EnterpriseAuthHandler;
import io.sealos.enterprise.auth.middleware.AuthMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Routes {
    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    public static void register(Javalin app) {

        logger.info("Register global before middleware...");

        // Add authentication middleware
        app.before(AuthMiddleware::authenticate);

        // app.beforeMatched(ctx -> {
        // // add check role logic here
        // });

        logger.info("Register route...");
        // route
        app.post("/enterprise-auth", EnterpriseAuthHandler::handleEnterpriseAuth);

        // Add more routes here as needed

        // route with role example
        // app.post("/enterprise-auth", EnterpriseAuthHandler::handleEnterpriseAuth,
        // Role.USER_WRITE);
    }
}