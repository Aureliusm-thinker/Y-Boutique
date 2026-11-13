package com.yvette.api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AdminOrderDtos {

    private AdminOrderDtos() {}

    public record OrderRow(Long id, String number, String status, String paymentStatus, String paymentMethod,
                           String customerName, String email, int itemCount, BigDecimal total, String currency,
                           String shippingMethod, Instant placedAt) {
        static OrderRow of(Order o) {
            return new OrderRow(o.getId(), o.getNumber(), o.getStatus().name(), o.getPaymentStatus().name(),
                    o.getPaymentMethod().name(), o.getShippingName(), o.getEmail(),
                    o.getLines().stream().mapToInt(OrderLine::getQuantity).sum(), o.getTotal(), o.getCurrency(),
                    o.getShippingMethodName(), o.getPlacedAt());
        }
    }

    public record EventRow(String type, String message, boolean customerVisible, String staffName, Instant at) {}

    public record PaymentRow(String provider, String method, String kind, String status, BigDecimal amount,
                             String reference, String failureMessage, Instant at) {}

    public record AdminOrderDetail(Long id, Long customerId, OrderDtos.OrderView order, List<EventRow> events,
                                   List<PaymentRow> payments, BigDecimal refundable, String deliveryNotes) {}

    public record ShipRequest(@Size(max = 60) String carrier, @Size(max = 80) String trackingNumber,
                              @Size(max = 500) String trackingUrl) {}

    public record NoteRequest(@NotBlank @Size(max = 500) String note) {}

    public record RestockLine(@NotNull Long lineId, @Min(1) int quantity) {}

    public record RefundRequest(@NotNull @DecimalMin("1") BigDecimal amount, @NotBlank @Size(max = 300) String reason,
                                @Size(max = 50) List<@Valid RestockLine> restock) {}

    public record CancelRequest(@NotBlank @Size(max = 300) String reason) {}

    public record ActionResult(AdminOrderDetail order, String message) {}
}
