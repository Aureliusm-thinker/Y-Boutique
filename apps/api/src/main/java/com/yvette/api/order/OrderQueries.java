package com.yvette.api.order;

import com.yvette.api.order.OrderDtos.OrderView;
import com.yvette.api.payment.PaymentService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only order views for shoppers (kept apart from OrderService so payments can depend on that). */
@Service
@Transactional(readOnly = true)
public class OrderQueries {

    private final OrderService orders;
    private final PaymentService payments;

    public OrderQueries(OrderService orders, PaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public OrderView view(String number, UUID token) {
        Order order = orders.findByNumberAndToken(number, token);
        return OrderDtos.view(order, payments.latest(order.getId()));
    }
}
