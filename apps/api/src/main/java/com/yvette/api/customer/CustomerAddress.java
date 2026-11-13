package com.yvette.api.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String label;
    private String fullName;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String county;
    private String postalCode;
    private boolean isDefault;
    private Instant createdAt = Instant.now();

    protected CustomerAddress() {}

    public CustomerAddress(Long customerId) {
        this.customerId = customerId;
    }

    public void update(String label, String fullName, String phone, String line1, String line2, String city,
                       String county, String postalCode) {
        this.label = label;
        this.fullName = fullName;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.county = county;
        this.postalCode = postalCode;
    }

    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getLabel() { return label; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getCounty() { return county; }
    public String getPostalCode() { return postalCode; }
    public boolean isDefault() { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
}
