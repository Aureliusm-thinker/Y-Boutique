package com.yvette.api.customer;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.common.PageResponse;
import com.yvette.api.customer.CustomerDtos.Profile;

import com.yvette.api.order.AdminOrderDtos.NoteRequest;
import com.yvette.api.order.OrderDtos.OrderSummary;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer lookup for support (FR-AD-08): search by email, name, phone or order number; add notes. */
@RestController
@RequestMapping("/api/v1/admin/customers")
@Transactional
public class AdminCustomerController {

    public record CustomerRow(Long id, String email, String fullName, String phone, boolean emailVerified,
                              long orderCount, BigDecimal totalSpent, Instant createdAt) {}

    public record NoteRow(String note, String staffName, Instant at) {}

    public record CustomerDetail(Profile profile, List<OrderSummary> orders, List<NoteRow> notes,
                                 BigDecimal totalSpent, Instant lastLoginAt) {}

    private final CustomerRepository customers;
    private final AccountService accounts;
    private final OrderRepository orders;
    private final JdbcClient jdbc;

    public AdminCustomerController(CustomerRepository customers, AccountService accounts, OrderRepository orders, JdbcClient jdbc) {
        this.customers = customers;
        this.accounts = accounts;
        this.orders = orders;
        this.jdbc = jdbc;
    }

    @GetMapping
    @PreAuthorize(Roles.CUSTOMERS_READ)
    @Transactional(readOnly = true)
    public PageResponse<CustomerRow> search(@RequestParam(defaultValue = "") String q,
                                            @RequestParam(defaultValue = "0") int page) {
        String query = q.strip();
        // An order number finds that order's customer.
        if (query.toUpperCase(Locale.ROOT).matches("YV-\\d+")) {
            List<CustomerRow> rows = orders.findByNumber(query.toUpperCase(Locale.ROOT))
                    .filter(o -> o.getCustomerId() != null)
                    .flatMap(o -> customers.findById(o.getCustomerId()))
                    .map(this::row).stream().toList();
            return new PageResponse<>(rows, 0, rows.size(), rows.size(), 1);
        }
        PageRequest pageable = PageRequest.of(Math.max(0, page), 30);
        String digits = query.replaceAll("\\D", "");
        String like = digits.length() >= 6 && digits.length() == query.replaceAll("[\\s+\\-]", "").length()
                ? "%" + (digits.startsWith("0") ? digits.substring(1) : digits) + "%"
                : "%" + query.toLowerCase(Locale.ROOT) + "%";
        Page<Customer> result = query.isEmpty() ? customers.findAllByOrderByCreatedAtDesc(pageable)
                : customers.search(like, pageable);
        return PageResponse.of(result, this::row);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.CUSTOMERS_READ)
    @Transactional(readOnly = true)
    public CustomerDetail get(@PathVariable Long id) {
        Customer c = customers.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
        List<NoteRow> notes = jdbc.sql("""
                        SELECT n.note, s.full_name, n.created_at FROM customer_notes n
                        LEFT JOIN staff_users s ON s.id = n.staff_user_id
                        WHERE n.customer_id = :c ORDER BY n.created_at DESC""")
                .param("c", id)
                .query((rs, n) -> new NoteRow(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant()))
                .list();
        return new CustomerDetail(accounts.profile(id), accounts.orders(id), notes, spent(id), c.getLastLoginAt());
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize(Roles.CUSTOMERS_READ)
    public CustomerDetail addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest req, @AuthenticationPrincipal Jwt jwt) {
        if (!customers.existsById(id)) throw new NotFoundException("Customer not found");
        jdbc.sql("INSERT INTO customer_notes (customer_id, staff_user_id, note) VALUES (:c, :s, :n)")
                .param("c", id).param("s", CurrentStaff.id(jwt)).param("n", req.note().strip()).update();
        return get(id);
    }

    private CustomerRow row(Customer c) {
        long count = jdbc.sql("SELECT count(*) FROM orders WHERE customer_id = :c AND payment_status <> 'PENDING' AND payment_status <> 'FAILED'")
                .param("c", c.getId()).query(Long.class).single();
        return new CustomerRow(c.getId(), c.getEmail(), c.getFullName(), c.getPhone(), c.isEmailVerified(), count,
                spent(c.getId()), c.getCreatedAt());
    }

    private BigDecimal spent(Long customerId) {
        return jdbc.sql("""
                        SELECT coalesce(sum(total - refunded_total), 0) FROM orders
                        WHERE customer_id = :c AND payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')""")
                .param("c", customerId).query(BigDecimal.class).single();
    }
}
