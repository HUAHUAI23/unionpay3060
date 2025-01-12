package io.sealos.enterprise.auth;

import io.javalin.Javalin;
import io.sealos.enterprise.auth.config.AppConfig;
import io.sealos.enterprise.auth.config.ErrorHandler;
import io.sealos.enterprise.auth.config.EnvConfig;
import io.sealos.enterprise.auth.routes.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting UnionPay 3060 Service in {} environment...", EnvConfig.getEnvironment());

        Javalin app = Javalin.create(AppConfig::configure);

        // Configure error handling
        ErrorHandler.configure(app);

        // Register routes
        Routes.register(app);

        // Start server
        int port = EnvConfig.getServerPort();
        app.start(port);

        logger.info("Server started on port {}", port);
        logger.info("Swagger UI available at: http://localhost:{}{}", port, EnvConfig.getSwaggerPath());
        logger.info("ReDoc available at: http://localhost:{}{}", port, EnvConfig.getRedocPath());
        logger.info("OpenAPI JSON available at: http://localhost:{}{}", port, EnvConfig.getDocsPath());
    }
}
