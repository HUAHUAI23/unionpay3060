package io.sealos.enterprise.auth.middleware;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.sealos.enterprise.auth.model.AppTokenPayload;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.utils.JwtUtilsHmacSHA256;

public class AuthMiddleware {
    public static void authenticate(Context ctx) {
        String token = ctx.header("Authorization");

        UserDTO userDTO = JwtUtilsHmacSHA256.parseToken(token, AppTokenPayload.class)
                .map(payload -> new UserDTO(
                        payload.getUserId(),
                        payload.getWorkspaceId(),
                        payload.getRegionUid()))
                .orElseThrow(() -> new UnauthorizedResponse("Invalid or expired token"));

        ctx.attribute("user", userDTO);
    }
}
