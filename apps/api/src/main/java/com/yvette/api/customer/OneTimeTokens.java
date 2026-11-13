package com.yvette.api.customer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Single-use links (verify email, reset password). Only a SHA-256 hash of each token is stored. */
@Component
public class OneTimeTokens {

    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

    private final JdbcClient jdbc;
    private final SecureRandom random = new SecureRandom();

    public OneTimeTokens(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String create(Long customerId, Purpose purpose, Duration ttl) {
        // A new link replaces any earlier unused one for the same purpose.
        jdbc.sql("UPDATE customer_tokens SET used_at = now() WHERE customer_id = :c AND purpose = :p AND used_at IS NULL")
                .param("c", customerId).param("p", purpose.name()).update();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("""
                INSERT INTO customer_tokens (customer_id, purpose, token_hash, expires_at)
                VALUES (:c, :p, :h, :e)""")
                .param("c", customerId).param("p", purpose.name()).param("h", hash(token))
                .param("e", Timestamp.from(Instant.now().plus(ttl)))
                .update();
        return token;
    }

    /** Uses up the token and returns its customer id, or null if it is unknown, used or expired. */
    public Long consume(String token, Purpose purpose) {
        return jdbc.sql("""
                        UPDATE customer_tokens SET used_at = now()
                        WHERE token_hash = :h AND purpose = :p AND used_at IS NULL AND expires_at > now()
                        RETURNING customer_id""")
                .param("h", hash(token)).param("p", purpose.name())
                .query(Long.class).optional().orElse(null);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
