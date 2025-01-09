package io.sealos.enterprise.auth.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.sealos.enterprise.auth.model.AppTokenPayload;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private static final String JWT_SECRET_ENV = "JWT_SECRET";
    private static final long CLOCK_SKEW = 60;
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static SecretKey secretKey;

    static {
        initializeKey();
    }

    // TODO: 使用双重检查锁单例模式 线程安全
    private static void initializeKey() {
        try {
            String secret = loadJwtSecret();
            // 确保密钥长度至少为 256 位（32 字节）
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Failed to initialize JWT secret key", e);
            throw new JwtConfigurationException("JWT secret key initialization failed", e);
        }
    }

    private static String loadJwtSecret() {
        // 首先尝试从系统环境变量获取
        String secret = System.getenv(JWT_SECRET_ENV);
        // 如果系统环境变量中没有，则尝试从 .env 文件获取
        if (secret == null || secret.trim().isEmpty()) {
            secret = dotenv.get(JWT_SECRET_ENV);
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new JwtConfigurationException("JWT_SECRET not found in environment variables or .env file");
        }
        return secret.trim();
    }

    private static String extractToken(String token) {
        return token != null && token.startsWith(TOKEN_PREFIX) ? token.substring(TOKEN_PREFIX.length()) : token;
    }

    public static String createToken(AppTokenPayload payload, long expirationInSeconds) {
        if (payload == null || expirationInSeconds <= 0) {
            throw new IllegalArgumentException("Invalid payload or expiration time");
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationInSeconds * 1000);

        try {
            Map<String, Object> claims = objectMapper.convertValue(payload,
                    new TypeReference<Map<String, Object>>() {
                    });

            return Jwts.builder()
                    .header()
                    .keyId("key-id-1")
                    .type("JWT")
                    // .add("aName", "aValue") custom header

                    .and()
                    .claims(claims)
                    .issuedAt(now)
                    .expiration(expiration)
                    .signWith(secretKey, Jwts.SIG.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Failed to create JWT token", e);
            throw new JwtException("Failed to create JWT token", e);
        }
    }

    public static Optional<AppTokenPayload> parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            String actualToken = extractToken(token);
            Claims claims = parseAndVerifyClaims(actualToken);

            // 移除标准声明，只保留自定义数据
            Map<String, Object> customClaims = new HashMap<>(claims);
            customClaims.remove("exp");
            customClaims.remove("iat");

            return Optional.of(objectMapper.convertValue(customClaims, AppTokenPayload.class));
        } catch (Exception e) {
            logger.error("Failed to parse JWT token", e);
            return Optional.empty();
        }
    }

    private static Claims parseAndVerifyClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(CLOCK_SKEW)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        try {
            String actualToken = extractToken(token);
            Claims claims = parseAndVerifyClaims(actualToken);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            logger.error("Failed to check token expiration", e);
            return true;
        }
    }

    public static boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String actualToken = extractToken(token);
            parseAndVerifyClaims(actualToken);
            return !isTokenExpired(actualToken);
        } catch (Exception e) {
            logger.debug("Token validation failed", e);
            return false;
        }
    }

    public static class JwtConfigurationException extends RuntimeException {
        public JwtConfigurationException(String message) {
            super(message);
        }

        public JwtConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}