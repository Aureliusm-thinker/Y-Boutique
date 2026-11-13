package com.yvette.api.order;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.CurrentStaff;
import com.yvette.api.common.PageResponse;
import com.yvette.api.order.AdminOrderDtos.ActionResult;
import com.yvette.api.order.AdminOrderDtos.AdminOrderDetail;
import com.yvette.api.order.AdminOrderDtos.CancelRequest;
import com.yvette.api.order.AdminOrderDtos.NoteRequest;
import com.yvette.api.order.AdminOrderDtos.OrderRow;
import com.yvette.api.order.AdminOrderDtos.RefundRequest;
import com.yvette.api.order.AdminOrderDtos.ShipRequest;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orders;

    public AdminOrderController(AdminOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    @PreAuthorize(Roles.ORDERS_READ)
    public PageResponse<OrderRow> list(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) Long customerId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "30") int size) {
        Order.Status s = null;
        if (status != null && !status.isBlank()) {
            try {
                s = Order.Status.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown status " + status);
            }
        }
        return orders.list(s, q, customerId, Math.max(0, page), Math.clamp(size, 1, 100));
    }

    @GetMapping("/counts")
    @PreAuthorize(Roles.ANY_STAFF)
    public Map<String, Long> counts() {
        return orders.counts();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ORDERS_READ)
    public AdminOrderDetail get(@PathVariable Long id) {
        return orders.detail(id);
    }

    /** Several orders at once, for printing a batch of packing slips (E5-2). */
    @GetMapping("/batch")
    @PreAuthorize(Roles.ORDERS_FULFIL)
    public List<AdminOrderDetail> batch(@RequestParam String ids) {
        return Arrays.stream(ids.split(",")).map(String::strip).filter(s -> s.matches("\\d+")).limit(50)
                .map(Long::valueOf).map(orders::detail).toList();
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize(Roles.ORDERS_FULFIL)
    public AdminOrderDetail ship(@PathVariable Long id, @Valid @RequestBody ShipRequest req, @AuthenticationPrincipal Jwt jwt) {
        return orders.ship(id, req.carrier(), req.trackingNumber(), req.trackingUrl(), CurrentStaff.id(jwt));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize(Roles.ORDERS_FULFIL)
    public AdminOrderDetail deliver(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return orders.deliver(id, CurrentStaff.id(jwt));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize(Roles.ORDERS_NOTE)
    public AdminOrderDetail note(@PathVariable Long id, @Valid @RequestBody NoteRequest req, @AuthenticationPrincipal Jwt jwt) {
        return orders.addNote(id, req.note(), CurrentStaff.id(jwt));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize(Roles.REFUNDS)
    public ActionResult refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req, @AuthenticationPrincipal Jwt jwt) {
        return orders.refund(id, req, CurrentStaff.id(jwt), isOnlySupport(jwt));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Roles.REFUNDS)
    public ActionResult cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest req, @AuthenticationPrincipal Jwt jwt) {
        return orders.cancel(id, req.reason(), CurrentStaff.id(jwt), isOnlySupport(jwt));
    }

    private static boolean isOnlySupport(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("SUPPORT");
    }
}
