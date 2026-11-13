package com.yvette.api.checkout;

import com.yvette.api.cart.Cart;
import com.yvette.api.cart.CartItem;
import com.yvette.api.cart.CartService;
import com.yvette.api.catalog.ProductStatus;
import com.yvette.api.catalog.ProductVariant;
import com.yvette.api.catalog.ProductVariantRepository;
import com.yvette.api.checkout.CheckoutDtos.CheckoutRequest;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.inventory.InventoryLevel;
import com.yvette.api.inventory.InventoryService;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderLine;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.payment.PaymentService;
import com.yvette.api.promotion.PromotionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a cart into an order (FR-CO-02..09). Stock rows are locked and reserved in the same transaction
 * that creates the order, so two shoppers can never both buy the last unit (FR-CO-09).
 * {@link Checkout} then starts the payment outside this transaction.
 */
@Service
public class CheckoutService {

    private final CartService carts;
    private final ProductVariantRepository variants;
    private final InventoryService inventory;
    private final ShippingMethodRepository shippingMethods;
    private final Pricing pricing;
    private final PromotionRepository promotions;
    private final OrderRepository orders;
    private final PaymentService payments;
    private final String currency;

    public CheckoutService(CartService carts, ProductVariantRepository variants, InventoryService inventory,
                           ShippingMethodRepository shippingMethods, Pricing pricing, PromotionRepository promotions,
                           OrderRepository orders, PaymentService payments,
                           @Value("${yvette.store.currency:KES}") String currency) {
        this.carts = carts;
        this.variants = variants;
        this.inventory = inventory;
        this.shippingMethods = shippingMethods;
        this.pricing = pricing;
        this.promotions = promotions;
        this.orders = orders;
        this.payments = payments;
        this.currency = currency;
    }

    @Transactional
    public Order placeOrder(CheckoutRequest req, Long customerId) {
        if (!payments.available(req.paymentMethod())) {
            throw new BadRequestException(req.paymentMethod() == Order.PaymentMethod.MPESA
                    ? "M-Pesa isn't available right now. Please pay by card."
                    : "Card payments aren't available right now. Please pay with M-Pesa.");
        }
        Cart cart = carts.find(req.cartToken());
        if (cart.getItems().isEmpty()) throw new BadRequestException("Your bag is empty");

        ShippingMethod method = shippingMethods.findById(req.shippingMethod()).filter(ShippingMethod::isActive)
                .orElseThrow(() -> new BadRequestException("Choose a delivery option"));
        String phone = phone(req.phone());
        Order.Address address = null;
        if (method.needsAddress()) {
            List<String> missing = new ArrayList<>();
            if (blank(req.addressLine1())) missing.add("Street address or building");
            if (blank(req.city())) missing.add("Town or city");
            if (blank(req.county()) || !CheckoutDtos.COUNTIES.contains(req.county())) missing.add("County");
            if (!missing.isEmpty()) throw new BadRequestException("Please complete your delivery address", missing);
            address = new Order.Address(req.addressLine1().strip(), trim(req.addressLine2()), req.city().strip(),
                    req.county(), trim(req.postalCode()), trim(req.deliveryNotes()));
        } else if (!blank(req.deliveryNotes())) {
            address = new Order.Address(null, null, null, null, null, trim(req.deliveryNotes()));
        }

        // Lock stock rows (in a fixed order) and check everything is still available.
        List<Long> ids = cart.getItems().stream().map(CartItem::getVariantId).toList();
        Map<Long, InventoryLevel> levels = inventory.lockForCheckout(ids);
        Map<Long, ProductVariant> byId = variants.findWithProduct(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        List<String> problems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductVariant v = byId.get(item.getVariantId());
            InventoryLevel level = levels.get(item.getVariantId());
            String name = v == null ? "An item" : v.getProduct().getTitle() + (v.getSize() == null ? "" : " (" + v.getSize() + ")");
            if (v == null || v.getProduct().getStatus() != ProductStatus.ACTIVE) {
                problems.add(name + " is no longer available");
            } else if (level == null || level.available() < item.getQuantity()) {
                int left = level == null ? 0 : level.available();
                problems.add(left == 0 ? name + " has just sold out" : name + ": only " + left + " left");
            } else {
                subtotal = subtotal.add(v.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        if (!problems.isEmpty()) {
            throw new ConflictException("Some items in your bag changed. Please review your bag.", problems);
        }

        Pricing.AppliedDiscount discount = pricing.discountFor(subtotal, cart.getDiscountCode(), Instant.now());
        if (discount.problem() != null) {
            throw new BadRequestException("Discount code " + discount.code() + ": " + discount.problem());
        }
        if (discount.promotion() != null && discount.promotion().getUsageLimit() != null) {
            // Hold the promotion row so the last uses of a limited code can't be over-spent.
            var locked = promotions.findForUpdate(discount.promotion().getId()).orElseThrow();
            if (locked.problemFor(subtotal, Instant.now()) != null) {
                throw new BadRequestException("Discount code " + discount.code() + " has just run out");
            }
        }
        Pricing.Totals totals = pricing.totals(subtotal, discount, method);

        Order order = new Order("YV-" + orders.nextNumber(), cart.getToken(), customerId,
                new Order.Contact(req.email().strip().toLowerCase(Locale.ROOT), phone, req.fullName().strip()),
                address, req.paymentMethod(), method.getCode(), method.getName(), currency);
        for (CartItem item : cart.getItems()) {
            ProductVariant v = byId.get(item.getVariantId());
            var product = v.getProduct();
            order.addLine(new OrderLine.Snapshot(v.getId(), product.getId(), v.getSku(), product.getTitle(),
                    v.getSize(), v.getColour(),
                    product.getImages().isEmpty() ? null : product.getImages().getFirst().getUrl(), v.getPrice()),
                    item.getQuantity());
            levels.get(v.getId()).reserve(item.getQuantity());
        }
        order.setTotals(totals.subtotal(), discount.amount(), totals.shipping(), totals.total(),
                discount.promotion() == null ? null : discount.promotion().getId(),
                discount.promotion() == null ? null : discount.promotion().getCode());
        order.log("PLACED", "Order placed", true, null);
        return orders.save(order);
    }

    static String phone(String input) {
        String p = KenyanPhone.normalise(input);
        if (p == null) throw new BadRequestException("Enter a Kenyan mobile number, e.g. 0712 345 678");
        return p;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return blank(s) ? null : s.strip();
    }
}
