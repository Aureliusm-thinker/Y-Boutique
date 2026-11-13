package com.yvette.api.returns;

import com.yvette.api.common.CurrentStaff;
import com.yvette.api.returns.ReturnService.ReturnRequest;
import com.yvette.api.returns.ReturnService.ReturnView;
import com.yvette.api.staff.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

public final class ReturnControllers {

    private ReturnControllers() {}

    /** Customer side, opened with the order's access token (works for guests and signed-in customers). */
    @RestController
    @RequestMapping("/api/v1/orders/{number}/returns")
    public static class Customer {

        private final ReturnService returns;

        public Customer(ReturnService returns) {
            this.returns = returns;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public ReturnView request(@PathVariable String number, @Valid @RequestBody ReturnRequest req) {
            return returns.request(number, req);
        }

        @GetMapping
        public List<ReturnView> list(@PathVariable String number, @RequestParam UUID token) {
            return returns.forOrder(number, token);
        }
    }

    public record CommentRequest(@Size(max = 500) String comment) {}

    public record ReceiveRequest(List<Long> restockLineIds) {}

    public record RefundRequest(@DecimalMin("1") BigDecimal amount) {}

    @RestController
    @RequestMapping("/api/v1/admin/returns")
    public static class Admin {

        private final ReturnService returns;

        public Admin(ReturnService returns) {
            this.returns = returns;
        }

        @GetMapping
        @PreAuthorize(Roles.RETURNS_RECEIVE)
        public List<ReturnView> list(@RequestParam(required = false) String status) {
            return returns.list(status);
        }

        @GetMapping("/{id}")
        @PreAuthorize(Roles.RETURNS_RECEIVE)
        public ReturnView get(@PathVariable Long id) {
            return returns.get(id);
        }

        @PostMapping("/{id}/approve")
        @PreAuthorize(Roles.RETURNS_APPROVE)
        public ReturnView approve(@PathVariable Long id, @Valid @RequestBody CommentRequest req, @AuthenticationPrincipal Jwt jwt) {
            return returns.approve(id, req.comment(), CurrentStaff.id(jwt));
        }

        @PostMapping("/{id}/reject")
        @PreAuthorize(Roles.RETURNS_APPROVE)
        public ReturnView reject(@PathVariable Long id, @Valid @RequestBody CommentRequest req, @AuthenticationPrincipal Jwt jwt) {
            return returns.reject(id, req.comment(), CurrentStaff.id(jwt));
        }

        @PostMapping("/{id}/receive")
        @PreAuthorize(Roles.RETURNS_RECEIVE)
        public ReturnView receive(@PathVariable Long id, @RequestBody ReceiveRequest req, @AuthenticationPrincipal Jwt jwt) {
            return returns.receive(id, req.restockLineIds(), CurrentStaff.id(jwt));
        }

        @PostMapping("/{id}/refund")
        @PreAuthorize(Roles.REFUNDS)
        public ReturnView refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req, @AuthenticationPrincipal Jwt jwt) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            return returns.refund(id, req.amount(), CurrentStaff.id(jwt), roles != null && roles.contains("SUPPORT"));
        }
    }
}
