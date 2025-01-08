package io.sealos.enterprise.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JWT Token Payload 数据结构
 */
@Data
public class AppTokenPayload {
    @JsonProperty("workspaceUid")
    private String workspaceUid;

    @JsonProperty("workspaceId")
    private String workspaceId;

    @JsonProperty("regionUid")
    private String regionUid;

    @JsonProperty("userCrUid")
    private String userCrUid;

    @JsonProperty("userCrName")
    private String userCrName;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("userUid")
    private String userUid;

    @JsonProperty("iat")
    private Long issuedAt;

    @JsonProperty("exp")
    private Long expiration;
}