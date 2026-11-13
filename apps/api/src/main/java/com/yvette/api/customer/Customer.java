package com.yvette.api.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private Instant emailVerifiedAt;
    private boolean marketingOptIn;

    /** JSON: {"sizes":[],"colours":[],"styles":[]} */
    @JdbcTypeCode(SqlTypes.JSON)
    private String preferences = "{}";

    private Instant createdAt = Instant.now();
    private Instant lastLoginAt;

    protected Customer() {}

    public Customer(String email, String passwordHash, String fullName, boolean marketingOptIn) {
        this.email = normaliseEmail(email);
        this.passwordHash = passwordHash;
        this.fullName = fullName.strip();
        this.marketingOptIn = marketingOptIn;
    }

    public static String normaliseEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public void updateProfile(String fullName, String phone, boolean marketingOptIn) {
        this.fullName = fullName.strip();
        this.phone = phone;
        this.marketingOptIn = marketingOptIn;
    }

    public void setPreferences(String json) { this.preferences = json; }
    public void changePasswordHash(String hash) { this.passwordHash = hash; }
    public void markEmailVerified() { if (emailVerifiedAt == null) emailVerifiedAt = Instant.now(); }
    public void recordLogin() { this.lastLoginAt = Instant.now(); }

    public boolean isEmailVerified() { return emailVerifiedAt != null; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public boolean isMarketingOptIn() { return marketingOptIn; }
    public String getPreferences() { return preferences; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
