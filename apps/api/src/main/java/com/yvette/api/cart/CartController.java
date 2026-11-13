package com.yvette.api.cart;

import com.yvette.api.cart.CartDtos.AddItemRequest;
import com.yvette.api.cart.CartDtos.CartView;
import com.yvette.api.cart.CartDtos.DiscountRequest;
import com.yvette.api.cart.CartDtos.SetQuantityRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public cart API. The cart token is a random secret held in the shopper's httpOnly cookie. */
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartView create() {
        return carts.create();
    }

    @GetMapping("/{token}")
    public CartView get(@PathVariable UUID token) {
        return carts.get(token);
    }

    @PostMapping("/{token}/items")
    public CartView add(@PathVariable UUID token, @Valid @RequestBody AddItemRequest request) {
        return carts.add(token, request.variantId(), request.quantity());
    }

    @PutMapping("/{token}/items/{variantId}")
    public CartView setQuantity(@PathVariable UUID token, @PathVariable Long variantId,
                                @Valid @RequestBody SetQuantityRequest request) {
        return carts.setQuantity(token, variantId, request.quantity());
    }

    @DeleteMapping("/{token}/items/{variantId}")
    public CartView remove(@PathVariable UUID token, @PathVariable Long variantId) {
        return carts.remove(token, variantId);
    }

    @PutMapping("/{token}/discount")
    public CartView applyDiscount(@PathVariable UUID token, @Valid @RequestBody DiscountRequest request) {
        return carts.applyDiscount(token, request.code());
    }
}
