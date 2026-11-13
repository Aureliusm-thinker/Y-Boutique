package com.yvette.api.payment;

import com.yvette.api.order.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/**
 * M-Pesa "Lipa na M-Pesa Online" (STK push) through Safaricom's Daraja API.
 * The shopper gets a prompt on their phone and enters their M-Pesa PIN; Safaricom then calls
 * {@code /api/v1/payments/mpesa/callback/{secret}} with the result.
 *
 * <p>Enabled when all {@code yvette.payments.mpesa.*} settings are present (see README → Payments).
 * Refunds are not automated (Daraja's reversal API needs extra approval from Safaricom): staff send the
 * money back from the M-Pesa business account and the refund is recorded as manual.
 */
@Component
public class MpesaDarajaProvider implements PaymentProvider {

    public static final String NAME = "MPESA";
    private static final Logger log = LoggerFactory.getLogger(MpesaDarajaProvider.class);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Africa/Nairobi"));

    private final String consumerKey;
    private final String consumerSecret;
    private final String shortcode;
    private final String passkey;
    private final String callbackUrl;
    private final String transactionType;
    private final RestClient http;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public MpesaDarajaProvider(
            @Value("${yvette.payments.mpesa.consumer-key:}") String consumerKey,
            @Value("${yvette.payments.mpesa.consumer-secret:}") String consumerSecret,
            @Value("${yvette.payments.mpesa.shortcode:}") String shortcode,
            @Value("${yvette.payments.mpesa.passkey:}") String passkey,
            @Value("${yvette.payments.mpesa.callback-url:}") String callbackUrl,
            /** CustomerPayBillOnline (paybill) or CustomerBuyGoodsOnline (till number). */
            @Value("${yvette.payments.mpesa.transaction-type:CustomerPayBillOnline}") String transactionType,
            @Value("${yvette.payments.mpesa.base-url:https://sandbox.safaricom.co.ke}") String baseUrl) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.shortcode = shortcode;
        this.passkey = passkey;
        this.callbackUrl = callbackUrl;
        this.transactionType = transactionType;
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Order.PaymentMethod method() {
        return Order.PaymentMethod.MPESA;
    }

    @Override
    public boolean enabled() {
        return !consumerKey.isBlank() && !consumerSecret.isBlank() && !shortcode.isBlank() && !passkey.isBlank()
                && !callbackUrl.isBlank();
    }

    @Override
    public StartResult start(Order order, Payment payment) {
        String timestamp = TIMESTAMP.format(Instant.now());
        String password = Base64.getEncoder().encodeToString(
                (shortcode + passkey + timestamp).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("BusinessShortCode", shortcode);
        body.put("Password", password);
        body.put("Timestamp", timestamp);
        body.put("TransactionType", transactionType);
        // M-Pesa only takes whole shillings.
        body.put("Amount", payment.getAmount().setScale(0, RoundingMode.CEILING).intValueExact());
        body.put("PartyA", payment.getPhone());
        body.put("PartyB", shortcode);
        body.put("PhoneNumber", payment.getPhone());
        body.put("CallBackURL", callbackUrl);
        body.put("AccountReference", order.getNumber());
        body.put("TransactionDesc", "Yvette order " + order.getNumber());
        try {
            JsonNode res = http.post().uri("/mpesa/stkpush/v1/processrequest")
                    .header("Authorization", "Bearer " + accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null || !"0".equals(res.path("ResponseCode").asString(""))) {
                throw new PaymentException("M-Pesa didn't accept the request. Check the phone number and try again.");
            }
            return new StartResult(NextAction.WAIT_FOR_PHONE, res.path("CheckoutRequestID").asString(), null);
        } catch (RestClientResponseException e) {
            log.warn("M-Pesa STK push failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("We couldn't reach M-Pesa. Please try again in a moment.");
        }
    }

    @Override
    public RefundResult refund(Order order, Payment charge, BigDecimal amount) {
        return new RefundResult(true, null, true,
                "Send KSh " + amount.setScale(0, RoundingMode.HALF_UP) + " back to " + charge.getPhone()
                        + " from the M-Pesa business account (receipt " + charge.getProviderReference() + ").");
    }

    /** Parsed result of Safaricom's STK callback. */
    public record CallbackResult(String checkoutRequestId, boolean success, String receipt, BigDecimal amount,
                                 String description) {}

    public static CallbackResult parseCallback(JsonNode body) {
        JsonNode cb = body.path("Body").path("stkCallback");
        String id = cb.path("CheckoutRequestID").asString(null);
        int code = cb.path("ResultCode").asInt(-1);
        String receipt = null;
        BigDecimal amount = null;
        for (JsonNode item : cb.path("CallbackMetadata").path("Item")) {
            String name = item.path("Name").asString("");
            if (name.equals("MpesaReceiptNumber")) receipt = item.path("Value").asString(null);
            if (name.equals("Amount")) amount = item.path("Value").decimalValue();
        }
        return new CallbackResult(id, code == 0, receipt, amount, cb.path("ResultDesc").asString(""));
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) return cachedToken;
        String basic = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));
        JsonNode res = http.get().uri("/oauth/v1/generate?grant_type=client_credentials")
                .header("Authorization", "Basic " + basic)
                .retrieve()
                .body(JsonNode.class);
        if (res == null || res.path("access_token").isMissingNode()) {
            throw new PaymentException("M-Pesa sign-in failed. Check the Daraja consumer key and secret.");
        }
        cachedToken = res.path("access_token").asString();
        // Refresh a minute early.
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, res.path("expires_in").asLong(3599) - 60));
        return cachedToken;
    }
}
