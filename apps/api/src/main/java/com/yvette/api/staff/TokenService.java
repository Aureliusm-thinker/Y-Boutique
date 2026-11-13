package com.yvette.api.staff;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived signed access tokens (JWT, HS256) for staff. */
@Service
public class TokenService {

    public static final String ISSUER = "yvette-api";
    public static final String ROLES_CLAIM = "roles";
    /** True when the session was opened with an authenticator code (two-step sign-in). */
    public static final String MFA_CLAIM = "mfa";

    private final JwtEncoder encoder;
    private final Duration ttl;
    private final Duration customerTtl;

    public TokenService(JwtEncoder encoder, @Value("${yvette.security.token-ttl:PT8H}") Duration ttl,
                        @Value("${yvette.security.customer-token-ttl:P30D}") Duration customerTtl) {
        this.encoder = encoder;
        this.ttl = ttl;
        this.customerTtl = customerTtl;
    }

    /** Shopper session (role CUSTOMER). Lives longer than staff sessions: "stay signed in". */
    public IssuedToken issueCustomer(Long customerId, String email, String name) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(customerTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(customerId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", email)
                .claim("name", name)
                .claim(ROLES_CLAIM, List.of("CUSTOMER"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new IssuedToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    public IssuedToken issue(StaffUser staff) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(staff.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", staff.getEmail())
                .claim("name", staff.getFullName())
                .claim(ROLES_CLAIM, List.of(staff.getRole().name()))
                .claim(MFA_CLAIM, staff.isMfaEnabled())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
