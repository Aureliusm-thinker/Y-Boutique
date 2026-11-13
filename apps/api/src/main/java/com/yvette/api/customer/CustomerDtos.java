package com.yvette.api.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {}

    // ---- auth

    public record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 8, max = 128) String password,
                                  @NotBlank @Size(max = 120) String fullName,
                                  boolean marketingOptIn) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record Session(String accessToken, Instant expiresAt, Profile customer) {}

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {}

    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record ResetPasswordRequest(@NotBlank @Size(max = 200) String token,
                                       @NotBlank @Size(min = 8, max = 128) String password) {}

    // ---- profile

    public record Preferences(List<@Size(max = 20) String> sizes, List<@Size(max = 30) String> colours,
                              List<@Size(max = 30) String> styles) {
        public static Preferences empty() {
            return new Preferences(List.of(), List.of(), List.of());
        }
    }

    public record AddressDto(Long id, String label, String fullName, String phone, String line1, String line2,
                             String city, String county, String postalCode, boolean isDefault) {
        static AddressDto of(CustomerAddress a) {
            return new AddressDto(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(),
                    a.getCity(), a.getCounty(), a.getPostalCode(), a.isDefault());
        }
    }

    public record Profile(Long id, String email, String fullName, String phone, boolean emailVerified,
                          boolean marketingOptIn, Preferences preferences, List<AddressDto> addresses,
                          Instant createdAt) {}

    public record UpdateProfileRequest(@NotBlank @Size(max = 120) String fullName, @Size(max = 20) String phone,
                                       boolean marketingOptIn) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 128) String newPassword) {}

    public record AddressRequest(@Size(max = 40) String label, @NotBlank @Size(max = 120) String fullName,
                                 @Size(max = 20) String phone, @NotBlank @Size(max = 200) String line1,
                                 @Size(max = 200) String line2, @NotBlank @Size(max = 80) String city,
                                 @NotBlank @Size(max = 40) String county, @Size(max = 20) String postalCode,
                                 boolean makeDefault) {}

    public record DeleteAccountRequest(@NotBlank String password) {}

    public record AttachCartRequest(@NotNull UUID cartToken) {}

    public record AttachCartResponse(UUID cartToken, int itemCount) {}

    public record StockAlertRequest(@NotNull Long variantId, @NotBlank @Email @Size(max = 254) String email) {}
}
