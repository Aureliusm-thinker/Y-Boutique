package com.yvette.api.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StaffDtos {

    private StaffDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /**
     * Either a session ({@code accessToken} set) or, when two-step sign-in is on, a short-lived
     * {@code mfaTicket} to exchange for a session at /login/mfa together with an authenticator code.
     */
    public record LoginResponse(String accessToken, Instant expiresAt, StaffDto staff,
                                boolean mfaRequired, String mfaTicket) {
        static LoginResponse session(String token, Instant expiresAt, StaffDto staff) {
            return new LoginResponse(token, expiresAt, staff, false, null);
        }

        static LoginResponse needsCode(String ticket) {
            return new LoginResponse(null, null, null, true, ticket);
        }
    }

    public record MfaLoginRequest(@NotBlank String mfaTicket, @NotBlank String code) {}

    public record MfaCodeRequest(@NotBlank String code) {}

    public record MfaSetupResponse(String secret, String otpauthUri) {}

    public record StaffDto(Long id, String email, String fullName, StaffRole role, boolean active,
                           Instant lastLoginAt, boolean mfaEnabled) {
        static StaffDto of(StaffUser s) {
            return new StaffDto(s.getId(), s.getEmail(), s.getFullName(), s.getRole(), s.isActive(),
                    s.getLastLoginAt(), s.isMfaEnabled());
        }
    }

    /** The signed-in staff member, plus whether this store requires two-step sign-in. */
    public record MeDto(Long id, String email, String fullName, StaffRole role, boolean active,
                        Instant lastLoginAt, boolean mfaEnabled, boolean mfaRequired) {
        static MeDto of(StaffUser s, boolean mfaRequired) {
            return new MeDto(s.getId(), s.getEmail(), s.getFullName(), s.getRole(), s.isActive(),
                    s.getLastLoginAt(), s.isMfaEnabled(), mfaRequired);
        }
    }

    public record CreateStaffRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull StaffRole role,
            @NotBlank @Size(min = 12, max = 128) String password) {}

    public record UpdateStaffRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotNull StaffRole role,
            boolean active) {}
}
