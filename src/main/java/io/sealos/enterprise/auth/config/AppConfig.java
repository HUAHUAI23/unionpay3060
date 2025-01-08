package io.sealos.enterprise.auth.config;

import io.javalin.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    public static void configure(JavalinConfig config) {
        // Configure CORS
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));

        if (EnvConfig.isDevelopment()) {
            // Configure debug logging
            config.bundledPlugins.enableDevLogging();

            // Configure request logging
            config.requestLogger.http((ctx, ms) -> {
                String path = ctx.path();
                String method = ctx.method().toString();
                int status = ctx.status().getCode();
                logger.info("{} {} - Status: {} - Time: {}ms", method, path, status, ms);
            });
        }

        // Configure other settings
        config.http.defaultContentType = "application/json";

        if (EnvConfig.isProduction()) {
            config.showJavalinBanner = false;
        }
    }
}