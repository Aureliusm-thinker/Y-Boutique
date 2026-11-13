package com.yvette.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yvette.api.checkout.CheckoutDtos.CheckoutRequest;
import com.yvette.api.checkout.CheckoutService;
import com.yvette.api.common.ConflictException;
import com.yvette.api.order.Order;
import com.yvette.api.order.OrderRepository;
import com.yvette.api.order.OrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@ApiIntegrationTest
class CheckoutApiTests {

    @Autowired MockMvc mvc;
    @Autowired CheckoutService checkoutService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired JdbcClient jdbc;

    @Test
    void cartAddsItemsAppliesDiscountAndRejectsBadCodes() throws Exception {
        String cart = newCart();
        long variant = variantId("LWD-SAND-M");
        addItem(cart, variant, 2)
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.subtotal").value(25800));

        mvc.perform(put("/api/v1/carts/" + cart + "/discount").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("We don't recognise that code"));
        mvc.perform(put("/api/v1/carts/" + cart + "/discount").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"karibu10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount.code").value("KARIBU10"))
                .andExpect(jsonPath("$.discount.amount").value(2580))
                .andExpect(jsonPath("$.totalBeforeShipping").value(23220))
                // Over KSh 10,000 → Nairobi delivery is free.
                .andExpect(jsonPath("$.shippingOptions[0].code").value("NAIROBI"))
                .andExpect(jsonPath("$.shippingOptions[0].price").value(0));

        // Can't add more than we have (10 in stock).
        addItem(cart, variant, 10).andExpect(jsonPath("$.lines[0].quantity").value(10));
        addItem(cart, variant, 1).andExpect(status().isBadRequest());
    }

    @Test
    void mpesaCheckoutReservesStockThenPaymentCompletesTheOrder() throws Exception {
        long variant = variantId("CPS-WHT-L");
        int onHandBefore = onHand(variant);
        String cart = newCart();
        addItem(cart, variant, 1);

        String result = mvc.perform(post("/api/v1/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(cart, "MPESA", "NAIROBI")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("YV-")))
                .andExpect(jsonPath("$.payment.nextAction").value("WAIT_FOR_PHONE"))
                .andExpect(jsonPath("$.payment.phone").value("254712345678"))
                .andReturn().getResponse().getContentAsString();
        String number = JsonPath.read(result, "$.orderNumber");
        String token = JsonPath.read(result, "$.accessToken");
        assertThat(reserved(variant)).isEqualTo(1);

        mvc.perform(get("/api/v1/orders/" + number).param("token", token))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.total").value(7900 + 300)) // under the free-delivery threshold
                .andExpect(jsonPath("$.shippingTotal").value(300));
        mvc.perform(get("/api/v1/orders/" + number).param("token", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/orders/" + number + "/test-mpesa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"approve\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.timeline[*].type").value(hasItem("PAID")));

        assertThat(reserved(variant)).isZero();
        assertThat(onHand(variant)).isEqualTo(onHandBefore - 1);
        mvc.perform(get("/api/v1/carts/" + cart)).andExpect(jsonPath("$.itemCount").value(0));
        assertThat(jdbc.sql("SELECT count(*) FROM inventory_adjustments WHERE variant_id = :v AND reason = 'SOLD'")
                .param("v", variant).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    void declinedCardCanBeRetried() throws Exception {
        String cart = newCart();
        addItem(cart, variantId("WST-NAT-OS"), 1);
        String result = mvc.perform(post("/api/v1/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(cart, "CARD", "PICKUP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.nextAction").value("ENTER_TEST_CARD"))
                .andReturn().getResponse().getContentAsString();
        String number = JsonPath.read(result, "$.orderNumber");
        String token = JsonPath.read(result, "$.accessToken");

        mvc.perform(post("/api/v1/orders/" + number + "/test-card").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"cardNumber\":\"4000 0000 0000 0002\"}"))
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.payment.failureMessage").value(org.hamcrest.Matchers.containsString("declined")));
        mvc.perform(post("/api/v1/orders/" + number + "/pay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(jsonPath("$.nextAction").value("ENTER_TEST_CARD"));
        mvc.perform(post("/api/v1/orders/" + number + "/test-card").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"cardNumber\":\"4242424242424242\"}"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.shippingTotal").value(0)); // pickup
    }

    @Test
    void checkoutValidatesPhoneAndAddress() throws Exception {
        String cart = newCart();
        addItem(cart, variantId("LWD-SAND-L"), 1);
        mvc.perform(post("/api/v1/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(cart, "MPESA", "NAIROBI").replace("0712 345 678", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Kenyan mobile")));
        mvc.perform(post("/api/v1/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(cart, "MPESA", "NAIROBI").replace("\"county\":\"Nairobi\"", "\"county\":\"Atlantis\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").value(hasItem("County")));
    }

    @Test
    void twoShoppersCannotBothBuyTheLastUnit() throws Exception {
        long variant = createLiveProductWithStock("LAST-ONE-1", 1);
        List<UUID> carts = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String cart = newCart();
            addItem(cart, variant, 1).andExpect(status().isOk());
            carts.add(UUID.fromString(cart));
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (UUID cart : carts) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    return checkoutService.placeOrder(request(cart), null).getNumber();
                } catch (ConflictException e) {
                    return "CONFLICT";
                }
            }));
        }
        start.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) outcomes.add(f.get());
        pool.shutdown();

        assertThat(outcomes).containsOnlyOnce("CONFLICT");
        assertThat(outcomes.stream().filter(o -> o.startsWith("YV-"))).hasSize(1);
        assertThat(reserved(variant)).isEqualTo(1);
        assertThat(onHand(variant)).isEqualTo(1);
    }

    @Test
    void unpaidOrderReleasesItsStockWhenCancelled() throws Exception {
        long variant = createLiveProductWithStock("EXPIRE-1", 2);
        String cart = newCart();
        addItem(cart, variant, 2);
        Order order = checkoutService.placeOrder(request(UUID.fromString(cart)), null);
        assertThat(reserved(variant)).isEqualTo(2);

        assertThat(orderService.cancelUnpaid(order.getId(), "Payment not received in time", null)).isTrue();
        assertThat(reserved(variant)).isZero();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(Order.Status.CANCELLED);
        // Product can be bought again.
        String cart2 = newCart();
        addItem(cart2, variant, 2).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ helpers

    private String newCart() throws Exception {
        String body = mvc.perform(post("/api/v1/carts")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private org.springframework.test.web.servlet.ResultActions addItem(String cart, long variantId, int qty) throws Exception {
        return mvc.perform(post("/api/v1/carts/" + cart + "/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\":" + variantId + ",\"quantity\":" + qty + "}"));
    }

    private long variantId(String sku) {
        return jdbc.sql("SELECT id FROM product_variants WHERE sku = :sku").param("sku", sku).query(Long.class).single();
    }

    private int onHand(long variant) {
        return jdbc.sql("SELECT on_hand FROM inventory_levels WHERE variant_id = :v").param("v", variant).query(Integer.class).single();
    }

    private int reserved(long variant) {
        return jdbc.sql("SELECT reserved FROM inventory_levels WHERE variant_id = :v").param("v", variant).query(Integer.class).single();
    }

    private static String checkoutJson(String cart, String method, String shipping) {
        return """
                {"cartToken":"%s","email":"Wanjiru@Example.com","phone":"0712 345 678","fullName":"Wanjiru Kamau",
                 "addressLine1":"Riverside Drive 14","city":"Nairobi","county":"Nairobi",
                 "shippingMethod":"%s","paymentMethod":"%s"}""".formatted(cart, shipping, method);
    }

    private static CheckoutRequest request(UUID cart) {
        return new CheckoutRequest(cart, "race@example.com", "0712345678", "Race Tester", "Moi Avenue 1", null,
                "Nairobi", "Nairobi", null, null, "NAIROBI", Order.PaymentMethod.MPESA, null);
    }

    /** A live product with one variant and the given stock, created through the admin API. */
    private long createLiveProductWithStock(String sku, int stock) throws Exception {
        String login = mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@yvette.local\",\"password\":\"yvette-dev-owner\"}"))
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + JsonPath.read(login, "$.accessToken");
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'accessories'").query(Long.class).single();
        String created = mvc.perform(post("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Test %s","description":"Test item","categoryId":%d,"material":"Cotton",
                                 "variants":[{"sku":"%s","size":"One size","price":1000,"openingStock":%d}],
                                 "images":[{"url":"https://placehold.co/800x1000.png"}]}""".formatted(sku, categoryId, sku, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer productId = JsonPath.read(created, "$.id");
        mvc.perform(post("/api/v1/admin/products/" + productId + "/publish").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
        return ((Integer) JsonPath.read(created, "$.variants[0].id")).longValue();
    }
}
