package com.yvette.api.checkout;

import com.yvette.api.checkout.CheckoutDtos.CheckoutOptions;
import com.yvette.api.checkout.CheckoutDtos.CheckoutRequest;
import com.yvette.api.checkout.CheckoutDtos.CheckoutResult;
import com.yvette.api.common.CurrentCustomer;
import com.yvette.api.order.Order;
import com.yvette.api.payment.PaymentService;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Guest or signed-in checkout (FR-CO-02). A signed-in customer's order is linked to their account. */
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final Checkout checkout;
    private final PaymentService payments;
    private final boolean testMode;

    public CheckoutController(Checkout checkout, PaymentService payments,
                              @Value("${yvette.payments.test-mode:false}") boolean testMode) {
        this.checkout = checkout;
        this.payments = payments;
        this.testMode = testMode;
    }

    @GetMapping("/options")
    public CheckoutOptions options() {
        return new CheckoutOptions(
                Arrays.stream(Order.PaymentMethod.values()).filter(payments::available).map(Enum::name).toList(),
                testMode, CheckoutDtos.COUNTIES);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResult checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
        return checkout.checkout(request, CurrentCustomer.idOrNull(jwt));
    }
}
