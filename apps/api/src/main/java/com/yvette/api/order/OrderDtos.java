package com.yvette.api.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Customer-facing order representations (confirmation page, account order history). */
public final class OrderDtos {

    private OrderDtos() {}

    public record LineView(Long id, Long productId, String sku, String title, String size, String colour,
                           String imageUrl, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                           int returnedQuantity) {
        static LineView of(OrderLine l) {
            return new LineView(l.getId(), l.getProductId(), l.getSku(), l.getTitle(), l.getSize(), l.getColour(),
                    l.getImageUrl(), l.getUnitPrice(), l.getQuantity(), l.getLineTotal(), l.getReturnedQuantity());
        }
    }

    public record TimelineEntry(String type, String message, Instant at) {}

    public record PaymentView(String provider, String method, String status, String nextAction, String redirectUrl,
                              String phone, String failureMessage, boolean testMode) {}

    public record AddressView(String name, String line1, String line2, String city, String county,
                              String postalCode, String notes) {}

    public record OrderView(
            String number, String accessToken, String status, String paymentStatus, String paymentMethod,
            String email, String phone, AddressView shipTo, String shippingMethod,
            List<LineView> lines, BigDecimal subtotal, BigDecimal discountTotal, String discountCode,
            BigDecimal shippingTotal, BigDecimal total, BigDecimal refundedTotal, String currency,
            String carrier, String trackingNumber, String trackingUrl,
            Instant placedAt, Instant paidAt, Instant shippedAt, Instant deliveredAt, Instant cancelledAt,
            List<TimelineEntry> timeline, PaymentView payment) {}

    public static OrderView view(Order o, PaymentView payment) {
        return new OrderView(o.getNumber(), o.getAccessToken().toString(), o.getStatus().name(),
                o.getPaymentStatus().name(), o.getPaymentMethod().name(), o.getEmail(), o.getPhone(),
                new AddressView(o.getShippingName(), o.getAddressLine1(), o.getAddressLine2(), o.getCity(),
                        o.getCounty(), o.getPostalCode(), o.getDeliveryNotes()),
                o.getShippingMethodName(),
                o.getLines().stream().map(LineView::of).toList(),
                o.getSubtotal(), o.getDiscountTotal(), o.getDiscountCode(), o.getShippingTotal(), o.getTotal(),
                o.getRefundedTotal(), o.getCurrency(), o.getCarrier(), o.getTrackingNumber(), o.getTrackingUrl(),
                o.getPlacedAt(), o.getPaidAt(), o.getShippedAt(), o.getDeliveredAt(), o.getCancelledAt(),
                o.getEvents().stream().filter(OrderEvent::isCustomerVisible)
                        .map(e -> new TimelineEntry(e.getType(), e.getMessage(), e.getCreatedAt())).toList(),
                payment);
    }

    /** Short row for "My orders". */
    public record OrderSummary(String number, String accessToken, String status, String paymentStatus,
                               BigDecimal total, String currency, int itemCount, Instant placedAt,
                               String firstImageUrl) {
        public static OrderSummary of(Order o) {
            return new OrderSummary(o.getNumber(), o.getAccessToken().toString(), o.getStatus().name(),
                    o.getPaymentStatus().name(), o.getTotal(), o.getCurrency(),
                    o.getLines().stream().mapToInt(OrderLine::getQuantity).sum(), o.getPlacedAt(),
                    o.getLines().isEmpty() ? null : o.getLines().getFirst().getImageUrl());
        }
    }
}
