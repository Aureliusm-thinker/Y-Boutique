package com.yvette.api.customer;

import com.yvette.api.cart.CartDtos.CartView;
import com.yvette.api.cart.CartService;
import com.yvette.api.catalog.CatalogDtos.ProductSummaryDto;
import com.yvette.api.common.CurrentCustomer;
import com.yvette.api.customer.CustomerDtos.AddressRequest;
import com.yvette.api.customer.CustomerDtos.AttachCartRequest;
import com.yvette.api.customer.CustomerDtos.AttachCartResponse;
import com.yvette.api.customer.CustomerDtos.ChangePasswordRequest;
import com.yvette.api.customer.CustomerDtos.DeleteAccountRequest;
import com.yvette.api.customer.CustomerDtos.EmailRequest;
import com.yvette.api.customer.CustomerDtos.LoginRequest;
import com.yvette.api.customer.CustomerDtos.Preferences;
import com.yvette.api.customer.CustomerDtos.Profile;
import com.yvette.api.customer.CustomerDtos.RegisterRequest;
import com.yvette.api.customer.CustomerDtos.ResetPasswordRequest;
import com.yvette.api.customer.CustomerDtos.Session;
import com.yvette.api.customer.CustomerDtos.StockAlertRequest;
import com.yvette.api.customer.CustomerDtos.TokenRequest;
import com.yvette.api.customer.CustomerDtos.UpdateProfileRequest;
import com.yvette.api.order.OrderDtos.OrderSummary;
import com.yvette.api.order.OrderDtos.OrderView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints for shoppers' accounts. */
public final class CustomerControllers {

    private CustomerControllers() {}

    @RestController
    @RequestMapping("/api/v1/auth/customer")
    public static class Auth {

        private final CustomerAuthService auth;

        public Auth(CustomerAuthService auth) {
            this.auth = auth;
        }

        @PostMapping("/register")
        @ResponseStatus(HttpStatus.CREATED)
        public Session register(@Valid @RequestBody RegisterRequest request) {
            return auth.register(request);
        }

        @PostMapping("/login")
        public Session login(@Valid @RequestBody LoginRequest request) {
            return auth.login(request.email(), request.password());
        }

        @PostMapping("/verify-email")
        public Session verify(@Valid @RequestBody TokenRequest request) {
            return auth.verifyEmail(request.token());
        }

        @PostMapping("/forgot-password")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void forgot(@Valid @RequestBody EmailRequest request) {
            auth.requestPasswordReset(request.email());
        }

        @PostMapping("/reset-password")
        public Session reset(@Valid @RequestBody ResetPasswordRequest request) {
            return auth.resetPassword(request.token(), request.password());
        }

        @ExceptionHandler(BadCredentialsException.class)
        ProblemDetail badCredentials(BadCredentialsException ex) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        @ExceptionHandler(LockedException.class)
        ProblemDetail locked(LockedException ex) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        }
    }

    @RestController
    @RequestMapping("/api/v1/account")
    public static class Account {

        private final AccountService accounts;
        private final CustomerAuthService auth;
        private final CartService carts;

        public Account(AccountService accounts, CustomerAuthService auth, CartService carts) {
            this.accounts = accounts;
            this.auth = auth;
            this.carts = carts;
        }

        @GetMapping("/me")
        public Profile me(@AuthenticationPrincipal Jwt jwt) {
            return accounts.profile(CurrentCustomer.id(jwt));
        }

        @PutMapping("/me")
        public Profile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest req) {
            return accounts.updateProfile(CurrentCustomer.id(jwt), req.fullName(), req.phone(), req.marketingOptIn());
        }

        @PutMapping("/me/preferences")
        public Profile preferences(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Preferences req) {
            return accounts.updatePreferences(CurrentCustomer.id(jwt), req);
        }

        @PutMapping("/me/password")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void password(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req) {
            accounts.changePassword(CurrentCustomer.id(jwt), req.currentPassword(), req.newPassword());
        }

        @PostMapping("/me/resend-verification")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void resendVerification(@AuthenticationPrincipal Jwt jwt) {
            auth.resendVerification(CurrentCustomer.id(jwt));
        }

        @PostMapping("/addresses")
        @ResponseStatus(HttpStatus.CREATED)
        public Profile addAddress(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest req) {
            return accounts.saveAddress(CurrentCustomer.id(jwt), null, req);
        }

        @PutMapping("/addresses/{id}")
        public Profile updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                     @Valid @RequestBody AddressRequest req) {
            return accounts.saveAddress(CurrentCustomer.id(jwt), id, req);
        }

        @DeleteMapping("/addresses/{id}")
        public Profile deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
            return accounts.deleteAddress(CurrentCustomer.id(jwt), id);
        }

        @GetMapping("/orders")
        public List<OrderSummary> orders(@AuthenticationPrincipal Jwt jwt) {
            return accounts.orders(CurrentCustomer.id(jwt));
        }

        @GetMapping("/orders/{number}")
        public OrderView order(@AuthenticationPrincipal Jwt jwt, @PathVariable String number) {
            return accounts.order(CurrentCustomer.id(jwt), number);
        }

        @GetMapping("/wishlist")
        public List<ProductSummaryDto> wishlist(@AuthenticationPrincipal Jwt jwt) {
            return accounts.wishlist(CurrentCustomer.id(jwt));
        }

        @GetMapping("/wishlist/ids")
        public List<Long> wishlistIds(@AuthenticationPrincipal Jwt jwt) {
            return accounts.wishlistIds(CurrentCustomer.id(jwt));
        }

        @PutMapping("/wishlist/{productId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void addToWishlist(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
            accounts.addToWishlist(CurrentCustomer.id(jwt), productId);
        }

        @DeleteMapping("/wishlist/{productId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void removeFromWishlist(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
            accounts.removeFromWishlist(CurrentCustomer.id(jwt), productId);
        }

        /** Joins the guest bag to the account after sign-in; returns the bag to keep using. */
        @PostMapping("/cart")
        public AttachCartResponse attachCart(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AttachCartRequest req) {
            CartView cart = carts.attachToCustomer(CurrentCustomer.id(jwt), req.cartToken());
            return new AttachCartResponse(cart.token(), cart.itemCount());
        }

        @GetMapping("/export")
        public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal Jwt jwt) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"yvette-my-data.json\"")
                    .body(accounts.export(CurrentCustomer.id(jwt)));
        }

        @PostMapping("/delete")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteAccountRequest req) {
            accounts.deleteAccount(CurrentCustomer.id(jwt), req.password());
        }
    }

    /** "Email me when it's back in stock" — open to guests (FR / E4-6). */
    @RestController
    @RequestMapping("/api/v1/catalog/stock-alerts")
    public static class StockAlerts {

        private final StockAlertService alerts;

        public StockAlerts(StockAlertService alerts) {
            this.alerts = alerts;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void subscribe(@Valid @RequestBody StockAlertRequest req, @AuthenticationPrincipal Jwt jwt) {
            alerts.subscribe(req.variantId(), req.email(), CurrentCustomer.idOrNull(jwt));
        }
    }
}
