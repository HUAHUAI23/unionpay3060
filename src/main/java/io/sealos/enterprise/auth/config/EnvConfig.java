package io.sealos.enterprise.auth.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        return value != null ? value : System.getenv().getOrDefault(key, defaultValue);
    }

    private static String getEnv(String key) {
        String value = dotenv.get(key);
        return value != null ? value : System.getenv(key);
    }

    private static final String APP_ENV = getEnv("APP_ENV", "prod");
    private static final int DEFAULT_PORT = 2342;

    public static boolean isDevelopment() {
        return "dev".equals(APP_ENV);
    }

    public static boolean isProduction() {
        return !"dev".equals(APP_ENV);
    }

    public static int getServerPort() {
        String port = getEnv("PORT");
        if (port != null && !port.isEmpty()) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return DEFAULT_PORT;
            }
        }
        return DEFAULT_PORT;
    }

    public static String getJwtSecret() {
        return getEnv("JWT_SECRET");
    }

    public static String getUnionpay3060Api() {
        return getEnv("UNIONPAY_3060_API");
    }

    public static String getMerchantNo() {
        return getEnv("MERCHANT_NO");
    }

    public static String getConfigPath() {
        return getEnv("SECSS_CONFIG_PATH");
    }

    public static String getEnvironment() {
        return APP_ENV;
    }
}