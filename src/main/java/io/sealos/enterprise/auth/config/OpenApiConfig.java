package io.sealos.enterprise.auth.config;

import io.javalin.config.JavalinConfig;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;

import io.sealos.enterprise.auth.model.entity.Role;

public class OpenApiConfig {
    // OpenAPI specification path (Note: direct path configuration might be
    // deprecated in future versions)
    private static final String DOCS_PATH = EnvConfig.getDocsPath();
    private static final String SWAGGER_PATH = EnvConfig.getSwaggerPath();
    private static final String REDOC_PATH = EnvConfig.getRedocPath();

    public static void configure(JavalinConfig config) {
        // Configure OpenAPI
        config.registerPlugin(getOpenApiPlugin());

        // Register Swagger UI (Note: this configuration method might be deprecated in
        // future versions)
        config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
            swaggerConfig.setDocumentationPath(DOCS_PATH);
            swaggerConfig.setUiPath(SWAGGER_PATH);
        }));

        // Register ReDoc (Note: this configuration method might be deprecated in future
        // versions)
        config.registerPlugin(new ReDocPlugin(reDocConfig -> {
            reDocConfig.setDocumentationPath(DOCS_PATH);
            reDocConfig.setUiPath(REDOC_PATH);
        }));

    }

    private static OpenApiPlugin getOpenApiPlugin() {
        return new OpenApiPlugin(openApiConfig -> openApiConfig
                .withDocumentationPath(DOCS_PATH)
                .withRoles(Role.ANYONE)
                .withDefinitionConfiguration((version, definition) -> definition
                        .withInfo(info -> {
                            info.title("Sealos Enterprise Auth API")
                                    .summary("Authentication Service for Sealos Enterprise")
                                    .description("Enterprise Authentication Service API Documentation")
                                    .termsOfService("https://sealos.io")
                                    .contact("Sealos Team", "https://sealos.io", "support@sealos.io")
                                    .license("Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0", "Apache-2.0");
                        })
                        .withServer(server -> server
                                .description("Enterprise Auth Server")
                                .url("http://localhost:{port}{basePath}/")
                                .variable("port", "Server's port", String.valueOf(EnvConfig.getServerPort()),
                                        String.valueOf(EnvConfig.getServerPort()),
                                        String.valueOf(EnvConfig.getServerPort()))
                                .variable("basePath", "Base path of the server", "", "", "/v1", "v2"))

                        .withSecurity(security -> security
                                .withBearerAuth("Bearer") // Give it a specific name
                                .withGlobalSecurity("Bearer", globalSecurity -> {
                                }) // Make it global
                        )

                ));
    }
}