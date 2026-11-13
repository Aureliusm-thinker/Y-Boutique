package com.yvette.api.cart;

import com.yvette.api.cart.CartDtos.CartLine;
import com.yvette.api.cart.CartDtos.CartView;
import com.yvette.api.cart.CartDtos.DiscountView;
import com.yvette.api.cart.CartDtos.ShippingOption;
import com.yvette.api.catalog.ProductStatus;
import com.yvette.api.catalog.ProductVariant;
import com.yvette.api.catalog.ProductVariantRepository;
import com.yvette.api.checkout.Pricing;
import com.yvette.api.checkout.ShippingMethodRepository;
import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.NotFoundException;
import com.yvette.api.inventory.InventoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persistent shopping bag (FR-CO-01). Stock is only held once an order is placed, not while in the bag. */
@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final ProductVariantRepository variants;
    private final InventoryService inventory;
    private final Pricing pricing;
    private final ShippingMethodRepository shippingMethods;
    private final String currency;

    public CartService(CartRepository carts, ProductVariantRepository variants, InventoryService inventory,
                       Pricing pricing, ShippingMethodRepository shippingMethods,
                       @Value("${yvette.store.currency:KES}") String currency) {
        this.carts = carts;
        this.variants = variants;
        this.inventory = inventory;
        this.pricing = pricing;
        this.shippingMethods = shippingMethods;
        this.currency = currency;
    }

    public CartView create() {
        return view(carts.save(new Cart(UUID.randomUUID())));
    }

    @Transactional(readOnly = true)
    public CartView get(UUID token) {
        return view(find(token));
    }

    public CartView add(UUID token, Long variantId, int quantity) {
        Cart cart = find(token);
        ProductVariant variant = variants.findWithProduct(List.of(variantId)).stream().findFirst()
                .filter(v -> v.getProduct().getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("This item is no longer available"));
        int available = inventory.availableByVariant(List.of(variantId)).getOrDefault(variantId, 0);
        int inBag = cart.item(variantId).map(CartItem::getQuantity).orElse(0);
        if (available <= 0) throw new BadRequestException("Sorry, this size is sold out");
        if (inBag >= Math.min(available, Cart.MAX_QUANTITY)) {
            throw new BadRequestException(available <= inBag
                    ? "You already have all " + available + " we have in your bag"
                    : "You can buy up to " + Cart.MAX_QUANTITY + " of each item");
        }
        if (cart.item(variantId).isEmpty() && cart.getItems().size() >= Cart.MAX_LINES) {
            throw new BadRequestException("Your bag is full");
        }
        cart.add(variant.getId(), Math.min(quantity, available - inBag));
        return view(cart);
    }

    public CartView setQuantity(UUID token, Long variantId, int quantity) {
        Cart cart = find(token);
        cart.setQuantity(variantId, quantity);
        return view(cart);
    }

    public CartView remove(UUID token, Long variantId) {
        Cart cart = find(token);
        cart.remove(variantId);
        return view(cart);
    }

    /** Applies a discount code; a code that can't be used is refused with the reason. */
    public CartView applyDiscount(UUID token, String code) {
        Cart cart = find(token);
        if (code == null || code.isBlank()) {
            cart.setDiscountCode(null);
            return view(cart);
        }
        BigDecimal subtotal = priceLines(cart).subtotal();
        Pricing.AppliedDiscount d = pricing.discountFor(subtotal, code, Instant.now());
        if (d.problem() != null) throw new BadRequestException(d.problem());
        cart.setDiscountCode(d.code());
        return view(cart);
    }

    /**
     * On sign-in, the guest's bag joins the customer's saved bag (FR-CO-01): quantities are added up
     * (capped), and the customer keeps one bag across devices. Returns the token to keep using.
     */
    public CartView attachToCustomer(Long customerId, UUID guestToken) {
        Cart guest = carts.findByToken(guestToken).orElse(null);
        Cart saved = carts.findFirstByCustomerIdOrderByUpdatedAtDesc(customerId).orElse(null);
        if (guest != null && guest.getCustomerId() != null && !guest.getCustomerId().equals(customerId)) {
            guest = null; // someone else's bag: never merge it
        }
        if (saved == null && guest == null) {
            Cart fresh = new Cart(UUID.randomUUID());
            fresh.assignTo(customerId);
            return view(carts.save(fresh));
        }
        if (saved == null) {
            guest.assignTo(customerId);
            return view(guest);
        }
        if (guest != null && !guest.getId().equals(saved.getId())) {
            for (CartItem item : guest.getItems()) {
                if (saved.item(item.getVariantId()).isPresent() || saved.getItems().size() < Cart.MAX_LINES) {
                    saved.add(item.getVariantId(), item.getQuantity());
                }
            }
            if (saved.getDiscountCode() == null && guest.getDiscountCode() != null) {
                saved.setDiscountCode(guest.getDiscountCode());
            }
            carts.delete(guest);
        }
        return view(saved);
    }

    public Cart find(UUID token) {
        return carts.findByToken(token).orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    // ------------------------------------------------------------------ view

    record PricedLines(List<CartLine> lines, BigDecimal subtotal, boolean allAvailable) {}

    PricedLines priceLines(Cart cart) {
        List<Long> ids = cart.getItems().stream().map(CartItem::getVariantId).toList();
        Map<Long, ProductVariant> byId = variants.findWithProduct(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<Long, Integer> available = inventory.availableByVariant(ids);
        List<CartLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allAvailable = true;
        for (CartItem item : cart.getItems()) {
            ProductVariant v = byId.get(item.getVariantId());
            if (v == null) continue;
            var product = v.getProduct();
            int left = available.getOrDefault(v.getId(), 0);
            String problem = null;
            if (product.getStatus() != ProductStatus.ACTIVE) problem = "No longer available";
            else if (left <= 0) problem = "Sold out";
            else if (left < item.getQuantity()) problem = "Only " + left + " left — please reduce the quantity";
            if (problem != null) allAvailable = false;
            BigDecimal lineTotal = v.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            if (problem == null) subtotal = subtotal.add(lineTotal);
            lines.add(new CartLine(v.getId(), product.getSlug(), product.getTitle(), v.getSize(), v.getColour(),
                    v.getSku(), product.getImages().isEmpty() ? null : product.getImages().getFirst().getUrl(),
                    v.getPrice(), v.getCompareAtPrice(), item.getQuantity(), lineTotal, left, problem));
        }
        return new PricedLines(lines, subtotal, allAvailable);
    }

    CartView view(Cart cart) {
        PricedLines priced = priceLines(cart);
        Pricing.AppliedDiscount d = pricing.discountFor(priced.subtotal(), cart.getDiscountCode(), Instant.now());
        DiscountView discount = d.promotion() == null && d.problem() == null ? null
                : new DiscountView(d.code(), d.promotion() == null ? null : d.promotion().getName(), d.amount(),
                d.freeShipping(), d.problem());
        List<ShippingOption> options = shippingMethods.findByActiveTrueOrderByPositionAsc().stream()
                .map(m -> new ShippingOption(m.getCode(), m.getName(), m.getDescription(),
                        d.freeShipping() ? BigDecimal.ZERO : m.priceFor(priced.subtotal().subtract(d.amount())),
                        m.getFreeOver(), m.needsAddress()))
                .toList();
        int count = priced.lines().stream().mapToInt(CartLine::quantity).sum();
        return new CartView(cart.getToken(), priced.lines(), count, priced.subtotal(), discount,
                priced.subtotal().subtract(d.amount()), options, currency,
                !priced.lines().isEmpty() && priced.allAvailable());
    }
}
