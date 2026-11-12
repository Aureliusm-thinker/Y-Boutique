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

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, @Value("${yvette.security.token-ttl:PT8H}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
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
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
