package com.yvette.api.order;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One entry in an order's timeline (placed, paid, shipped, notes…). */
@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private String type;
    private String message;
    private boolean customerVisible;
    private Long staffUserId;
    private Instant createdAt = Instant.now();

    protected OrderEvent() {}

    OrderEvent(Order order, String type, String message, boolean customerVisible, Long staffUserId) {
        this.order = order;
        this.type = type;
        this.message = message.length() > 500 ? message.substring(0, 500) : message;
        this.customerVisible = customerVisible;
        this.staffUserId = staffUserId;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isCustomerVisible() { return customerVisible; }
    public Long getStaffUserId() { return staffUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
