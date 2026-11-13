package com.yvette.api.payment;

import com.yvette.api.order.Order;
import java.math.BigDecimal;

/**
 * A way to take (and refund) money. Real providers (M-Pesa Daraja, Stripe) are used when their keys
 * are configured; the test providers stand in for them locally ({@code yvette.payments.test-mode}).
 */
public interface PaymentProvider {

    /** What the shopper has to do next to complete the payment. */
    enum NextAction {
        /** Fill in the (fake) card form on our payment page — test mode only. */
        ENTER_TEST_CARD,
        /** Approve the M-Pesa prompt on their phone; the page waits for the result. */
        WAIT_FOR_PHONE,
        /** Go to the provider's hosted payment page. */
        REDIRECT,
        NONE
    }

    record StartResult(NextAction action, String providerReference, String redirectUrl) {}

    /** {@code manual} = the money must be sent back by a person (e.g. from the M-Pesa business account). */
    record RefundResult(boolean succeeded, String providerReference, boolean manual, String message) {}

    String name();

    Order.PaymentMethod method();

    boolean enabled();

    /** Real providers win over test ones when both are enabled. */
    default boolean isTest() {
        return false;
    }

    StartResult start(Order order, Payment payment);

    RefundResult refund(Order order, Payment charge, BigDecimal amount);
}
