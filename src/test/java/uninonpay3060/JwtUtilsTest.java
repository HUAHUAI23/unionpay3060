package uninonpay3060;

import io.sealos.enterprise.auth.model.AppTokenPayload;
import io.sealos.enterprise.auth.utils.JwtUtils;
import java.util.Optional;

public class JwtUtilsTest {
    private static void testCreateAndParseToken() {
        // Prepare test payload
        AppTokenPayload testPayload = new AppTokenPayload();
        testPayload.setUserId("test-user-id");
        testPayload.setUserUid("test-user-uid");
        testPayload.setWorkspaceUid("test-workspace-uid");

        // Create token with 1 hour expiration
        String token = JwtUtils.createToken(testPayload, 3600);
        assert token != null && !token.isEmpty() : "Token should not be null or empty";

        // Parse the token
        Optional<AppTokenPayload> parsedPayload = JwtUtils.parseToken(token);
        assert parsedPayload.isPresent() : "Parsed payload should be present";

        // Verify payload contents
        AppTokenPayload payload = parsedPayload.get();
        assert testPayload.getUserId().equals(payload.getUserId()) : "User ID mismatch";
        assert testPayload.getUserUid().equals(payload.getUserUid()) : "User UID mismatch";
        assert testPayload.getWorkspaceUid().equals(payload.getWorkspaceUid()) : "Workspace UID mismatch";
        assert payload.getIssuedAt() != null : "Issued at should not be null";
        assert payload.getExpiration() != null : "Expiration should not be null";

        System.out.println("Create and parse token test passed");
    }

    private static void testJwtTokenString(String token) {
        Optional<AppTokenPayload> payload = JwtUtils.parseToken(token);
        System.out.println(payload);
    }

    private static void testTokenExpiration() {
        AppTokenPayload testPayload = new AppTokenPayload();
        testPayload.setUserId("test-user-id");

        // Create token with 1 second expiration
        String token = JwtUtils.createToken(testPayload, 1);
        assert !JwtUtils.isTokenExpired(token) : "Token should not be expired immediately";

        // Wait for token to expire
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
            return;
        }

        assert JwtUtils.isTokenExpired(token) : "Token should be expired after waiting";
        System.out.println("Token expiration test passed");
    }

    private static void testInvalidToken() {
        String invalidToken = "invalid.token.string";

        // Parse invalid token should return empty optional
        Optional<AppTokenPayload> parsedPayload = JwtUtils.parseToken(invalidToken);
        assert !parsedPayload.isPresent() : "Invalid token should not parse successfully";

        // Invalid token should throw JwtException when checking expiration
        try {
            JwtUtils.isTokenExpired(invalidToken);
            assert false : "Should have thrown exception for invalid token";
        } catch (io.jsonwebtoken.JwtException e) {
            // Expected exception
            System.out.println("Invalid token test passed");
        }
    }

    public static void main(String[] args) {
        try {
            // String token = "xxxxxxxxxx";
            System.out.println("Starting JWT Utils tests...");
            testCreateAndParseToken();
            // testJwtTokenString(token);
            testTokenExpiration();
            testInvalidToken();
            System.out.println("All tests passed successfully!");
        } catch (AssertionError e) {
            System.err.println("Test failed: " + e.getMessage());
            throw e;
        }
    }
}