package com.yvette.api.payment;

import com.yvette.api.order.Order;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** One payment attempt (CHARGE) or refund (REFUND) for an order, via one provider. */
@Entity
@Table(name = "payments")
public class Payment {

    public enum Kind { CHARGE, REFUND }

    public enum Status { PENDING, SUCCEEDED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private String provider;

    @Enumerated(EnumType.STRING)
    private Order.PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private Kind kind = Kind.CHARGE;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private BigDecimal amount;
    private String providerReference;
    private String phone;
    private String redirectUrl;
    private String failureMessage;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Payment() {}

    public Payment(Long orderId, String provider, Order.PaymentMethod method, Kind kind, BigDecimal amount,
                   String phone) {
        this.orderId = orderId;
        this.provider = provider;
        this.method = method;
        this.kind = kind;
        this.amount = amount;
        this.phone = phone;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void started(String providerReference, String redirectUrl) {
        this.providerReference = providerReference;
        this.redirectUrl = redirectUrl;
    }

    public void succeeded(String providerReference) {
        this.status = Status.SUCCEEDED;
        if (providerReference != null) this.providerReference = providerReference;
        this.failureMessage = null;
    }

    public void failed(String message) {
        this.status = Status.FAILED;
        this.failureMessage = message == null ? null : message.substring(0, Math.min(300, message.length()));
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getProvider() { return provider; }
    public Order.PaymentMethod getMethod() { return method; }
    public Kind getKind() { return kind; }
    public Status getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getProviderReference() { return providerReference; }
    public String getPhone() { return phone; }
    public String getRedirectUrl() { return redirectUrl; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
