package com.yvette.api.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Server-to-server payment results from Safaricom (M-Pesa) and Stripe. */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService payments;
    private final StripeProvider stripe;
    private final JsonMapper json;
    private final String mpesaCallbackSecret;

    public PaymentWebhookController(PaymentService payments, StripeProvider stripe, JsonMapper json,
                                    @Value("${yvette.payments.mpesa.callback-secret:}") String mpesaCallbackSecret) {
        this.payments = payments;
        this.stripe = stripe;
        this.json = json;
        this.mpesaCallbackSecret = mpesaCallbackSecret;
    }

    /**
     * Safaricom doesn't sign callbacks, so the callback URL carries a long random secret, and the
     * result is matched to a request we started (CheckoutRequestID) and checked against the amount.
     */
    @PostMapping("/mpesa/callback/{secret}")
    public ResponseEntity<String> mpesaCallback(@PathVariable String secret, @RequestBody JsonNode body) {
        if (mpesaCallbackSecret.length() < 16 || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8), mpesaCallbackSecret.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.notFound().build();
        }
        payments.handleMpesaCallback(MpesaDarajaProvider.parseCallback(body));
        return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}");
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload,
                                              @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!stripe.verifySignature(payload, signature)) {
            log.warn("Rejected Stripe webhook with an invalid signature");
            return ResponseEntity.badRequest().build();
        }
        JsonNode event = json.readTree(payload);
        JsonNode session = event.path("data").path("object");
        switch (event.path("type").asString("")) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    payments.handleStripeCheckoutCompleted(session.path("id").asString(),
                            session.path("payment_intent").asString(null), session.path("payment_status").asString(""));
            case "checkout.session.expired", "checkout.session.async_payment_failed" ->
                    payments.handleStripeCheckoutExpired(session.path("id").asString());
            default -> { /* not needed */ }
        }
        return ResponseEntity.ok().build();
    }
}
