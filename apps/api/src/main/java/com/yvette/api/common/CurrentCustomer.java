package com.yvette.api.common;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the signed-in customer's id from their access token (null for guests and staff). */
public final class CurrentCustomer {

    public static final String ROLE = "CUSTOMER";

    private CurrentCustomer() {}

    public static Long idOrNull(Jwt jwt) {
        if (jwt == null) return null;
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(ROLE) ? Long.valueOf(jwt.getSubject()) : null;
    }

    public static Long id(Jwt jwt) {
        Long id = idOrNull(jwt);
        if (id == null) throw new IllegalStateException("Not a customer session");
        return id;
    }
}
