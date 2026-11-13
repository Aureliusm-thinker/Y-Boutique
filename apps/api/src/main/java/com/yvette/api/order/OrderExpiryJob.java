package com.yvette.api.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Unpaid orders give their reserved stock back after the payment window (default 30 minutes). */
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderService orders;

    public OrderExpiryJob(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${yvette.checkout.expiry-check:PT1M}", initialDelayString = "PT30S")
    public void expireUnpaidOrders() {
        for (Long id : orders.unpaidPastPaymentWindow()) {
            try {
                // Each cancellation runs in its own transaction (OrderService is @Transactional).
                orders.cancelUnpaid(id, "Payment not received in time — order cancelled", null);
            } catch (RuntimeException e) {
                log.warn("Could not expire order {}: {}", id, e.getMessage());
            }
        }
    }
}
