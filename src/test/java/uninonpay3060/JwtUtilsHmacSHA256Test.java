package uninonpay3060;

import io.sealos.enterprise.auth.model.AppTokenPayload;
import io.sealos.enterprise.auth.utils.JwtUtilsHmacSHA256;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JwtUtilsHmacSHA256Test {
    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USER_UID = "test-user-uid";
    private static final String TEST_WORKSPACE_UID = "test-workspace-uid";
    private static final long ONE_HOUR = 3600;
    private static final long ONE_SECOND = 1;

    private static AppTokenPayload createTestPayload() {
        AppTokenPayload payload = new AppTokenPayload();
        payload.setUserId(TEST_USER_ID);
        payload.setUserUid(TEST_USER_UID);
        payload.setWorkspaceUid(TEST_WORKSPACE_UID);
        return payload;
    }

    private static void testCreateAndParseToken() {
        // Given
        AppTokenPayload testPayload = createTestPayload();

        // When
        String token = JwtUtilsHmacSHA256.createToken(testPayload, ONE_HOUR);
        Optional<AppTokenPayload> parsedPayload = JwtUtilsHmacSHA256.parseToken(token, AppTokenPayload.class);

        // Then
        assert token != null && !token.isEmpty() : "Token should not be null or empty";
        assert parsedPayload.isPresent() : "Parsed payload should be present";

        AppTokenPayload payload = parsedPayload.get();
        assert TEST_USER_ID.equals(payload.getUserId()) : "User ID mismatch";
        assert TEST_USER_UID.equals(payload.getUserUid()) : "User UID mismatch";
        assert TEST_WORKSPACE_UID.equals(payload.getWorkspaceUid()) : "Workspace UID mismatch";
        assert payload.getIssuedAt() != null : "Issued at should not be null";
        assert payload.getExpiration() != null : "Expiration should not be null";
        assert payload.getExpiration() > Instant.now().getEpochSecond() : "Token should not be expired";

        System.out.println("Create and parse token test passed");
    }

    private static void testParseValidJwtTokenString(String token) {
        // When
        Optional<AppTokenPayload> result = JwtUtilsHmacSHA256.parseToken(token, AppTokenPayload.class);

        // Then
        assert result.isPresent() : "Token should be successfully parsed";
        AppTokenPayload payload = result.get();
        assert "8U_4WZiuwl".equals(payload.getUserId()) : "User ID mismatch";
        assert "fa1fc838-6e4c-47cf-bf2b-9f79f6d36cb2".equals(payload.getUserUid()) : "User UID mismatch";
        assert "6ad7fb39-0748-47d1-90e6-650bac0512f0".equals(payload.getWorkspaceUid()) : "Workspace UID mismatch";

        System.out.println("Token parsing test passed");
        printTokenInfo(payload);
    }

    private static void testTokenExpiration() {
        // Given
        AppTokenPayload testPayload = createTestPayload();
        String token = JwtUtilsHmacSHA256.createToken(testPayload, ONE_SECOND);

        // When & Then
        assert JwtUtilsHmacSHA256.validateToken(token) : "Token should be valid initially";

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
            return;
        }

        assert !JwtUtilsHmacSHA256.validateToken(token) : "Token should be expired after waiting";
        System.out.println("Token expiration test passed");
    }

    private static void testInvalidTokens() {
        String[] invalidTokens = {
                null,
                "",
                "invalid.token.string",
                "invalid.token",
                "invalid"
        };

        for (String invalidToken : invalidTokens) {
            assert !JwtUtilsHmacSHA256.parseToken(invalidToken, AppTokenPayload.class).isPresent()
                    : "Invalid token should not parse successfully: " + invalidToken;
            assert !JwtUtilsHmacSHA256.validateToken(invalidToken)
                    : "Invalid token should not be valid: " + invalidToken;
        }

        System.out.println("Invalid tokens test passed");
    }

    private static void testNullPayload() {
        boolean exceptionThrown = false;
        try {
            JwtUtilsHmacSHA256.createToken(null, ONE_HOUR);
        } catch (NullPointerException e) {
            assert "Payload cannot be null".equals(e.getMessage()) : "Unexpected error message";
            exceptionThrown = true;
        }
        assert exceptionThrown : "Should throw NullPointerException for null payload";
        System.out.println("Null payload test passed");
    }

    private static void testInvalidExpirationTime() {
        AppTokenPayload payload = createTestPayload();

        // Test zero expiration
        boolean zeroExceptionThrown = false;
        try {
            JwtUtilsHmacSHA256.createToken(payload, 0);
        } catch (IllegalArgumentException e) {
            zeroExceptionThrown = true;
        }
        assert zeroExceptionThrown : "Should throw exception for zero expiration";

        // Test negative expiration
        boolean negativeExceptionThrown = false;
        try {
            JwtUtilsHmacSHA256.createToken(payload, -1);
        } catch (IllegalArgumentException e) {
            negativeExceptionThrown = true;
        }
        assert negativeExceptionThrown : "Should throw exception for negative expiration";

        System.out.println("Invalid expiration time test passed");
    }

    private static void printTokenInfo(AppTokenPayload payload) {
        System.out.println("\nToken 解析结果:");
        System.out.println("WorkspaceUid: " + payload.getWorkspaceUid());
        System.out.println("UserId: " + payload.getUserId());
        System.out.println("UserUid: " + payload.getUserUid());
        System.out.println("IssuedAt: " + payload.getIssuedAt());
        System.out.println("Expiration: " + payload.getExpiration());
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting JWT Utils tests...\n");

            String token = "xxxxxxxxxx";
            testCreateAndParseToken();
            // testParseValidJwtTokenString(token);
            testTokenExpiration();
            testInvalidTokens();
            testNullPayload();
            testInvalidExpirationTime();

            System.out.println("\nAll tests passed successfully!");
        } catch (AssertionError e) {
            System.err.println("\nTest failed: " + e.getMessage());
            throw e;
        }
    }
}