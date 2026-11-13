package com.yvette.api.review;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.events.EventService;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderLine;
import com.yvette.api.order.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reviews with fit feedback from verified buyers, shown after moderation (FR-RV-01/02). */
@Service
@Transactional
public class ReviewService {

    static final Set<String> FITS = Set.of("RUNS_SMALL", "TRUE_TO_SIZE", "RUNS_LARGE");

    public record ReviewRequest(@NotNull UUID token, @NotNull Long productId, @Min(1) @Max(5) int rating,
                                @Size(max = 120) String title, @Size(max = 2000) String body, String fit) {}

    public record ReviewView(Long id, Long productId, String productTitle, String productSlug, String authorName,
                             int rating, String title, String body, String fit, String sizeBought, String status,
                             boolean verifiedBuyer, Instant createdAt) {}

    public record FitSummary(long runsSmall, long trueToSize, long runsLarge) {}

    public record ProductReviews(BigDecimal average, long count, FitSummary fit, List<ReviewView> reviews) {}

    private final JdbcClient jdbc;
    private final OrderService orders;
    private final EventService analytics;

    public ReviewService(JdbcClient jdbc, OrderService orders, EventService analytics) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.analytics = analytics;
    }

    public ReviewView submit(String orderNumber, ReviewRequest req) {
        Order order = orders.findByNumberAndToken(orderNumber, req.token());
        if (order.getStatus() != Order.Status.DELIVERED) {
            throw new BadRequestException("You can review items once your order has been delivered");
        }
        OrderLine line = order.getLines().stream().filter(l -> req.productId().equals(l.getProductId())).findFirst()
                .orElseThrow(() -> new BadRequestException("That product isn't part of this order"));
        if (req.fit() != null && !req.fit().isBlank() && !FITS.contains(req.fit())) {
            throw new BadRequestException("Choose how it fits");
        }
        String[] name = order.getShippingName().strip().split("\\s+");
        String author = name[0] + (name.length > 1 ? " " + name[name.length - 1].charAt(0) + "." : "");
        Long id;
        try {
            id = jdbc.sql("""
                            INSERT INTO reviews (product_id, customer_id, order_id, author_name, rating, title, body, fit, size_bought)
                            VALUES (:p, :c, :o, :a, :r, :t, :b, :f, :s) RETURNING id""")
                    .param("p", req.productId()).param("c", order.getCustomerId()).param("o", order.getId())
                    .param("a", author).param("r", req.rating()).param("t", blank(req.title())).param("b", blank(req.body()))
                    .param("f", blank(req.fit())).param("s", line.getSize())
                    .query(Long.class).single();
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ConflictException("You've already reviewed this item — thank you!");
        }
        analytics.recordServerEvent("review_submitted", "order:" + order.getNumber(), order.getCustomerId(),
                Map.of("productId", req.productId(), "rating", req.rating(), "fit", req.fit() == null ? "" : req.fit()));
        return get(id);
    }

    /** Products in a delivered order the customer can still review. */
    @Transactional(readOnly = true)
    public List<Long> reviewedProductIds(String orderNumber, UUID token) {
        Order order = orders.findByNumberAndToken(orderNumber, token);
        return jdbc.sql("SELECT product_id FROM reviews WHERE order_id = :o").param("o", order.getId()).query(Long.class).list();
    }

    @Transactional(readOnly = true)
    public ProductReviews forProduct(String slug) {
        Long productId = jdbc.sql("SELECT id FROM products WHERE slug = :s AND status = 'ACTIVE'").param("s", slug)
                .query(Long.class).optional().orElseThrow(() -> new NotFoundException("Product not found"));
        record Stats(BigDecimal avg, long count, long small, long trueSize, long large) {}
        Stats s = jdbc.sql("""
                        SELECT avg(rating), count(*),
                               count(*) FILTER (WHERE fit = 'RUNS_SMALL'), count(*) FILTER (WHERE fit = 'TRUE_TO_SIZE'),
                               count(*) FILTER (WHERE fit = 'RUNS_LARGE')
                        FROM reviews WHERE product_id = :p AND status = 'APPROVED'""")
                .param("p", productId)
                .query((rs, n) -> new Stats(rs.getBigDecimal(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)))
                .single();
        List<ReviewView> latest = query("WHERE r.product_id = :p AND r.status = 'APPROVED' ORDER BY r.created_at DESC LIMIT 20",
                Map.of("p", productId));
        return new ProductReviews(s.avg() == null ? null : s.avg().setScale(1, RoundingMode.HALF_UP), s.count(),
                new FitSummary(s.small(), s.trueSize(), s.large()), latest);
    }

    @Transactional(readOnly = true)
    public List<ReviewView> moderationQueue(String status) {
        return query("WHERE r.status = :s ORDER BY r.created_at ASC LIMIT 200", Map.of("s", status == null ? "PENDING" : status));
    }

    public ReviewView moderate(Long id, boolean approve, Long staffId) {
        int n = jdbc.sql("UPDATE reviews SET status = :s, moderated_at = now(), moderated_by = :staff WHERE id = :id")
                .param("s", approve ? "APPROVED" : "REJECTED").param("staff", staffId).param("id", id).update();
        if (n == 0) throw new NotFoundException("Review not found");
        return get(id);
    }

    private ReviewView get(Long id) {
        return query("WHERE r.id = :id", Map.of("id", id)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Review not found"));
    }

    private List<ReviewView> query(String where, Map<String, Object> params) {
        return jdbc.sql("""
                        SELECT r.id, r.product_id, p.title, p.slug, r.author_name, r.rating, r.title, r.body, r.fit,
                               r.size_bought, r.status, r.order_id IS NOT NULL, r.created_at
                        FROM reviews r JOIN products p ON p.id = r.product_id""" + " " + where)
                .params(params)
                .query((rs, n) -> new ReviewView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                        rs.getString(11), rs.getBoolean(12), rs.getTimestamp(13).toInstant()))
                .list();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
