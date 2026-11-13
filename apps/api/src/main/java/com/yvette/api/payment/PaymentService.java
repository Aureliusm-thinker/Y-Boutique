package com.yvette.api.payment;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderDtos.PaymentView;
import com.yvette.api.order.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts payments with the right provider and applies their results to orders. */
@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final List<PaymentProvider> providers;
    private final PaymentRepository payments;
    private final OrderService orders;

    public PaymentService(List<PaymentProvider> providers, PaymentRepository payments, OrderService orders) {
        this.providers = providers;
        this.payments = payments;
        this.orders = orders;
    }

    /** The provider used for a method: a real one if configured, otherwise the test one (if enabled). */
    public Optional<PaymentProvider> providerFor(Order.PaymentMethod method) {
        return providers.stream()
                .filter(p -> p.method() == method && p.enabled())
                .min((a, b) -> Boolean.compare(a.isTest(), b.isTest()));
    }

    public boolean available(Order.PaymentMethod method) {
        return providerFor(method).isPresent();
    }

    /** Starts (or restarts) taking payment for an unpaid order. */
    public PaymentView start(Long orderId, String phone) {
        Order order = orders.lock(orderId);
        if (order.getStatus() != Order.Status.PENDING_PAYMENT || order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("This order doesn't need paying");
        }
        PaymentProvider provider = providerFor(order.getPaymentMethod())
                .orElseThrow(() -> new PaymentException("This payment method isn't available right now"));
        // Any earlier attempt that is still waiting is superseded.
        payments.findFirstByOrderIdAndKindAndStatusOrderByCreatedAtDesc(orderId, Payment.Kind.CHARGE, Payment.Status.PENDING)
                .ifPresent(p -> p.failed("Replaced by a new attempt"));
        order.markPaymentPending();

        Payment payment = payments.save(new Payment(orderId, provider.name(), order.getPaymentMethod(),
                Payment.Kind.CHARGE, order.getTotal(), phone == null ? order.getPhone() : phone));
        PaymentProvider.StartResult started = provider.start(order, payment);
        payment.started(started.providerReference(), started.redirectUrl());
        return view(payment, provider.isTest(), started.action());
    }

    @Transactional(readOnly = true)
    public PaymentView latest(Long orderId) {
        return payments.findFirstByOrderIdAndKindOrderByCreatedAtDesc(orderId, Payment.Kind.CHARGE)
                .map(p -> view(p, isTestProvider(p.getProvider()), nextAction(p)))
                .orElse(null);
    }

    // ------------------------------------------------------------------ results

    public void succeeded(Payment payment, String reference, String note) {
        if (payment.getStatus() == Payment.Status.SUCCEEDED) return;
        payment.succeeded(reference);
        orders.markPaid(payment.getOrderId(), note);
    }

    public void failed(Payment payment, String reason) {
        if (payment.getStatus() != Payment.Status.PENDING) return;
        payment.failed(reason);
        orders.paymentFailed(payment.getOrderId(), reason);
    }

    /** Test card form (test mode only). */
    public void payWithTestCard(Long orderId, String cardNumber) {
        Payment payment = pendingTestPayment(orderId, TestProviders.TEST_CARD);
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        switch (digits) {
            case TestProviders.CARD_SUCCESS -> succeeded(payment, null, "test card");
            case TestProviders.CARD_DECLINED -> failed(payment, "Your card was declined. Try another card or M-Pesa.");
            default -> throw new BadRequestException("Use a test card: 4242 4242 4242 4242 (or 4000 0000 0000 0002 to see a decline)");
        }
    }

    /** Stands in for the shopper approving or cancelling the M-Pesa prompt (test mode only). */
    public void simulateMpesa(Long orderId, boolean approve) {
        Payment payment = pendingTestPayment(orderId, TestProviders.TEST_MPESA);
        if (approve) {
            succeeded(payment, null, "M-Pesa test receipt");
        } else {
            failed(payment, "The M-Pesa request was cancelled on the phone.");
        }
    }

    public void handleMpesaCallback(MpesaDarajaProvider.CallbackResult result) {
        Payment payment = payments.findByProviderAndProviderReference(MpesaDarajaProvider.NAME, result.checkoutRequestId())
                .orElse(null);
        if (payment == null) {
            log.warn("M-Pesa callback for unknown request {}", result.checkoutRequestId());
            return;
        }
        if (!result.success()) {
            failed(payment, "M-Pesa: " + result.description());
            return;
        }
        if (result.amount() != null && result.amount().compareTo(payment.getAmount().setScale(0, java.math.RoundingMode.CEILING)) < 0) {
            failed(payment, "M-Pesa amount " + result.amount() + " is less than the order total");
            return;
        }
        succeeded(payment, result.receipt(), "M-Pesa receipt " + result.receipt());
    }

    public void handleStripeCheckoutCompleted(String sessionId, String paymentIntentId, String paymentStatus) {
        Payment payment = payments.findByProviderAndProviderReference(StripeProvider.NAME, sessionId).orElse(null);
        if (payment == null) return; // already processed (reference is now the payment intent) or not ours
        if ("paid".equals(paymentStatus)) {
            succeeded(payment, paymentIntentId, "card");
        }
    }

    public void handleStripeCheckoutExpired(String sessionId) {
        payments.findByProviderAndProviderReference(StripeProvider.NAME, sessionId)
                .ifPresent(p -> failed(p, "The card payment page was closed or timed out."));
    }

    // ------------------------------------------------------------------ refunds

    public record RefundOutcome(boolean manual, String message) {}

    /** Sends money back through the provider that took it and records the refund. */
    public RefundOutcome refund(Order order, BigDecimal amount) {
        Payment charge = payments.findFirstByOrderIdAndKindAndStatusOrderByCreatedAtDesc(order.getId(),
                        Payment.Kind.CHARGE, Payment.Status.SUCCEEDED)
                .orElseThrow(() -> new BadRequestException("There is no successful payment to refund"));
        PaymentProvider provider = providers.stream().filter(p -> p.name().equals(charge.getProvider())).findFirst()
                .orElseThrow(() -> new BadRequestException("Payment provider " + charge.getProvider() + " is not available"));
        PaymentProvider.RefundResult result = provider.refund(order, charge, amount);
        Payment refund = payments.save(new Payment(order.getId(), provider.name(), charge.getMethod(),
                Payment.Kind.REFUND, amount, charge.getPhone()));
        if (!result.succeeded()) {
            refund.failed(result.message());
            throw new PaymentException(result.message());
        }
        refund.succeeded(result.providerReference());
        return new RefundOutcome(result.manual(), result.message());
    }

    // ------------------------------------------------------------------ helpers

    private Payment pendingTestPayment(Long orderId, String provider) {
        Payment payment = payments.findFirstByOrderIdAndKindOrderByCreatedAtDesc(orderId, Payment.Kind.CHARGE)
                .filter(p -> p.getProvider().equals(provider))
                .orElseThrow(() -> new NotFoundException("No test payment is waiting for this order"));
        if (!isTestProvider(provider) || providers.stream().noneMatch(p -> p.name().equals(provider) && p.enabled())) {
            throw new NotFoundException("Test payments are switched off");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new BadRequestException("This payment attempt is finished. Start a new one.");
        }
        return payment;
    }

    private boolean isTestProvider(String name) {
        return providers.stream().anyMatch(p -> p.name().equals(name) && p.isTest());
    }

    private PaymentProvider.NextAction nextAction(Payment p) {
        if (p.getStatus() != Payment.Status.PENDING) return PaymentProvider.NextAction.NONE;
        if (p.getRedirectUrl() != null) return PaymentProvider.NextAction.REDIRECT;
        if (p.getProvider().equals(TestProviders.TEST_CARD)) return PaymentProvider.NextAction.ENTER_TEST_CARD;
        return PaymentProvider.NextAction.WAIT_FOR_PHONE;
    }

    private static PaymentView view(Payment p, boolean test, PaymentProvider.NextAction action) {
        return new PaymentView(p.getProvider(), p.getMethod().name(), p.getStatus().name(), action.name(),
                p.getRedirectUrl(), p.getPhone(), p.getFailureMessage(), test);
    }
}
