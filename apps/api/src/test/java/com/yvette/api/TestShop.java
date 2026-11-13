package com.yvette.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/** Shopping and staff helpers shared by the API tests. */
final class TestShop {

    record PlacedOrder(String number, String token, long id) {}

    private final MockMvc mvc;
    private final JdbcClient jdbc;

    TestShop(MockMvc mvc, JdbcClient jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    String newCart() throws Exception {
        String body = mvc.perform(post("/api/v1/carts")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    void add(String cart, String sku, int qty) throws Exception {
        mvc.perform(post("/api/v1/carts/" + cart + "/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + variantId(sku) + ",\"quantity\":" + qty + "}"))
                .andExpect(status().isOk());
    }

    long variantId(String sku) {
        return jdbc.sql("SELECT id FROM product_variants WHERE sku = :s").param("s", sku).query(Long.class).single();
    }

    int onHand(String sku) {
        return jdbc.sql("SELECT on_hand FROM inventory_levels WHERE variant_id = :v").param("v", variantId(sku))
                .query(Integer.class).single();
    }

    /** Guest (or signed-in, with a customer token) checkout paid by test M-Pesa. */
    PlacedOrder buyAndPay(String email, String customerToken, String sku, int qty) throws Exception {
        String cart = newCart();
        add(cart, sku, qty);
        var request = post("/api/v1/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                {"cartToken":"%s","email":"%s","phone":"0712345678","fullName":"Amina Wanjiku",
                 "addressLine1":"Ngong Road 5","city":"Nairobi","county":"Nairobi",
                 "shippingMethod":"NAIROBI","paymentMethod":"MPESA"}""".formatted(cart, email));
        if (customerToken != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken);
        String placed = mvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String number = JsonPath.read(placed, "$.orderNumber");
        String token = JsonPath.read(placed, "$.accessToken");
        mvc.perform(post("/api/v1/orders/" + number + "/test-mpesa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"approve\":true}")).andExpect(status().isOk());
        long id = jdbc.sql("SELECT id FROM orders WHERE number = :n").param("n", number).query(Long.class).single();
        return new PlacedOrder(number, token, id);
    }

    String staffLogin(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    String owner() throws Exception {
        return staffLogin("owner@yvette.local", "yvette-dev-owner");
    }

    /** Creates (once) and signs in a staff member with the given role. */
    String staff(String role) throws Exception {
        String email = role.toLowerCase() + "-test@yvette.local";
        int exists = jdbc.sql("SELECT count(*) FROM staff_users WHERE email = :e").param("e", email).query(Integer.class).single();
        if (exists == 0) {
            as(owner(), post("/api/v1/admin/staff").contentType(MediaType.APPLICATION_JSON).content("""
                    {"email":"%s","fullName":"Test %s","role":"%s","password":"test-password-123"}"""
                    .formatted(email, role, role))).andExpect(status().isCreated());
        }
        return staffLogin(email, "test-password-123");
    }

    /** A live product with one variant and the given stock; returns the variant id. */
    long createLiveProduct(String sku, int stock) throws Exception {
        String owner = owner();
        long categoryId = jdbc.sql("SELECT id FROM categories WHERE slug = 'accessories'").query(Long.class).single();
        String created = as(owner, post("/api/v1/admin/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Test %s","description":"Test item","categoryId":%d,"material":"Cotton",
                 "variants":[{"sku":"%s","size":"One size","price":1000,"openingStock":%d}],
                 "images":[{"url":"https://placehold.co/800x1000.png"}]}""".formatted(sku, categoryId, sku, stock)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer productId = JsonPath.read(created, "$.id");
        as(owner, post("/api/v1/admin/products/" + productId + "/publish")).andExpect(status().isOk());
        return ((Integer) JsonPath.read(created, "$.variants[0].id")).longValue();
    }

    ResultActions as(String token, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
