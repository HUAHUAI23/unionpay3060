package io.sealos.enterprise.auth.config;

import java.io.IOException;
import java.util.Properties;

public class EnvConfig {
    private static final Properties properties = new Properties();

    static {
        try {
            properties.load(EnvConfig.class.getClassLoader().getResourceAsStream("security.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load security.properties", e);
        }
    }

    private static final String APP_ENV = properties.getProperty("APP_ENV", "prod");
    private static final int DEFAULT_PORT = 2342;

    public static boolean isDevelopment() {
        return "dev".equals(APP_ENV);
    }

    public static boolean isProduction() {
        return !"dev".equals(APP_ENV);
    }

    public static int getServerPort() {
        String port = properties.getProperty("PORT");
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
        return properties.getProperty("secss.jwtSecret");
    }

    public static String getUnionpay3060Api() {
        return properties.getProperty("unionpay3060Api");
    }

    public static String getMerchantNo() {
        return properties.getProperty("secss.merNo");
    }

    public static String getEnvironment() {
        return APP_ENV;
    }
}