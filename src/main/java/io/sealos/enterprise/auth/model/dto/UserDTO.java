package io.sealos.enterprise.auth.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UserDTO {
    private String userId;
    private String namespace;
    private String regionUid;
} 