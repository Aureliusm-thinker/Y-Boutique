package com.yvette.api.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StaffDtos {

    private StaffDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginResponse(String accessToken, Instant expiresAt, StaffDto staff) {}

    public record StaffDto(Long id, String email, String fullName, StaffRole role, boolean active,
                           Instant lastLoginAt) {
        static StaffDto of(StaffUser s) {
            return new StaffDto(s.getId(), s.getEmail(), s.getFullName(), s.getRole(), s.isActive(),
                    s.getLastLoginAt());
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
