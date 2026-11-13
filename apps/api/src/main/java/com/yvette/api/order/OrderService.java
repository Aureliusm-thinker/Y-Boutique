package com.yvette.api.order;

import com.yvette.api.cart.CartRepository;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.inventory.InventoryLevel;
import com.yvette.api.inventory.InventoryService;
import com.yvette.api.promotion.PromotionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order state changes that touch stock: payment confirmed (stock leaves the shelf), unpaid order
 * cancelled or expired (reservation released). Every change locks the order row first.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** Published after commit when an order is paid (confirmation email, analytics). */
    public record OrderPaid(Long orderId) {}

    private final OrderRepository orders;
    private final InventoryService inventory;
    private final PromotionRepository promotions;
    private final CartRepository carts;
    private final ApplicationEventPublisher publisher;
    private final Duration paymentWindow;
    private final com.yvette.api.staff.LoginRateLimiter lookupLimiter;

    public OrderService(OrderRepository orders, InventoryService inventory, PromotionRepository promotions,
                        CartRepository carts, ApplicationEventPublisher publisher,
                        @Value("${yvette.checkout.payment-window:PT30M}") Duration paymentWindow,
                        com.yvette.api.staff.LoginRateLimiter lookupLimiter) {
        this.lookupLimiter = lookupLimiter;
        this.orders = orders;
        this.inventory = inventory;
        this.promotions = promotions;
        this.carts = carts;
        this.publisher = publisher;
        this.paymentWindow = paymentWindow;
    }

    public Order lock(Long orderId) {
        return orders.findForUpdate(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    /** Payment confirmed. Safe to call more than once (provider callbacks can repeat). */
    public void markPaid(Long orderId, String paymentNote) {
        Order order = lock(orderId);
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID
                || order.getPaymentStatus() == Order.PaymentStatus.REFUNDED
                || order.getPaymentStatus() == Order.PaymentStatus.PARTIALLY_REFUNDED) {
            return;
        }

        if (order.getStatus() == Order.Status.CANCELLED) {
            // Paid after the reservation expired: take the stock now if it's all still there.
            if (!takeStockWithoutReservation(order)) {
                order.markPaid();
                order.log("ATTENTION", "Payment arrived after the order expired and some items are sold out. "
                        + "Refund the customer or arrange a replacement.", false, null);
                log.warn("Order {} paid after expiry but stock is gone", order.getNumber());
                return;
            }
            order.reopenAsPaid();
        } else {
            for (OrderLine line : order.getLines()) {
                if (line.getVariantId() != null) {
                    inventory.commitSale(line.getVariantId(), line.getQuantity(), order.getId(), order.getNumber());
                }
            }
            order.markPaid();
        }

        if (order.getPromotionId() != null) {
            promotions.findForUpdate(order.getPromotionId()).ifPresent(p -> p.recordUse());
        }
        if (order.getCartToken() != null) {
            carts.findByToken(order.getCartToken()).ifPresent(c -> c.clear());
        }
        order.log("PAID", "Payment received" + (paymentNote == null ? "" : " (" + paymentNote + ")"), true, null);
        publisher.publishEvent(new OrderPaid(order.getId()));
    }

    public void paymentFailed(Long orderId, String reason) {
        Order order = lock(orderId);
        if (order.getPaymentStatus() != Order.PaymentStatus.PENDING) return;
        order.markPaymentFailed();
        order.log("PAYMENT_FAILED", "Payment didn't go through" + (reason == null ? "" : ": " + reason), false, null);
    }

    /** Cancels an unpaid order and releases its reserved stock. */
    public boolean cancelUnpaid(Long orderId, String reason, Long staffId) {
        Order order = lock(orderId);
        if (order.getStatus() != Order.Status.PENDING_PAYMENT || order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            return false;
        }
        for (OrderLine line : order.getLines()) {
            if (line.getVariantId() != null) inventory.releaseReservation(line.getVariantId(), line.getQuantity());
        }
        order.cancel();
        order.log("CANCELLED", reason, true, staffId);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Long> unpaidPastPaymentWindow() {
        return orders.findUnpaidPlacedBefore(Instant.now().minus(paymentWindow));
    }

    /** Finds an order by number + email; repeated misses from one email are slowed down. */
    @Transactional(readOnly = true)
    public Order lookup(String number, String email) {
        String key = "lookup:" + email.strip().toLowerCase(java.util.Locale.ROOT);
        if (lookupLimiter.isBlocked(key)) throw new com.yvette.api.common.BadRequestException("Too many attempts. Try again in 15 minutes.");
        Order order = orders.findByNumber(number)
                .filter(o -> o.getEmail().equalsIgnoreCase(email.strip()))
                .orElse(null);
        if (order == null) {
            lookupLimiter.recordFailure(key);
            throw new NotFoundException("We couldn't find an order with that number and email");
        }
        return order;
    }

    public Order findByNumberAndToken(String number, UUID token) {
        Order order = orders.findByNumber(number).orElseThrow(() -> new NotFoundException("Order not found"));
        if (token == null || !order.getAccessToken().equals(token)) throw new NotFoundException("Order not found");
        return order;
    }

    private boolean takeStockWithoutReservation(Order order) {
        List<OrderLine> lines = order.getLines().stream().filter(l -> l.getVariantId() != null).toList();
        Map<Long, InventoryLevel> levels = inventory.lockForCheckout(lines.stream().map(OrderLine::getVariantId).toList());
        Map<Long, Integer> needed = new java.util.HashMap<>();
        lines.forEach(l -> needed.merge(l.getVariantId(), l.getQuantity(), Integer::sum));
        boolean allThere = needed.entrySet().stream().allMatch(e ->
                levels.containsKey(e.getKey()) && levels.get(e.getKey()).available() >= e.getValue());
        if (!allThere) return false;
        for (OrderLine l : lines) {
            inventory.sellWithoutReservation(l.getVariantId(), l.getQuantity(), order.getId(), order.getNumber());
        }
        return true;
    }

}
