package com.yvette.api.report;

import com.yvette.api.staff.Roles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner/manager dashboard numbers (FR-AD-11): sales, orders, AOV, top products, low stock and the shopping funnel. */
@RestController
@RequestMapping("/api/v1/admin/reports")
@Transactional(readOnly = true)
public class ReportService {

    public record DayTotal(LocalDate day, BigDecimal sales, long orders) {}

    public record TopProduct(Long productId, String title, long unitsSold, BigDecimal revenue) {}

    /** Distinct visitors at each step (only visitors who accepted analytics are counted). */
    public record Funnel(long viewedProduct, long addedToBag, long startedCheckout, long purchased) {}

    public record Summary(int days, BigDecimal sales, BigDecimal refunds, long orders, BigDecimal averageOrderValue,
                          long itemsSold, long newCustomers, List<DayTotal> daily, List<TopProduct> topProducts,
                          Funnel funnel, long lowStockVariants, long toShip, long openReturns, long pendingReviews,
                          List<ReasonCount> returnReasons) {}

    public record ReasonCount(String reason, long count) {}

    private final JdbcClient jdbc;

    public ReportService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String PAID = "payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')";

    @GetMapping("/summary")
    @PreAuthorize(Roles.REPORTS)
    public Summary summary(@RequestParam(defaultValue = "30") int days) {
        int d = Math.clamp(days, 1, 366);
        Timestamp since = Timestamp.from(Instant.now().minus(Duration.ofDays(d)));

        record Totals(BigDecimal sales, BigDecimal refunds, long orders) {}
        Totals t = jdbc.sql("SELECT coalesce(sum(total), 0), coalesce(sum(refunded_total), 0), count(*) FROM orders WHERE "
                        + PAID + " AND paid_at >= :since")
                .param("since", since)
                .query((rs, n) -> new Totals(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getLong(3))).single();
        long items = jdbc.sql("SELECT coalesce(sum(l.quantity), 0) FROM order_lines l JOIN orders o ON o.id = l.order_id WHERE "
                + "o." + PAID + " AND o.paid_at >= :since").param("since", since).query(Long.class).single();
        BigDecimal net = t.sales().subtract(t.refunds());
        BigDecimal aov = t.orders() == 0 ? BigDecimal.ZERO : t.sales().divide(BigDecimal.valueOf(t.orders()), 0, RoundingMode.HALF_UP);

        List<DayTotal> daily = jdbc.sql("""
                        SELECT g.day::date, coalesce(sum(o.total), 0), count(o.id)
                        FROM generate_series((now() AT TIME ZONE 'Africa/Nairobi')::date - (:d - 1), (now() AT TIME ZONE 'Africa/Nairobi')::date, interval '1 day') AS g(day)
                        LEFT JOIN orders o ON (o.paid_at AT TIME ZONE 'Africa/Nairobi')::date = g.day::date
                             AND o.payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')
                        GROUP BY g.day ORDER BY g.day""")
                .param("d", d)
                .query((rs, n) -> new DayTotal(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2), rs.getLong(3))).list();

        List<TopProduct> top = jdbc.sql("""
                        SELECT l.product_id, max(l.title), sum(l.quantity), sum(l.line_total)
                        FROM order_lines l JOIN orders o ON o.id = l.order_id
                        WHERE o.payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED') AND o.paid_at >= :since
                        GROUP BY l.product_id ORDER BY sum(l.line_total) DESC LIMIT 5""")
                .param("since", since)
                .query((rs, n) -> new TopProduct(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4))).list();

        Funnel funnel = jdbc.sql("""
                        SELECT count(DISTINCT anonymous_id) FILTER (WHERE name = 'product_view'),
                               count(DISTINCT anonymous_id) FILTER (WHERE name = 'add_to_cart'),
                               count(DISTINCT anonymous_id) FILTER (WHERE name = 'begin_checkout')
                        FROM events WHERE occurred_at >= :since""")
                .param("since", since)
                .query((rs, n) -> new Funnel(rs.getLong(1), rs.getLong(2), rs.getLong(3), t.orders())).single();

        long newCustomers = jdbc.sql("SELECT count(*) FROM customers WHERE created_at >= :since").param("since", since)
                .query(Long.class).single();
        long lowStock = jdbc.sql("""
                        SELECT count(*) FROM inventory_levels i JOIN product_variants v ON v.id = i.variant_id
                        JOIN products p ON p.id = v.product_id
                        WHERE p.status <> 'ARCHIVED' AND (i.on_hand - i.reserved) <= i.low_stock_threshold""")
                .query(Long.class).single();
        long toShip = jdbc.sql("SELECT count(*) FROM orders WHERE status = 'PAID'").query(Long.class).single();
        long openReturns = jdbc.sql("SELECT count(*) FROM returns WHERE status IN ('REQUESTED', 'APPROVED', 'RECEIVED')")
                .query(Long.class).single();
        long pendingReviews = jdbc.sql("SELECT count(*) FROM reviews WHERE status = 'PENDING'").query(Long.class).single();
        List<ReasonCount> reasons = jdbc.sql("""
                        SELECT rl.reason, sum(rl.quantity) FROM return_lines rl JOIN returns r ON r.id = rl.return_id
                        WHERE r.created_at >= :since GROUP BY rl.reason ORDER BY 2 DESC""")
                .param("since", since)
                .query((rs, n) -> new ReasonCount(rs.getString(1), rs.getLong(2))).list();

        return new Summary(d, net, t.refunds(), t.orders(), aov, items, newCustomers, daily, top, funnel, lowStock, toShip,
                openReturns, pendingReviews, reasons);
    }
}
