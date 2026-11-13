package com.yvette.api.staff;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "staff_users")
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String passwordHash;
    private String fullName;

    @Enumerated(EnumType.STRING)
    private StaffRole role;

    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant lastLoginAt;

    private String mfaSecret;
    private boolean mfaEnabled;
    private long mfaLastStep;

    protected StaffUser() {}

    public StaffUser(String email, String passwordHash, String fullName, StaffRole role) {
        this.email = normaliseEmail(email);
        this.passwordHash = passwordHash;
        this.fullName = fullName.trim();
        this.role = role;
    }

    public static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void recordLogin() { this.lastLoginAt = Instant.now(); }
    public void changeRole(StaffRole role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void changeFullName(String fullName) { this.fullName = fullName.trim(); }
    public void changePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** Stores a new, not-yet-confirmed authenticator secret (replaces any pending one). */
    public void startMfaSetup(String secret) {
        if (mfaEnabled) throw new IllegalStateException("Two-step sign-in is already on");
        this.mfaSecret = secret;
    }

    public void enableMfa(long acceptedStep) {
        this.mfaEnabled = true;
        this.mfaLastStep = acceptedStep;
    }

    public void disableMfa() {
        this.mfaEnabled = false;
        this.mfaSecret = null;
        this.mfaLastStep = 0;
    }

    /** Accepts a code's time step only once, so an intercepted code can't be reused. */
    public boolean acceptMfaStep(long step) {
        if (step <= mfaLastStep) return false;
        this.mfaLastStep = step;
        return true;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public StaffRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public String getMfaSecret() { return mfaSecret; }
    public boolean isMfaEnabled() { return mfaEnabled; }
}
