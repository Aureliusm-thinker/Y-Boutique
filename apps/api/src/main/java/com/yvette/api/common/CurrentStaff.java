package com.yvette.api.common;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the signed-in staff member's id from their access token. */
public final class CurrentStaff {

    private CurrentStaff() {}

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
