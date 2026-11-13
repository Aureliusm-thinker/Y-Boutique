package com.yvette.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yvette.api.notification.MailRequest;
import com.yvette.api.notification.Mailer;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
@RecordApplicationEvents
class AccountApiTests {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ApplicationEvents events;
    TestShop shop;

    @BeforeEach
    void setUp() {
        shop = new TestShop(mvc, jdbc);
    }

    @Test
    void registerVerifyLinksGuestOrdersAndSignIn() throws Exception {
        // A guest order placed before the account exists…
        TestShop.PlacedOrder guestOrder = shop.buyAndPay("Njeri@Example.com", null, "LWD-SAND-S", 1);

        String session = register("njeri@example.com", "Njeri Mwangi");
        mvc.perform(get("/api/v1/account/orders").header("Authorization", "Bearer " + session))
                .andExpect(jsonPath("$.length()").value(0)); // not linked until the email is proven

        mvc.perform(post("/api/v1/auth/customer/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("NJERI@example.com", "Someone")))
                .andExpect(status().isConflict());

        String token = linkFromEmail("Confirm your email", "/account/verify?token=");
        mvc.perform(post("/api/v1/auth/customer/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.emailVerified").value(true));
        mvc.perform(post("/api/v1/auth/customer/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest()); // single use

        mvc.perform(get("/api/v1/account/orders").header("Authorization", "Bearer " + session))
                .andExpect(jsonPath("$[*].number").value(hasItem(guestOrder.number())));
        mvc.perform(get("/api/v1/account/orders/" + guestOrder.number()).header("Authorization", "Bearer " + session))
                .andExpect(jsonPath("$.status").value("PAID"));

        mvc.perform(post("/api/v1/auth/customer/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"njeri@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        login("njeri@example.com", "a-good-password");
    }

    @Test
    void customersCannotUseStaffEndpointsAndGuestsCannotUseAccount() throws Exception {
        String session = register("kamau@example.com", "Kamau Otieno");
        mvc.perform(get("/api/v1/admin/me").header("Authorization", "Bearer " + session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/account/me")).andExpect(status().isUnauthorized());
        String staff = shop.owner();
        mvc.perform(get("/api/v1/account/me").header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
    }

    @Test
    void profileAddressesPreferencesAndWishlist() throws Exception {
        String s = "Bearer " + register("zawadi@example.com", "Zawadi Achieng");
        mvc.perform(put("/api/v1/account/me").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Zawadi A.\",\"phone\":\"+254 722 000 111\",\"marketingOptIn\":true}"))
                .andExpect(jsonPath("$.phone").value("254722000111"));
        mvc.perform(post("/api/v1/account/addresses").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                        .content(address("Home", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addresses[0].isDefault").value(true)); // first one becomes default
        String profile = mvc.perform(post("/api/v1/account/addresses").header("Authorization", s)
                        .contentType(MediaType.APPLICATION_JSON).content(address("Office", true)))
                .andExpect(jsonPath("$.addresses[0].label").value("Office"))
                .andExpect(jsonPath("$.addresses[1].isDefault").value(false))
                .andReturn().getResponse().getContentAsString();
        Integer office = JsonPath.read(profile, "$.addresses[0].id");
        mvc.perform(delete("/api/v1/account/addresses/" + office).header("Authorization", s))
                .andExpect(jsonPath("$.addresses.length()").value(1))
                .andExpect(jsonPath("$.addresses[0].isDefault").value(true));
        mvc.perform(put("/api/v1/account/me/preferences").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sizes\":[\"M\",\"M\",\" L \"],\"colours\":[\"Black\"],\"styles\":[\"minimal\"]}"))
                .andExpect(jsonPath("$.preferences.sizes.length()").value(2))
                .andExpect(jsonPath("$.preferences.colours[0]").value("Black"));

        long product = jdbc.sql("SELECT id FROM products WHERE slug = 'silk-slip-dress'").query(Long.class).single();
        mvc.perform(put("/api/v1/account/wishlist/" + product).header("Authorization", s)).andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/account/wishlist/" + product).header("Authorization", s)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/account/wishlist").header("Authorization", s))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("silk-slip-dress"));
        mvc.perform(delete("/api/v1/account/wishlist/" + product).header("Authorization", s));
        mvc.perform(get("/api/v1/account/wishlist").header("Authorization", s)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void guestBagMergesIntoSavedBagOnSignIn() throws Exception {
        String s = "Bearer " + register("bag@example.com", "Bag Tester");
        // Saved bag from another device.
        String saved = shop.newCart();
        shop.add(saved, "CPS-WHT-M", 1);
        mvc.perform(post("/api/v1/account/cart").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cartToken\":\"" + saved + "\"}")).andExpect(jsonPath("$.cartToken").value(saved));
        // New guest bag on this device.
        String guest = shop.newCart();
        shop.add(guest, "CPS-WHT-M", 2);
        shop.add(guest, "WST-NAT-OS", 1);
        mvc.perform(post("/api/v1/account/cart").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartToken\":\"" + guest + "\"}"))
                .andExpect(jsonPath("$.cartToken").value(saved))
                .andExpect(jsonPath("$.itemCount").value(4));
        mvc.perform(get("/api/v1/carts/" + guest)).andExpect(status().isNotFound());
    }

    @Test
    void forgotAndResetPassword() throws Exception {
        register("reset@example.com", "Reset Tester");
        mvc.perform(post("/api/v1/auth/customer/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody-here@example.com\"}")).andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/auth/customer/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Reset@Example.com\"}")).andExpect(status().isAccepted());
        String token = linkFromEmail("Reset your Yvette password", "/account/reset?token=");
        mvc.perform(post("/api/v1/auth/customer/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"brand-new-pass\"}"))
                .andExpect(status().isOk());
        login("reset@example.com", "brand-new-pass");
    }

    @Test
    void exportAndDeleteKeepOrdersButRemovePersonalData() throws Exception {
        String session = register("leaving@example.com", "Leaving Customer");
        TestShop.PlacedOrder order = shop.buyAndPay("leaving@example.com", session, "WST-NAT-OS", 1);
        String s = "Bearer " + session;
        mvc.perform(get("/api/v1/account/export").header("Authorization", s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("leaving@example.com"))
                .andExpect(jsonPath("$.orders[0].number").value(order.number()));
        mvc.perform(post("/api/v1/account/delete").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"wrong\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/account/delete").header("Authorization", s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"a-good-password\"}")).andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM customers WHERE email = 'leaving@example.com'").query(Integer.class).single()).isZero();
        String email = jdbc.sql("SELECT email FROM orders WHERE id = :id").param("id", order.id()).query(String.class).single();
        assertThat(email).startsWith("deleted-").doesNotContain("leaving");
        mvc.perform(get("/api/v1/account/me").header("Authorization", s)).andExpect(status().isNotFound());
    }

    @Test
    void backInStockAlertIsSentWhenStockReturns() throws Exception {
        long soldOut = shop.createLiveProduct("ALERT-1", 0); // its own product, so other tests' stock is untouched
        mvc.perform(post("/api/v1/catalog/stock-alerts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + soldOut + ",\"email\":\"waiting@example.com\"}")).andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/catalog/stock-alerts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + shop.variantId("LWD-SAND-M") + ",\"email\":\"waiting@example.com\"}"))
                .andExpect(status().isBadRequest()); // already in stock

        shop.as(shop.owner(), post("/api/v1/admin/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + soldOut + ",\"delta\":3,\"reason\":\"RECEIVED\"}")).andExpect(status().isOk());

        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        Integer notified = 0;
        while (Instant.now().isBefore(deadline) && notified == 0) {
            notified = jdbc.sql("SELECT count(*) FROM stock_alerts WHERE email = 'waiting@example.com' AND notified_at IS NOT NULL")
                    .query(Integer.class).single();
            Thread.sleep(200);
        }
        assertThat(notified).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private String register(String email, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/customer/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer.emailVerified").value(false))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private void login(String email, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/customer/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    private static String registerJson(String email, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"a-good-password\",\"fullName\":\"" + name + "\"}";
    }

    private static String address(String label, boolean makeDefault) {
        return """
                {"label":"%s","fullName":"Zawadi Achieng","phone":"0711222333","line1":"Kilimani Rd 3",
                 "city":"Nairobi","county":"Nairobi","makeDefault":%s}""".formatted(label, makeDefault);
    }

    /** Pulls the one-time token out of the most recent email with this subject. */
    private String linkFromEmail(String subjectStart, String marker) {
        MailRequest mail = events.stream(MailRequest.class)
                .filter(m -> m.subject().startsWith(subjectStart))
                .reduce((a, b) -> b).orElseThrow();
        String link = mail.blocks().stream().map(Mailer.Block::link).filter(l -> l != null && l.contains(marker))
                .findFirst().orElseThrow();
        return link.substring(link.indexOf(marker) + marker.length());
    }
}
