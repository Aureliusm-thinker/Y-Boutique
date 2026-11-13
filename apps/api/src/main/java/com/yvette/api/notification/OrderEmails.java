package com.yvette.api.notification;

import com.yvette.api.events.EventService;
import com.yvette.api.notification.Mailer.Block;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderLine;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.order.OrderService.OrderPaid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Customer emails about their order (FR-CO-08) and the matching staff alert. Sent after the change commits. */
@Component
public class OrderEmails {

    private final OrderRepository orders;
    private final Mailer mailer;
    private final EventService events;
    private final String siteUrl;
    private final String staffTo;

    public OrderEmails(OrderRepository orders, Mailer mailer, EventService events,
                       @Value("${yvette.site-url}") String siteUrl,
                       @Value("${yvette.mail.staff-notifications-to:}") String staffTo) {
        this.orders = orders;
        this.mailer = mailer;
        this.events = events;
        this.siteUrl = siteUrl;
        this.staffTo = staffTo;
    }

    /** Published when an order ships or a refund is issued, so the customer hears about it. */
    public record OrderUpdate(Long orderId, String kind, String detail) {}

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // writes the analytics "purchase" event
    public void onPaid(OrderPaid paid) {
        Order o = orders.findById(paid.orderId()).orElse(null);
        if (o == null) return;
        events.recordServerEvent("purchase", "order:" + o.getNumber(), o.getCustomerId(), Map.of(
                "orderNumber", o.getNumber(), "total", o.getTotal(), "currency", o.getCurrency(),
                "productIds", o.getLines().stream().map(OrderLine::getProductId).filter(Objects::nonNull).toList()));

        List<Block> blocks = new ArrayList<>();
        blocks.add(Block.text("Hi " + firstName(o) + ", thank you for shopping with Yvette. We've received your payment "
                + "and are getting your order ready."));
        blocks.add(Block.text(summary(o)));
        blocks.add(Block.text(delivery(o)));
        blocks.add(Block.button("View your order", orderLink(o)));
        mailer.send(o.getEmail(), "Order " + o.getNumber() + " confirmed", blocks);

        if (!staffTo.isBlank()) {
            mailer.send(staffTo, "New order " + o.getNumber() + " — " + money(o.getTotal()), List.of(
                    Block.text(summary(o)), Block.text(delivery(o)),
                    Block.button("Open in the back office", siteUrl + "/admin/orders/" + o.getId())));
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onUpdate(OrderUpdate update) {
        Order o = orders.findById(update.orderId()).orElse(null);
        if (o == null) return;
        switch (update.kind()) {
            case "SHIPPED" -> {
                List<Block> blocks = new ArrayList<>();
                blocks.add(Block.text("Hi " + firstName(o) + ", good news — your order " + o.getNumber() + " is on its way."));
                if (o.getCarrier() != null || o.getTrackingNumber() != null) {
                    blocks.add(Block.text("Courier: " + Objects.toString(o.getCarrier(), "—")
                            + "\nTracking number: " + Objects.toString(o.getTrackingNumber(), "—")));
                }
                if (o.getTrackingUrl() != null) blocks.add(Block.button("Track your parcel", o.getTrackingUrl()));
                blocks.add(Block.button("View your order", orderLink(o)));
                mailer.send(o.getEmail(), "Your order " + o.getNumber() + " has shipped", blocks);
            }
            case "DELIVERED" -> mailer.send(o.getEmail(), "Your order " + o.getNumber() + " was delivered", List.of(
                    Block.text("Hi " + firstName(o) + ", we hope you love your new pieces. You can leave a review or "
                            + "start a return from your order page."),
                    Block.button("View your order", orderLink(o))));
            case "REFUNDED" -> mailer.send(o.getEmail(), "Refund for order " + o.getNumber(), List.of(
                    Block.text("Hi " + firstName(o) + ", we've issued a refund of " + update.detail()
                            + " for order " + o.getNumber() + ". "
                            + (o.getPaymentMethod() == Order.PaymentMethod.MPESA
                            ? "It will arrive on your M-Pesa line." : "Card refunds take 5–10 working days to appear.")),
                    Block.button("View your order", orderLink(o))));
            case "CANCELLED" -> mailer.send(o.getEmail(), "Order " + o.getNumber() + " cancelled", List.of(
                    Block.text("Hi " + firstName(o) + ", your order " + o.getNumber() + " has been cancelled. "
                            + Objects.toString(update.detail(), "")),
                    Block.button("View your order", orderLink(o))));
            default -> { }
        }
    }

    private String orderLink(Order o) {
        return siteUrl + "/order/" + o.getNumber() + "?t=" + o.getAccessToken();
    }

    private static String firstName(Order o) {
        return o.getShippingName().split("\\s+")[0];
    }

    static String money(BigDecimal amount) {
        NumberFormat f = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-KE"));
        return "KSh " + f.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    private static String summary(Order o) {
        StringBuilder sb = new StringBuilder("Order " + o.getNumber() + "\n");
        for (OrderLine l : o.getLines()) {
            sb.append(l.getQuantity()).append(" × ").append(l.getTitle());
            String variant = String.join(" / ", java.util.stream.Stream.of(l.getSize(), l.getColour())
                    .filter(Objects::nonNull).toList());
            if (!variant.isEmpty()) sb.append(" (").append(variant).append(")");
            sb.append(" — ").append(money(l.getLineTotal())).append("\n");
        }
        sb.append("\nSubtotal: ").append(money(o.getSubtotal()));
        if (o.getDiscountTotal().signum() > 0) {
            sb.append("\nDiscount").append(o.getDiscountCode() == null ? "" : " (" + o.getDiscountCode() + ")")
                    .append(": −").append(money(o.getDiscountTotal()));
        }
        sb.append("\nDelivery: ").append(o.getShippingTotal().signum() == 0 ? "Free" : money(o.getShippingTotal()));
        sb.append("\nTotal paid: ").append(money(o.getTotal()));
        return sb.toString();
    }

    private static String delivery(Order o) {
        if (o.getAddressLine1() == null) return "Collection: " + o.getShippingMethodName() + ". We'll let you know when it's ready.";
        return o.getShippingMethodName() + " to:\n" + o.getShippingName() + "\n" + o.getAddressLine1()
                + (o.getAddressLine2() == null ? "" : "\n" + o.getAddressLine2())
                + "\n" + o.getCity() + ", " + o.getCounty();
    }
}
