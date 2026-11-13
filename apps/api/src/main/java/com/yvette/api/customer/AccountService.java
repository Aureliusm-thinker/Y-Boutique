package com.yvette.api.customer;

import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.catalog.CatalogService;
import com.yvette.api.checkout.CheckoutDtos;
import com.yvette.api.checkout.KenyanPhone;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.customer.CustomerDtos.AddressDto;
import com.yvette.api.customer.CustomerDtos.AddressRequest;
import com.yvette.api.customer.CustomerDtos.Preferences;
import com.yvette.api.customer.CustomerDtos.Profile;


import com.yvette.api.order.Order;
import com.yvette.api.order.OrderDtos;
import com.yvette.api.order.OrderDtos.OrderSummary;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.payment.PaymentService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** A signed-in customer's own data: profile, addresses, orders, wishlist, export and deletion (FR-CA-03..08). */
@Service
@Transactional
public class AccountService {

    private static final int MAX_ADDRESSES = 10;
    private static final int MAX_WISHLIST = 100;

    private final CustomerRepository customers;
    private final CustomerAddressRepository addresses;
    private final OrderRepository orders;
    private final PaymentService payments;
    private final CatalogService catalog;
    private final PasswordEncoder passwords;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AccountService(CustomerRepository customers, CustomerAddressRepository addresses, OrderRepository orders, PaymentService payments,
                          CatalogService catalog, PasswordEncoder passwords, JdbcClient jdbc, JsonMapper json) {
        this.customers = customers;
        this.addresses = addresses;
        this.orders = orders;
        this.payments = payments;
        this.catalog = catalog;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Profile profile(Long customerId) {
        return profile(find(customerId));
    }

    Profile profile(Customer c) {
        return new Profile(c.getId(), c.getEmail(), c.getFullName(), c.getPhone(), c.isEmailVerified(),
                c.isMarketingOptIn(), preferences(c),
                c.getId() == null ? List.of()
                        : addresses.findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(c.getId()).stream().map(AddressDto::of).toList(),
                c.getCreatedAt());
    }

    public Profile updateProfile(Long customerId, String fullName, String phone, boolean marketingOptIn) {
        Customer c = find(customerId);
        c.updateProfile(fullName, normalisePhone(phone), marketingOptIn);
        return profile(c);
    }

    public Profile updatePreferences(Long customerId, Preferences prefs) {
        Customer c = find(customerId);
        Preferences clean = new Preferences(clean(prefs.sizes()), clean(prefs.colours()), clean(prefs.styles()));
        c.setPreferences(json.writeValueAsString(clean));
        return profile(c);
    }

    public void changePassword(Long customerId, String current, String next) {
        Customer c = find(customerId);
        if (!passwords.matches(current, c.getPasswordHash())) throw new BadRequestException("Your current password is wrong");
        c.changePasswordHash(passwords.encode(next));
    }

    // ------------------------------------------------------------------ addresses

    public Profile saveAddress(Long customerId, Long addressId, AddressRequest req) {
        if (!CheckoutDtos.COUNTIES.contains(req.county())) throw new BadRequestException("Choose a county");
        CustomerAddress a;
        if (addressId == null) {
            if (addresses.countByCustomerId(customerId) >= MAX_ADDRESSES) {
                throw new BadRequestException("You can save up to " + MAX_ADDRESSES + " addresses");
            }
            a = new CustomerAddress(customerId);
        } else {
            a = addresses.findByIdAndCustomerId(addressId, customerId)
                    .orElseThrow(() -> new NotFoundException("Address not found"));
        }
        a.update(blankToNull(req.label()), req.fullName().strip(), normalisePhone(req.phone()), req.line1().strip(),
                blankToNull(req.line2()), req.city().strip(), req.county(), blankToNull(req.postalCode()));
        a = addresses.save(a);
        List<CustomerAddress> all = addresses.findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(customerId);
        if (req.makeDefault() || all.stream().noneMatch(CustomerAddress::isDefault)) {
            Long keep = a.getId();
            all.forEach(x -> x.setDefault(x.getId().equals(keep)));
        }
        return profile(customerId);
    }

    public Profile deleteAddress(Long customerId, Long addressId) {
        CustomerAddress a = addresses.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addresses.delete(a);
        addresses.flush();
        if (a.isDefault()) {
            addresses.findByCustomerIdOrderByIsDefaultDescCreatedAtAsc(customerId).stream().findFirst()
                    .ifPresent(x -> x.setDefault(true));
        }
        return profile(customerId);
    }

    // ------------------------------------------------------------------ orders

    @Transactional(readOnly = true)
    public List<OrderSummary> orders(Long customerId) {
        return orders.findByCustomerIdOrderByPlacedAtDesc(customerId).stream().map(OrderSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderView order(Long customerId, String number) {
        Order o = orders.findByNumber(number).filter(x -> customerId.equals(x.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return OrderDtos.view(o, payments.latest(o.getId()));
    }

    // ------------------------------------------------------------------ wishlist

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> wishlist(Long customerId) {
        List<Long> ids = jdbc.sql("SELECT product_id FROM wishlist_items WHERE customer_id = :c ORDER BY added_at DESC")
                .param("c", customerId).query(Long.class).list();
        return catalog.summaries(ids);
    }

    @Transactional(readOnly = true)
    public List<Long> wishlistIds(Long customerId) {
        return jdbc.sql("SELECT product_id FROM wishlist_items WHERE customer_id = :c")
                .param("c", customerId).query(Long.class).list();
    }

    public void addToWishlist(Long customerId, Long productId) {
        long count = jdbc.sql("SELECT count(*) FROM wishlist_items WHERE customer_id = :c").param("c", customerId)
                .query(Long.class).single();
        if (count >= MAX_WISHLIST) throw new BadRequestException("Your wishlist is full");
        int exists = jdbc.sql("SELECT count(*) FROM products WHERE id = :p").param("p", productId).query(Integer.class).single();
        if (exists == 0) throw new NotFoundException("Product not found");
        jdbc.sql("INSERT INTO wishlist_items (customer_id, product_id) VALUES (:c, :p) ON CONFLICT DO NOTHING")
                .param("c", customerId).param("p", productId).update();
    }

    public void removeFromWishlist(Long customerId, Long productId) {
        jdbc.sql("DELETE FROM wishlist_items WHERE customer_id = :c AND product_id = :p")
                .param("c", customerId).param("p", productId).update();
    }

    // ------------------------------------------------------------------ privacy (FR-CA-08)

    /** Everything we hold about the customer, as a downloadable document. */
    @Transactional(readOnly = true)
    public Map<String, Object> export(Long customerId) {
        Customer c = find(customerId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now());
        out.put("profile", profile(c));
        out.put("orders", orders.findByCustomerIdOrderByPlacedAtDesc(customerId).stream()
                .map(o -> OrderDtos.view(o, null)).toList());
        out.put("wishlist", wishlist(customerId));
        out.put("stockAlerts", jdbc.sql("SELECT variant_id, created_at, notified_at FROM stock_alerts WHERE customer_id = :c")
                .param("c", customerId).query().listOfRows());
        out.put("reviews", jdbc.sql("SELECT product_id, rating, title, body, fit, status, created_at FROM reviews WHERE customer_id = :c")
                .param("c", customerId).query().listOfRows());
        out.put("browsingEvents", jdbc.sql("SELECT name, properties, occurred_at FROM events WHERE user_id = :c ORDER BY occurred_at")
                .param("c", customerId).query().listOfRows());
        return out;
    }

    /**
     * Deletes the account. Orders are kept for tax/accounting but stripped of personal details;
     * browsing history is unlinked; everything else is deleted.
     */
    public void deleteAccount(Long customerId, String password) {
        Customer c = find(customerId);
        if (!passwords.matches(password, c.getPasswordHash())) throw new BadRequestException("Your password is wrong");
        for (Order o : orders.findByCustomerId(customerId)) o.anonymise();
        jdbc.sql("UPDATE events SET user_id = NULL WHERE user_id = :c").param("c", customerId).update();
        jdbc.sql("DELETE FROM stock_alerts WHERE customer_id = :c OR lower(email) = :e")
                .param("c", customerId).param("e", c.getEmail()).update();
        jdbc.sql("UPDATE reviews SET customer_id = NULL, author_name = 'Former customer' WHERE customer_id = :c")
                .param("c", customerId).update();
        customers.delete(c); // addresses, wishlist, tokens, carts and notes cascade
    }

    // ------------------------------------------------------------------ helpers

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private Preferences preferences(Customer c) {
        try {
            Preferences p = json.readValue(c.getPreferences() == null ? "{}" : c.getPreferences(), Preferences.class);
            return new Preferences(p.sizes() == null ? List.of() : p.sizes(), p.colours() == null ? List.of() : p.colours(),
                    p.styles() == null ? List.of() : p.styles());
        } catch (RuntimeException e) {
            return Preferences.empty();
        }
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of() : values.stream().map(String::strip).filter(s -> !s.isEmpty()).distinct().limit(20).toList();
    }

    private static String normalisePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String p = KenyanPhone.normalise(phone);
        if (p == null) throw new BadRequestException("Enter a Kenyan mobile number, e.g. 0712 345 678");
        return p;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
