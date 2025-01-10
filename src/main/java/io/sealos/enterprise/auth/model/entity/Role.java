package io.sealos.enterprise.auth.model.entity;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    ANYONE, USER_READ, USER_WRITE
}