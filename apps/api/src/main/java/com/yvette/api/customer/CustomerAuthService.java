package com.yvette.api.customer;

import com.yvette.api.common.BadRequestException;
import com.yvette.api.common.ConflictException;
import com.yvette.api.customer.CustomerDtos.RegisterRequest;
import com.yvette.api.customer.CustomerDtos.Session;

import com.yvette.api.notification.MailRequest;
import com.yvette.api.notification.Mailer.Block;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.staff.LoginRateLimiter;
import com.yvette.api.staff.TokenService;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shopper registration, sign-in, email verification and password reset (FR-CA-01/02). */
@Service
@Transactional
public class CustomerAuthService {

    private static final Duration VERIFY_TTL = Duration.ofDays(3);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final CustomerRepository customers;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final OneTimeTokens oneTimeTokens;
    private final LoginRateLimiter rateLimiter;
    private final AccountService accounts;
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final String siteUrl;
    private final String dummyHash;

    public CustomerAuthService(CustomerRepository customers, PasswordEncoder passwords, TokenService tokens,
                               OneTimeTokens oneTimeTokens, LoginRateLimiter rateLimiter, AccountService accounts,
                               OrderRepository orders, ApplicationEventPublisher events,
                               @Value("${yvette.site-url}") String siteUrl) {
        this.customers = customers;
        this.passwords = passwords;
        this.tokens = tokens;
        this.oneTimeTokens = oneTimeTokens;
        this.rateLimiter = rateLimiter;
        this.accounts = accounts;
        this.orders = orders;
        this.events = events;
        this.siteUrl = siteUrl;
        this.dummyHash = passwords.encode("timing-equaliser-not-a-real-password");
    }

    public Session register(RegisterRequest req) {
        String email = Customer.normaliseEmail(req.email());
        if (customers.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists. Sign in or reset your password.");
        }
        Customer c = customers.save(new Customer(email, passwords.encode(req.password()), req.fullName(),
                req.marketingOptIn()));
        sendVerification(c);
        return session(c);
    }

    public Session login(String rawEmail, String password) {
        String email = Customer.normaliseEmail(rawEmail);
        String key = "customer:" + email;
        if (rateLimiter.isBlocked(key)) throw new LockedException("Too many attempts. Try again in 15 minutes.");
        Customer c = customers.findByEmail(email).orElse(null);
        boolean ok = passwords.matches(password, c == null ? dummyHash : c.getPasswordHash());
        if (c == null || !ok) {
            rateLimiter.recordFailure(key);
            throw new BadCredentialsException("Wrong email or password");
        }
        rateLimiter.reset(key);
        c.recordLogin();
        return session(c);
    }

    /** Confirms the email address and links past guest orders placed with it. */
    public Session verifyEmail(String token) {
        Long id = oneTimeTokens.consume(token, OneTimeTokens.Purpose.VERIFY_EMAIL);
        if (id == null) throw new BadRequestException("This link has expired or was already used. Request a new one.");
        Customer c = customers.findById(id).orElseThrow();
        c.markEmailVerified();
        // Only after proving they own the inbox do earlier guest orders join the account.
        for (Order o : orders.findByEmailIgnoreCaseAndCustomerIdIsNull(c.getEmail())) {
            o.assignCustomer(c.getId());
        }
        return session(c);
    }

    public void resendVerification(Long customerId) {
        Customer c = customers.findById(customerId).orElseThrow();
        if (!c.isEmailVerified()) sendVerification(c);
    }

    /** Always succeeds (doesn't reveal whether the email has an account). */
    public void requestPasswordReset(String rawEmail) {
        String email = Customer.normaliseEmail(rawEmail);
        String key = "reset:" + email;
        if (rateLimiter.isBlocked(key)) return;
        rateLimiter.recordFailure(key); // at most 5 reset emails per 15 minutes per address
        customers.findByEmail(email).ifPresent(c -> {
            String token = oneTimeTokens.create(c.getId(), OneTimeTokens.Purpose.RESET_PASSWORD, RESET_TTL);
            events.publishEvent(new MailRequest(c.getEmail(), "Reset your Yvette password", List.of(
                    Block.text("Hi " + firstName(c) + ", we received a request to reset your password. "
                            + "The link works for one hour. If you didn't ask for this, you can ignore this email."),
                    Block.button("Choose a new password", siteUrl + "/account/reset?token=" + token))));
        });
    }

    public Session resetPassword(String token, String newPassword) {
        Long id = oneTimeTokens.consume(token, OneTimeTokens.Purpose.RESET_PASSWORD);
        if (id == null) throw new BadRequestException("This link has expired or was already used. Request a new one.");
        Customer c = customers.findById(id).orElseThrow();
        c.changePasswordHash(passwords.encode(newPassword));
        c.markEmailVerified(); // they proved they own the inbox
        rateLimiter.reset("customer:" + c.getEmail());
        return session(c);
    }

    private void sendVerification(Customer c) {
        String token = oneTimeTokens.create(c.getId(), OneTimeTokens.Purpose.VERIFY_EMAIL, VERIFY_TTL);
        events.publishEvent(new MailRequest(c.getEmail(), "Confirm your email for Yvette", List.of(
                Block.text("Welcome to Yvette, " + firstName(c) + "! Please confirm your email address so we can "
                        + "keep your account safe and show your past orders."),
                Block.button("Confirm my email", siteUrl + "/account/verify?token=" + token))));
    }

    private Session session(Customer c) {
        TokenService.IssuedToken t = tokens.issueCustomer(c.getId(), c.getEmail(), c.getFullName());
        return new Session(t.value(), t.expiresAt(), accounts.profile(c));
    }

    private static String firstName(Customer c) {
        return c.getFullName().split("\\s+")[0];
    }
}
