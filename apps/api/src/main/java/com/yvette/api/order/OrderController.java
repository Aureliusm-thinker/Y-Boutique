package com.yvette.api.order;

import com.yvette.api.checkout.CheckoutDtos.RetryPaymentRequest;
import com.yvette.api.checkout.CheckoutDtos.TestCardRequest;
import com.yvette.api.checkout.CheckoutDtos.TestMpesaRequest;
import com.yvette.api.checkout.KenyanPhone;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.order.OrderDtos.OrderView;
import com.yvette.api.order.OrderDtos.PaymentView;
import com.yvette.api.payment.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A shopper's own order, opened with the secret access token from their confirmation link or email.
 * Also the payment page actions (retry, and the test-mode card form / M-Pesa simulator).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orders;
    private final OrderQueries queries;
    private final PaymentService payments;

    public OrderController(OrderService orders, OrderQueries queries, PaymentService payments) {
        this.orders = orders;
        this.queries = queries;
        this.payments = payments;
    }

    @GetMapping("/{number}")
    public OrderView get(@PathVariable String number, @RequestParam UUID token) {
        return queries.view(number, token);
    }

    /** Starts a new payment attempt (after a decline, a cancelled M-Pesa prompt, or a different number). */
    @PostMapping("/{number}/pay")
    public PaymentView retry(@PathVariable String number, @Valid @RequestBody RetryPaymentRequest request) {
        Order order = orders.findByNumberAndToken(number, request.token());
        String phone = null;
        if (order.getPaymentMethod() == Order.PaymentMethod.MPESA && request.mpesaPhone() != null
                && !request.mpesaPhone().isBlank()) {
            phone = KenyanPhone.normalise(request.mpesaPhone());
            if (phone == null) throw new BadRequestException("Enter a Kenyan mobile number, e.g. 0712 345 678");
        }
        return payments.start(order.getId(), phone);
    }

    @PostMapping("/{number}/test-card")
    public OrderView testCard(@PathVariable String number, @Valid @RequestBody TestCardRequest request) {
        Order order = orders.findByNumberAndToken(number, request.token());
        payments.payWithTestCard(order.getId(), request.cardNumber());
        return get(number, request.token());
    }

    @PostMapping("/{number}/test-mpesa")
    public OrderView testMpesa(@PathVariable String number, @Valid @RequestBody TestMpesaRequest request) {
        Order order = orders.findByNumberAndToken(number, request.token());
        payments.simulateMpesa(order.getId(), request.approve());
        return get(number, request.token());
    }
}
