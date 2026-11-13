package com.yvette.api.payment;

import com.yvette.api.order.Order;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * Card payments through Stripe Checkout (Stripe's hosted, PCI-compliant payment page with 3-D Secure).
 * The shopper is redirected to Stripe; Stripe calls {@code /api/v1/payments/stripe/webhook} when paid.
 * Enabled when {@code yvette.payments.stripe.secret-key} and {@code webhook-secret} are set.
 */
@Component
public class StripeProvider implements PaymentProvider {

    public static final String NAME = "STRIPE";
    private static final Logger log = LoggerFactory.getLogger(StripeProvider.class);
    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;

    private final String secretKey;
    private final String webhookSecret;
    private final String siteUrl;
    private final RestClient http;

    public StripeProvider(@Value("${yvette.payments.stripe.secret-key:}") String secretKey,
                          @Value("${yvette.payments.stripe.webhook-secret:}") String webhookSecret,
                          @Value("${yvette.site-url:http://localhost:3000}") String siteUrl) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.siteUrl = siteUrl;
        this.http = RestClient.builder().baseUrl("https://api.stripe.com").build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Order.PaymentMethod method() {
        return Order.PaymentMethod.CARD;
    }

    @Override
    public boolean enabled() {
        return !secretKey.isBlank() && !webhookSecret.isBlank();
    }

    @Override
    public StartResult start(Order order, Payment payment) {
        String orderPage = siteUrl + "/order/" + order.getNumber() + "?t=" + order.getAccessToken();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("client_reference_id", order.getNumber());
        form.add("customer_email", order.getEmail());
        form.add("success_url", orderPage);
        form.add("cancel_url", siteUrl + "/checkout/pay/" + order.getNumber() + "?t=" + order.getAccessToken());
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", order.getCurrency().toLowerCase(Locale.ROOT));
        form.add("line_items[0][price_data][unit_amount]", toMinorUnits(payment.getAmount()));
        form.add("line_items[0][price_data][product_data][name]", "Yvette order " + order.getNumber());
        form.add("metadata[order]", order.getNumber());
        form.add("payment_intent_data[metadata][order]", order.getNumber());
        JsonNode session = post("/v1/checkout/sessions", form);
        return new StartResult(NextAction.REDIRECT, session.path("id").asString(), session.path("url").asString());
    }

    @Override
    public RefundResult refund(Order order, Payment charge, BigDecimal amount) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("payment_intent", charge.getProviderReference());
        form.add("amount", toMinorUnits(amount));
        JsonNode refund = post("/v1/refunds", form);
        boolean ok = !"failed".equals(refund.path("status").asString(""));
        return new RefundResult(ok, refund.path("id").asString(), false, ok ? "Refunded to card" : "Stripe refund failed");
    }

    /** Verifies the Stripe-Signature header (HMAC-SHA256 of "timestamp.payload") and its age. */
    public boolean verifySignature(String payload, String header) {
        if (header == null || webhookSecret.isBlank()) return false;
        long timestamp = -1;
        String expected = null;
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) timestamp = Long.parseLong(kv[1]);
            if (kv[0].equals("v1") && expected == null) expected = kv[1];
        }
        if (timestamp < 0 || expected == null) return false;
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > SIGNATURE_TOLERANCE_SECONDS) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String actual = HexFormat.of().formatHex(
                    mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private JsonNode post(String path, MultiValueMap<String, String> form) {
        try {
            JsonNode res = http.post().uri(path)
                    .header("Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null) throw new PaymentException("Card payments are unavailable right now.");
            return res;
        } catch (RestClientResponseException e) {
            log.warn("Stripe {} failed: {} {}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("Card payments are unavailable right now. Please try M-Pesa or try again later.");
        }
    }

    private static String toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
