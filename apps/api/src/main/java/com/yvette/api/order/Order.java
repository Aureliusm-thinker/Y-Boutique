package com.yvette.api.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "CustomerOrder") // "Order" is a reserved word in JPQL
@Table(name = "orders")
public class Order {

    public enum Status { PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }

    public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED }

    public enum PaymentMethod { MPESA, CARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;
    private UUID accessToken;
    private UUID cartToken;
    private Long customerId;
    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String shippingName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String county;
    private String postalCode;
    private String country = "KE";
    private String deliveryNotes;
    private String shippingMethodCode;
    private String shippingMethodName;

    private BigDecimal subtotal;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private BigDecimal shippingTotal = BigDecimal.ZERO;
    private BigDecimal total;
    private BigDecimal refundedTotal = BigDecimal.ZERO;
    private String currency;
    private Long promotionId;
    private String discountCode;

    private String carrier;
    private String trackingNumber;
    private String trackingUrl;

    private Instant placedAt = Instant.now();
    private Instant paidAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<OrderEvent> events = new ArrayList<>();

    protected Order() {}

    public record Contact(String email, String phone, String shippingName) {}

    public record Address(String line1, String line2, String city, String county, String postalCode, String notes) {}

    public Order(String number, UUID cartToken, Long customerId, Contact contact, Address address,
                 PaymentMethod paymentMethod, String shippingMethodCode, String shippingMethodName, String currency) {
        this.number = number;
        this.accessToken = UUID.randomUUID();
        this.cartToken = cartToken;
        this.customerId = customerId;
        this.email = contact.email();
        this.phone = contact.phone();
        this.shippingName = contact.shippingName();
        if (address != null) {
            this.addressLine1 = address.line1();
            this.addressLine2 = address.line2();
            this.city = address.city();
            this.county = address.county();
            this.postalCode = address.postalCode();
            this.deliveryNotes = address.notes();
        }
        this.paymentMethod = paymentMethod;
        this.shippingMethodCode = shippingMethodCode;
        this.shippingMethodName = shippingMethodName;
        this.currency = currency;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public OrderLine addLine(OrderLine.Snapshot s, int quantity) {
        OrderLine line = new OrderLine(this, s, quantity);
        lines.add(line);
        return line;
    }

    public void setTotals(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal total,
                          Long promotionId, String discountCode) {
        this.subtotal = subtotal;
        this.discountTotal = discount;
        this.shippingTotal = shipping;
        this.total = total;
        this.promotionId = promotionId;
        this.discountCode = discountCode;
    }

    public void log(String type, String message, boolean customerVisible, Long staffId) {
        events.add(new OrderEvent(this, type, message, customerVisible, staffId));
    }

    // ------------------------------------------------------------------ state changes

    public void markPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = Instant.now();
        if (status == Status.PENDING_PAYMENT) this.status = Status.PAID;
    }

    /** Late payment for an expired order whose items were still in stock. */
    public void reopenAsPaid() {
        markPaid();
        this.status = Status.PAID;
        this.cancelledAt = null;
    }

    public void markPaymentFailed() {
        if (paymentStatus == PaymentStatus.PENDING) this.paymentStatus = PaymentStatus.FAILED;
    }

    /** Payment retried after a failure. */
    public void markPaymentPending() {
        if (paymentStatus == PaymentStatus.FAILED) this.paymentStatus = PaymentStatus.PENDING;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
        this.cancelledAt = Instant.now();
        if (paymentStatus == PaymentStatus.PENDING) this.paymentStatus = PaymentStatus.FAILED;
    }

    public void ship(String carrier, String trackingNumber, String trackingUrl) {
        this.status = Status.SHIPPED;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.trackingUrl = trackingUrl;
        this.shippedAt = Instant.now();
    }

    public void deliver() {
        this.status = Status.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void recordRefund(BigDecimal amount) {
        this.refundedTotal = refundedTotal.add(amount);
        this.paymentStatus = refundedTotal.compareTo(total) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public BigDecimal refundable() {
        return paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.PARTIALLY_REFUNDED
                ? total.subtract(refundedTotal) : BigDecimal.ZERO;
    }

    public void assignCustomer(Long customerId) {
        this.customerId = customerId;
    }

    /** Personal data removed when a customer deletes their account (orders are kept for accounting). */
    public void anonymise() {
        this.email = "deleted-" + id + "@invalid";
        this.phone = "0";
        this.shippingName = "Deleted customer";
        this.addressLine1 = null;
        this.addressLine2 = null;
        this.postalCode = null;
        this.deliveryNotes = null;
        this.customerId = null;
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public UUID getAccessToken() { return accessToken; }
    public UUID getCartToken() { return cartToken; }
    public Long getCustomerId() { return customerId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Status getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getShippingName() { return shippingName; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getCounty() { return county; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public String getDeliveryNotes() { return deliveryNotes; }
    public String getShippingMethodCode() { return shippingMethodCode; }
    public String getShippingMethodName() { return shippingMethodName; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public BigDecimal getShippingTotal() { return shippingTotal; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getRefundedTotal() { return refundedTotal; }
    public String getCurrency() { return currency; }
    public Long getPromotionId() { return promotionId; }
    public String getDiscountCode() { return discountCode; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getTrackingUrl() { return trackingUrl; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public List<OrderLine> getLines() { return lines; }
    public List<OrderEvent> getEvents() { return events; }
}
