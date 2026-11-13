package com.yvette.api.order;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.common.PageResponse;
import com.yvette.api.inventory.InventoryService;
import com.yvette.api.notification.OrderEmails.OrderUpdate;
import com.yvette.api.order.AdminOrderDtos.AdminOrderDetail;
import com.yvette.api.order.AdminOrderDtos.EventRow;
import com.yvette.api.order.AdminOrderDtos.OrderRow;
import com.yvette.api.order.AdminOrderDtos.PaymentRow;
import com.yvette.api.order.AdminOrderDtos.RefundRequest;
import com.yvette.api.order.AdminOrderDtos.RestockLine;
import com.yvette.api.payment.PaymentRepository;
import com.yvette.api.payment.PaymentService;
import com.yvette.api.staff.StaffRepository;
import com.yvette.api.staff.StaffUser;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Back-office order management: queue, fulfilment, refunds, cancellations and notes (docs/06 Epic E5). */
@Service
@Transactional
public class AdminOrderService {

    private final OrderRepository orders;
    private final OrderService orderService;
    private final PaymentService payments;
    private final PaymentRepository paymentRows;
    private final InventoryService inventory;
    private final StaffRepository staff;
    private final ApplicationEventPublisher events;
    private final BigDecimal supportRefundLimit;

    public AdminOrderService(OrderRepository orders, OrderService orderService, PaymentService payments,
                             PaymentRepository paymentRows, InventoryService inventory, StaffRepository staff,
                             ApplicationEventPublisher events,
                             @Value("${yvette.refunds.support-limit:5000}") BigDecimal supportRefundLimit) {
        this.orders = orders;
        this.orderService = orderService;
        this.payments = payments;
        this.paymentRows = paymentRows;
        this.inventory = inventory;
        this.staff = staff;
        this.events = events;
        this.supportRefundLimit = supportRefundLimit;
    }

    /** The order queue: "to ship" (paid) shows oldest first so nothing waits too long (FR-AD-05). */
    @Transactional(readOnly = true)
    public PageResponse<OrderRow> list(Order.Status status, String query, Long customerId, int page, int size) {
        Specification<Order> spec = (root, q, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (status != null) where.add(cb.equal(root.get("status"), status));
            if (customerId != null) where.add(cb.equal(root.get("customerId"), customerId));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
                String digits = query.replaceAll("\\D", "");
                List<Predicate> any = new ArrayList<>(List.of(
                        cb.like(cb.lower(root.get("number")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("shippingName")), like)));
                if (digits.length() >= 6) any.add(cb.like(root.get("phone"), "%" + digits.substring(digits.startsWith("0") ? 1 : 0) + "%"));
                where.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        Sort sort = status == Order.Status.PAID ? Sort.by("placedAt").ascending() : Sort.by("placedAt").descending();
        return PageResponse.of(orders.findAll(spec, PageRequest.of(page, size, sort)), OrderRow::of);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        return Map.of(
                "toShip", orders.count((root, q, cb) -> cb.equal(root.get("status"), Order.Status.PAID)),
                "awaitingPayment", orders.count((root, q, cb) -> cb.equal(root.get("status"), Order.Status.PENDING_PAYMENT)),
                "shipped", orders.count((root, q, cb) -> cb.equal(root.get("status"), Order.Status.SHIPPED)));
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail detail(Long id) {
        return toDetail(find(id));
    }

    public AdminOrderDetail ship(Long id, String carrier, String trackingNumber, String trackingUrl, Long staffId) {
        Order o = orderService.lock(id);
        if (o.getStatus() != Order.Status.PAID) throw new BadRequestException("Only paid orders that haven't shipped can be marked shipped");
        if (trackingUrl != null && !trackingUrl.isBlank() && !trackingUrl.matches("^https?://.+")) {
            throw new BadRequestException("The tracking link must start with http:// or https://");
        }
        o.ship(blank(carrier), blank(trackingNumber), blank(trackingUrl));
        String detail = o.getCarrier() == null ? "" : " with " + o.getCarrier()
                + (o.getTrackingNumber() == null ? "" : " (tracking " + o.getTrackingNumber() + ")");
        o.log("SHIPPED", "Shipped" + detail, true, staffId);
        events.publishEvent(new OrderUpdate(o.getId(), "SHIPPED", null));
        return toDetail(o);
    }

    public AdminOrderDetail deliver(Long id, Long staffId) {
        Order o = orderService.lock(id);
        if (o.getStatus() != Order.Status.SHIPPED && !(o.getStatus() == Order.Status.PAID && "PICKUP".equals(o.getShippingMethodCode()))) {
            throw new BadRequestException("Only shipped orders (or collections) can be marked delivered");
        }
        o.deliver();
        o.log("DELIVERED", "PICKUP".equals(o.getShippingMethodCode()) ? "Collected from the boutique" : "Delivered", true, staffId);
        events.publishEvent(new OrderUpdate(o.getId(), "DELIVERED", null));
        return toDetail(o);
    }

    public AdminOrderDetail addNote(Long id, String note, Long staffId) {
        Order o = orderService.lock(id);
        o.log("NOTE", note.strip(), false, staffId);
        return toDetail(o);
    }

    /**
     * Refunds through the original payment provider (FR / E5-4). Optionally puts returned items back into stock.
     * Support staff can refund up to {@code yvette.refunds.support-limit}.
     */
    public AdminOrderDtos.ActionResult refund(Long id, RefundRequest req, Long staffId, boolean isSupport) {
        Order o = orderService.lock(id);
        BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(o.refundable()) > 0) {
            throw new BadRequestException("You can refund at most " + o.refundable().setScale(0, RoundingMode.HALF_UP) + " on this order");
        }
        if (isSupport && amount.compareTo(supportRefundLimit) > 0) {
            throw new BadRequestException("Support can refund up to KSh " + supportRefundLimit.setScale(0, RoundingMode.HALF_UP)
                    + ". Ask a manager for larger refunds.");
        }
        restock(o, req.restock(), staffId, "Refund");
        PaymentService.RefundOutcome outcome = payments.refund(o, amount);
        o.recordRefund(amount);
        String money = "KSh " + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        o.log("REFUNDED", "Refunded " + money + ": " + req.reason().strip(), true, staffId);
        if (outcome.manual()) o.log("ACTION_NEEDED", outcome.message(), false, staffId);
        events.publishEvent(new OrderUpdate(o.getId(), "REFUNDED", money));
        return new AdminOrderDtos.ActionResult(toDetail(o), outcome.manual() ? outcome.message() : "Refund of " + money + " issued.");
    }

    /** Cancels an order. Unpaid: releases stock. Paid but not shipped: full refund and everything back in stock. */
    public AdminOrderDtos.ActionResult cancel(Long id, String reason, Long staffId, boolean isSupport) {
        Order o = find(id);
        if (o.getStatus() == Order.Status.PENDING_PAYMENT) {
            orderService.cancelUnpaid(id, "Cancelled by the shop: " + reason.strip(), staffId);
            events.publishEvent(new OrderUpdate(id, "CANCELLED", reason.strip()));
            return new AdminOrderDtos.ActionResult(detail(id), "Order cancelled and stock released.");
        }
        if (o.getStatus() != Order.Status.PAID) {
            throw new BadRequestException("Shipped or delivered orders can't be cancelled — use a refund or return instead");
        }
        List<RestockLine> all = o.getLines().stream().map(l -> new RestockLine(l.getId(), l.getQuantity())).toList();
        AdminOrderDtos.ActionResult refunded = o.refundable().signum() > 0
                ? refund(id, new RefundRequest(o.refundable(), "Order cancelled: " + reason.strip(), all), staffId, isSupport)
                : null;
        Order locked = orderService.lock(id);
        locked.cancel();
        locked.log("CANCELLED", "Cancelled by the shop: " + reason.strip(), true, staffId);
        events.publishEvent(new OrderUpdate(id, "CANCELLED", reason.strip()));
        return new AdminOrderDtos.ActionResult(toDetail(locked),
                refunded == null ? "Order cancelled." : "Order cancelled. " + refunded.message());
    }

    private void restock(Order o, List<RestockLine> lines, Long staffId, String why) {
        if (lines == null) return;
        Map<Long, OrderLine> byId = o.getLines().stream().collect(Collectors.toMap(OrderLine::getId, Function.identity()));
        for (RestockLine r : lines) {
            OrderLine line = byId.get(r.lineId());
            if (line == null) throw new BadRequestException("Unknown order line " + r.lineId());
            int max = line.getQuantity() - line.getReturnedQuantity();
            if (r.quantity() > max) throw new BadRequestException("Only " + max + " of " + line.getTitle() + " can be restocked");
            if (line.getVariantId() != null) {
                inventory.restock(line.getVariantId(), r.quantity(), o.getId(), why + " " + o.getNumber(), staffId);
            }
            line.recordReturn(r.quantity());
        }
    }

    private Order find(Long id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private AdminOrderDetail toDetail(Order o) {
        Map<Long, String> names = staff.findAllById(o.getEvents().stream().map(OrderEvent::getStaffUserId)
                        .filter(java.util.Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(StaffUser::getId, StaffUser::getFullName));
        List<EventRow> eventRows = o.getEvents().stream()
                .map(e -> new EventRow(e.getType(), e.getMessage(), e.isCustomerVisible(),
                        e.getStaffUserId() == null ? null : names.get(e.getStaffUserId()), e.getCreatedAt()))
                .toList();
        List<PaymentRow> paymentRowsList = paymentRows.findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .map(p -> new PaymentRow(p.getProvider(), p.getMethod().name(), p.getKind().name(), p.getStatus().name(),
                        p.getAmount(), p.getProviderReference(), p.getFailureMessage(), p.getCreatedAt()))
                .toList();
        return new AdminOrderDetail(o.getId(), o.getCustomerId(), OrderDtos.view(o, payments.latest(o.getId())),
                eventRows, paymentRowsList, o.refundable(), o.getDeliveryNotes());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
