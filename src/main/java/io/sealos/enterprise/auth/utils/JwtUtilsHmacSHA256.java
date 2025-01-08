package io.sealos.enterprise.auth.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class JwtUtilsHmacSHA256 {

    private JwtUtilsHmacSHA256() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtUtilsHmacSHA256.class);
    private static final String PROPERTIES_FILE = "security.properties";
    private static final String SECRET_PROPERTY = "secss.jwtSecret";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long EXPIRATION_BUFFER = 5000; // 5 seconds buffer for clock skew

    private static volatile SecretKey secretKey;
    private static final Object lock = new Object();

    static {
        initializeKey();
    }

    // double-checked locking (DCL) singleton pattern thread safe
    // Probably not needed, since the class is loaded with static initialization only once, even in the multithreaded case
    private static void initializeKey() {
        if (secretKey == null) {
            synchronized (lock) {
                if (secretKey == null) {
                    try {
                        String secret = loadJwtSecret();
                        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                        secretKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
                    } catch (Exception e) {
                        logger.error("Failed to initialize JWT secret key", e);
                        throw new JwtConfigurationException("JWT secret key initialization failed", e);
                    }
                }
            }
        }
    }

    private static String loadJwtSecret() {
        Properties properties = new Properties();
        try (InputStream input = JwtUtilsHmacSHA256.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new JwtConfigurationException("Unable to find " + PROPERTIES_FILE);
            }
            properties.load(input);

            String secret = properties.getProperty(SECRET_PROPERTY);
            if (secret == null || secret.trim().isEmpty()) {
                throw new JwtConfigurationException(SECRET_PROPERTY + " is not configured in " + PROPERTIES_FILE);
            }

            return secret.trim();
        } catch (Exception e) {
            throw new JwtConfigurationException("Failed to load JWT secret from " + PROPERTIES_FILE, e);
        }
    }

    private static Mac createMacInstance() throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(secretKey);
        return mac;
    }

    private static String extractToken(String token) {
        if (token == null) {
            return null;
        }
        return token.startsWith(TOKEN_PREFIX) ? token.substring(TOKEN_PREFIX.length()).trim() : token.trim();
    }

    public static <T> String createToken(T payload, long expirationInSeconds) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        if (expirationInSeconds <= 0) {
            throw new IllegalArgumentException("Expiration time must be positive");
        }

        try {
            long currentTime = System.currentTimeMillis() / 1000;
            Map<String, Object> claims = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
            });
            claims.put("exp", currentTime + expirationInSeconds);
            claims.put("iat", currentTime);

            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            header.put("kid", UUID.randomUUID().toString()); // Dynamic key ID

            String headerEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));

            String signatureInput = headerEncoded + "." + payloadEncoded;
            byte[] signature = createMacInstance().doFinal(signatureInput.getBytes(StandardCharsets.UTF_8));

            return String.format("%s.%s.%s", headerEncoded, payloadEncoded,
                    Base64.getUrlEncoder().withoutPadding().encodeToString(signature));
        } catch (Exception e) {
            logger.error("Failed to create JWT token", e);
            throw new JwtTokenException("Failed to create JWT token", e);
        }
    }

    public static <T> Optional<T> parseToken(String token, Class<T> clazz) {
        try {
            String actualToken = extractToken(token);
            if (actualToken == null) {
                return Optional.empty();
            }

            String[] parts = actualToken.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            // Verify signature
            if (!verifySignature(parts[0] + "." + parts[1], parts[2])) {
                return Optional.empty();
            }

            // Parse and verify claims
            Map<String, Object> claims = decodePayload(parts[1]);
            if (isExpired(claims)) {
                return Optional.empty();
            }

            // Remove standard claims before converting to payload
            claims.remove("exp");
            claims.remove("iat");

            return Optional.of(objectMapper.convertValue(claims, clazz));
        } catch (Exception e) {
            logger.error("Failed to parse JWT token", e);
            return Optional.empty();
        }
    }

    private static boolean verifySignature(String signatureInput, String signaturePart) {
        try {
            byte[] expectedSignature = createMacInstance().doFinal(signatureInput.getBytes(StandardCharsets.UTF_8));
            byte[] actualSignature = Base64.getUrlDecoder().decode(signaturePart);
            return MessageDigest.isEqual(expectedSignature, actualSignature);
        } catch (Exception e) {
            logger.error("Signature verification failed", e);
            return false;
        }
    }

    private static Map<String, Object> decodePayload(String payloadPart) throws Exception {
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
        return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
        });
    }

    private static boolean isExpired(Map<String, Object> claims) {
        Number expNumber = (Number) claims.get("exp");
        return expNumber == null || (expNumber.longValue() * 1000 + EXPIRATION_BUFFER) < System.currentTimeMillis();
    }

    public static boolean validateToken(String token) {
        try {
            String actualToken = extractToken(token);
            if (actualToken == null) {
                return false;
            }

            String[] parts = actualToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Verify signature
            if (!verifySignature(parts[0] + "." + parts[1], parts[2])) {
                return false;
            }

            // Check expiration
            Map<String, Object> claims = decodePayload(parts[1]);
            return !isExpired(claims);
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

    public static class JwtTokenException extends RuntimeException {
        public JwtTokenException(String message) {
            super(message);
        }

        public JwtTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}