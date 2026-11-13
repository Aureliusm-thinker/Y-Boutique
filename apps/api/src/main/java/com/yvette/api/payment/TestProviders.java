package com.yvette.api.payment;

import com.yvette.api.order.Order;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stand-ins for card and M-Pesa payments so the whole checkout can be used and tested without
 * real accounts. Enabled by {@code yvette.payments.test-mode} (on in the dev profile, off by default).
 *
 * <ul>
 *   <li>Card: 4242 4242 4242 4242 succeeds, 4000 0000 0000 0002 is declined.</li>
 *   <li>M-Pesa: the payment page offers "approve" and "cancel" buttons in place of the phone prompt.</li>
 * </ul>
 */
@Configuration
public class TestProviders {

    public static final String TEST_CARD = "TEST";
    public static final String TEST_MPESA = "MPESA_TEST";
    public static final String CARD_SUCCESS = "4242424242424242";
    public static final String CARD_DECLINED = "4000000000000002";

    @Bean
    PaymentProvider testCardProvider(@Value("${yvette.payments.test-mode:false}") boolean testMode) {
        return new Simple(TEST_CARD, Order.PaymentMethod.CARD, testMode, PaymentProvider.NextAction.ENTER_TEST_CARD);
    }

    @Bean
    PaymentProvider testMpesaProvider(@Value("${yvette.payments.test-mode:false}") boolean testMode) {
        return new Simple(TEST_MPESA, Order.PaymentMethod.MPESA, testMode, PaymentProvider.NextAction.WAIT_FOR_PHONE);
    }

    record Simple(String name, Order.PaymentMethod method, boolean enabled, NextAction action)
            implements PaymentProvider {

        @Override
        public boolean isTest() {
            return true;
        }

        @Override
        public StartResult start(Order order, Payment payment) {
            return new StartResult(action, name + "-" + UUID.randomUUID(), null);
        }

        @Override
        public RefundResult refund(Order order, Payment charge, BigDecimal amount) {
            return new RefundResult(true, name + "-REFUND-" + UUID.randomUUID(), false, "Test refund");
        }
    }
}
