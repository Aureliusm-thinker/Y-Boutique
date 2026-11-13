package com.yvette.api.returns;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.events.EventService;
import com.yvette.api.inventory.InventoryService;
import com.yvette.api.notification.MailRequest;
import com.yvette.api.notification.Mailer.Block;
import com.yvette.api.order.AdminOrderDtos.RefundRequest;
import com.yvette.api.order.AdminOrderService;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderLine;
import com.yvette.api.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns (docs/06 Epic E6): the customer asks (with a reason per item, including size problems),
 * staff approve or reject, Fulfilment receives and restocks, then the refund goes out.
 */
@Service
@Transactional
public class ReturnService {

    public static final Set<String> REASONS = Set.of("TOO_SMALL", "TOO_LARGE", "NOT_AS_DESCRIBED", "DAMAGED",
            "CHANGED_MIND", "OTHER");

    public record LineRequest(@NotNull Long lineId, @Min(1) int quantity, @NotBlank String reason) {}

    public record ReturnRequest(@NotNull UUID token, @NotNull @Size(min = 1, max = 30) List<@Valid LineRequest> lines,
                                @Size(max = 1000) String comment) {}

    public record ReturnLineView(Long id, Long orderLineId, String title, String size, String colour, String sku,
                                 String imageUrl, int quantity, String reason, boolean restocked, BigDecimal unitPrice) {}

    public record ReturnView(Long id, Long orderId, String orderNumber, String status, String comment,
                             String staffComment, BigDecimal refundAmount, BigDecimal itemsValue, String customerName,
                             String email, Instant createdAt, Instant updatedAt, List<ReturnLineView> lines) {}

    private final JdbcClient jdbc;
    private final OrderService orders;
    private final AdminOrderService adminOrders;
    private final InventoryService inventory;
    private final ApplicationEventPublisher events;
    private final EventService analytics;
    private final Duration window;
    private final String siteUrl;

    public ReturnService(JdbcClient jdbc, OrderService orders, AdminOrderService adminOrders, InventoryService inventory,
                         ApplicationEventPublisher events, EventService analytics,
                         @Value("${yvette.returns.window-days:14}") int windowDays,
                         @Value("${yvette.site-url}") String siteUrl) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.adminOrders = adminOrders;
        this.inventory = inventory;
        this.events = events;
        this.analytics = analytics;
        this.window = Duration.ofDays(windowDays);
        this.siteUrl = siteUrl;
    }

    // ------------------------------------------------------------------ customer

    public ReturnView request(String number, ReturnRequest req) {
        Order order = orders.findByNumberAndToken(number, req.token());
        order = orders.lock(order.getId());
        if (order.getStatus() != Order.Status.DELIVERED || order.getDeliveredAt() == null) {
            throw new BadRequestException("Returns can be requested once your order has been delivered");
        }
        if (Instant.now().isAfter(order.getDeliveredAt().plus(window))) {
            throw new BadRequestException("The " + window.toDays() + "-day return window for this order has ended");
        }
        Map<Long, OrderLine> lines = order.getLines().stream().collect(Collectors.toMap(OrderLine::getId, Function.identity()));
        Map<Long, Integer> alreadyRequested = openRequestedQuantities(order.getId());
        for (LineRequest l : req.lines()) {
            OrderLine line = lines.get(l.lineId());
            if (line == null) throw new BadRequestException("That item isn't part of this order");
            if (!REASONS.contains(l.reason())) throw new BadRequestException("Choose a reason for each item");
            int max = line.getQuantity() - line.getReturnedQuantity() - alreadyRequested.getOrDefault(line.getId(), 0);
            if (l.quantity() > max) {
                throw new BadRequestException(max <= 0 ? line.getTitle() + " has already been returned"
                        : "You can return up to " + max + " of " + line.getTitle());
            }
        }
        Long id = jdbc.sql("INSERT INTO returns (order_id, comment) VALUES (:o, :c) RETURNING id")
                .param("o", order.getId()).param("c", blank(req.comment())).query(Long.class).single();
        for (LineRequest l : req.lines()) {
            jdbc.sql("INSERT INTO return_lines (return_id, order_line_id, quantity, reason) VALUES (:r, :l, :q, :reason)")
                    .param("r", id).param("l", l.lineId()).param("q", l.quantity()).param("reason", l.reason()).update();
        }
        order.log("RETURN_REQUESTED", "Return requested (" + req.lines().size() + " item"
                + (req.lines().size() == 1 ? "" : "s") + ")", true, null);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("orderNumber", order.getNumber());
        props.put("reasons", req.lines().stream().map(LineRequest::reason).toList());
        props.put("productIds", req.lines().stream().map(l -> lines.get(l.lineId()).getProductId()).toList());
        analytics.recordServerEvent("return_requested", "order:" + order.getNumber(), order.getCustomerId(), props);
        email(order, "We've received your return request", "We've received your request to return items from order "
                + order.getNumber() + ". We'll reply within one working day with the next steps.");
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<ReturnView> forOrder(String number, UUID token) {
        Order order = orders.findByNumberAndToken(number, token);
        return jdbc.sql("SELECT id FROM returns WHERE order_id = :o ORDER BY created_at DESC")
                .param("o", order.getId()).query(Long.class).list().stream().map(this::get).toList();
    }

    // ------------------------------------------------------------------ staff

    @Transactional(readOnly = true)
    public List<ReturnView> list(String status) {
        String where = status == null || status.isBlank() ? "" : " WHERE status = :s";
        var q = jdbc.sql("SELECT id FROM returns" + where + " ORDER BY created_at ASC LIMIT 200");
        if (!where.isEmpty()) q = q.param("s", status);
        return q.query(Long.class).list().stream().map(this::get).toList();
    }

    @Transactional(readOnly = true)
    public ReturnView get(Long id) {
        record Head(Long id, Long orderId, String number, String status, String comment, String staffComment,
                    BigDecimal refund, String name, String email, Instant created, Instant updated) {}
        Head h = jdbc.sql("""
                        SELECT r.id, r.order_id, o.number, r.status, r.comment, r.staff_comment, r.refund_amount,
                               o.shipping_name, o.email, r.created_at, r.updated_at
                        FROM returns r JOIN orders o ON o.id = r.order_id WHERE r.id = :id""")
                .param("id", id)
                .query((rs, n) -> new Head(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getBigDecimal(7), rs.getString(8), rs.getString(9),
                        rs.getTimestamp(10).toInstant(), rs.getTimestamp(11).toInstant()))
                .optional().orElseThrow(() -> new NotFoundException("Return not found"));
        List<ReturnLineView> lines = jdbc.sql("""
                        SELECT rl.id, ol.id, ol.title, ol.size, ol.colour, ol.sku, ol.image_url, rl.quantity, rl.reason,
                               rl.restocked, ol.unit_price
                        FROM return_lines rl JOIN order_lines ol ON ol.id = rl.order_line_id
                        WHERE rl.return_id = :id ORDER BY rl.id""")
                .param("id", id)
                .query((rs, n) -> new ReturnLineView(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8), rs.getString(9),
                        rs.getBoolean(10), rs.getBigDecimal(11)))
                .list();
        BigDecimal value = lines.stream().map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReturnView(h.id(), h.orderId(), h.number(), h.status(), h.comment(), h.staffComment(), h.refund(),
                value, h.name(), h.email(), h.created(), h.updated(), lines);
    }

    public ReturnView approve(Long id, String comment, Long staffId) {
        ReturnView r = transition(id, "REQUESTED", "APPROVED", comment);
        Order o = orders.lock(r.orderId());
        o.log("RETURN_APPROVED", "Return approved — please send the items back", true, staffId);
        email(o, "Your return is approved", "Your return for order " + o.getNumber() + " is approved. Please pack the "
                + "items with their tags and drop them at the boutique or hand them to our courier. "
                + (comment == null || comment.isBlank() ? "" : "\n\nNote from our team: " + comment.strip()));
        return get(id);
    }

    public ReturnView reject(Long id, String comment, Long staffId) {
        if (comment == null || comment.isBlank()) throw new BadRequestException("Tell the customer why");
        ReturnView r = transition(id, "REQUESTED", "REJECTED", comment);
        Order o = orders.lock(r.orderId());
        o.log("RETURN_REJECTED", "Return not accepted: " + comment.strip(), true, staffId);
        email(o, "About your return request", "We're sorry, we can't accept the return for order " + o.getNumber()
                + ".\n\n" + comment.strip() + "\n\nReply to this email if you have questions.");
        return get(id);
    }

    /** Items arrived back. Ticked lines go back into stock (Fulfilment may only restock returns — docs/03). */
    public ReturnView receive(Long id, List<Long> restockLineIds, Long staffId) {
        ReturnView r = transition(id, "APPROVED", "RECEIVED", null);
        Order o = orders.lock(r.orderId());
        Map<Long, OrderLine> orderLines = o.getLines().stream().collect(Collectors.toMap(OrderLine::getId, Function.identity()));
        for (ReturnLineView l : r.lines()) {
            OrderLine line = orderLines.get(l.orderLineId());
            line.recordReturn(l.quantity());
            if (restockLineIds != null && restockLineIds.contains(l.id()) && line.getVariantId() != null) {
                inventory.restock(line.getVariantId(), l.quantity(), o.getId(), "Return " + o.getNumber(), staffId);
                jdbc.sql("UPDATE return_lines SET restocked = TRUE WHERE id = :id").param("id", l.id()).update();
            }
        }
        o.log("RETURN_RECEIVED", "We've received your returned items", true, staffId);
        return get(id);
    }

    /** Refunds the returned items' value through the original payment (delivery isn't refunded). */
    public ReturnView refund(Long id, BigDecimal amountOverride, Long staffId, boolean isSupport) {
        ReturnView r = get(id);
        if (!"RECEIVED".equals(r.status())) throw new BadRequestException("Receive the items before refunding");
        BigDecimal amount = amountOverride != null ? amountOverride : r.itemsValue();
        adminOrders.refund(r.orderId(), new RefundRequest(amount, "Return #" + id, List.of()), staffId, isSupport);
        jdbc.sql("UPDATE returns SET status = 'REFUNDED', refund_amount = :a, updated_at = now() WHERE id = :id")
                .param("a", amount.setScale(2, RoundingMode.HALF_UP)).param("id", id).update();
        return get(id);
    }

    // ------------------------------------------------------------------ helpers

    private ReturnView transition(Long id, String from, String to, String comment) {
        int changed = jdbc.sql("""
                        UPDATE returns SET status = :to, staff_comment = coalesce(:c, staff_comment), updated_at = now()
                        WHERE id = :id AND status = :from""")
                .param("to", to).param("c", blank(comment)).param("id", id).param("from", from).update();
        if (changed == 0) {
            ReturnView current = get(id);
            throw new BadRequestException("This return is already " + current.status().toLowerCase()
                    + ", so that step isn't possible now");
        }
        return get(id);
    }

    private Map<Long, Integer> openRequestedQuantities(Long orderId) {
        Map<Long, Integer> out = new java.util.HashMap<>();
        jdbc.sql("""
                        SELECT rl.order_line_id, sum(rl.quantity) FROM return_lines rl JOIN returns r ON r.id = rl.return_id
                        WHERE r.order_id = :o AND r.status IN ('REQUESTED', 'APPROVED') GROUP BY rl.order_line_id""")
                .param("o", orderId)
                .query((rs, n) -> out.put(rs.getLong(1), rs.getInt(2))).list();
        return out;
    }

    private void email(Order o, String subject, String text) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(Block.text("Hi " + o.getShippingName().split("\\s+")[0] + ", " + Character.toLowerCase(text.charAt(0)) + text.substring(1)));
        blocks.add(Block.button("View your order", siteUrl + "/order/" + o.getNumber() + "?t=" + o.getAccessToken()));
        events.publishEvent(new MailRequest(o.getEmail(), subject, blocks));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
