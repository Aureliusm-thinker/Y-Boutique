package com.yvette.api.checkout;

import com.yvette.api.checkout.CheckoutDtos.CheckoutRequest;
import com.yvette.api.checkout.CheckoutDtos.CheckoutResult;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderDtos.PaymentView;
import com.yvette.api.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Places the order (its own transaction), then asks the payment provider to start (outside it). */
@Component
public class Checkout {

    private static final Logger log = LoggerFactory.getLogger(Checkout.class);

    private final CheckoutService checkout;
    private final PaymentService payments;

    public Checkout(CheckoutService checkout, PaymentService payments) {
        this.checkout = checkout;
        this.payments = payments;
    }

    public CheckoutResult checkout(CheckoutRequest req, Long customerId) {
        Order order = checkout.placeOrder(req, customerId);
        String mpesaPhone = null;
        if (req.paymentMethod() == Order.PaymentMethod.MPESA) {
            mpesaPhone = req.mpesaPhone() == null || req.mpesaPhone().isBlank()
                    ? order.getPhone() : CheckoutService.phone(req.mpesaPhone());
        }
        PaymentView payment = null;
        try {
            payment = payments.start(order.getId(), mpesaPhone);
        } catch (RuntimeException e) {
            // The order stays open (stock held for the payment window): the shopper can retry on the payment page.
            log.warn("Could not start payment for {}: {}", order.getNumber(), e.getMessage());
        }
        return new CheckoutResult(order.getNumber(), order.getAccessToken().toString(), payment);
    }
}
