package com.yvette.api.staff;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TotpTest {

    // RFC 6238 Appendix B test vectors (SHA1, secret "12345678901234567890"), last 6 digits.
    @Test
    void matchesRfc6238Vectors() {
        byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertThat(Totp.codeAt(key, 59L / 30)).isEqualTo("287082");
        assertThat(Totp.codeAt(key, 1111111109L / 30)).isEqualTo("081804");
        assertThat(Totp.codeAt(key, 1234567890L / 30)).isEqualTo("005924");
    }

    @Test
    void base32RoundTripAndDriftWindow() {
        String secret = Totp.newSecret();
        assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
        byte[] key = Totp.base32Decode(secret);
        assertThat(Totp.base32Encode(key)).isEqualTo(secret);

        long now = 1_000_000;
        assertThat(Totp.matchingStep(secret, Totp.codeAt(key, now - 1), now)).isEqualTo(now - 1);
        assertThat(Totp.matchingStep(secret, Totp.codeAt(key, now - 3), now)).isEqualTo(-1);
        assertThat(Totp.matchingStep(secret, "abc", now)).isEqualTo(-1);
    }
}
